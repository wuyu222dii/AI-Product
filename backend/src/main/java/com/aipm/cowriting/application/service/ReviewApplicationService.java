package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.AppealReviewResult;
import com.aipm.cowriting.application.dto.ai.ReviewGenerationResult;
import com.aipm.cowriting.application.dto.ai.ReviewRecheckResult;
import com.aipm.cowriting.application.dto.review.AppealRequest;
import com.aipm.cowriting.application.dto.review.AppealResponse;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.review.ReviewRecheckLogResponse;
import com.aipm.cowriting.application.dto.review.UpdateReviewStatusRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AppealCaseEntity;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.entity.ReviewRecheckLogEntity;
import com.aipm.cowriting.domain.model.ReviewImpactLevel;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.AppealCaseRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.ReviewRecheckLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ReviewApplicationService {

    private static final Pattern APA_CITATION_PATTERN = Pattern.compile("[（(][^）)]{1,80}[，,]\\s*((19|20)\\d{2}|n\\.d\\.)[）)]");
    private static final Pattern GBT_CITATION_PATTERN = Pattern.compile("\\[(\\d{1,3})]");
    private static final Pattern REFERENCE_SECTION_PATTERN = Pattern.compile("(?m)^\\s*(参考文献|References)\\s*$");
    private static final Pattern REFERENCE_ENTRY_PATTERN = Pattern.compile("(?m)^\\s*(\\[\\d+]|\\d+[.、])\\s*\\S+");

    private final DraftVersionRepository draftVersionRepository;
    private final RequirementSnapshotRepository requirementSnapshotRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final AppealCaseRepository appealCaseRepository;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final ReviewRecheckLogRepository reviewRecheckLogRepository;
    private final OpenAiReviewService openAiReviewService;
    private final WritingRiskApplicationService writingRiskApplicationService;
    private final ObjectMapper objectMapper;

    public ReviewApplicationService(
            DraftVersionRepository draftVersionRepository,
            RequirementSnapshotRepository requirementSnapshotRepository,
            ReviewItemRepository reviewItemRepository,
            AppealCaseRepository appealCaseRepository,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            ReviewRecheckLogRepository reviewRecheckLogRepository,
            OpenAiReviewService openAiReviewService,
            WritingRiskApplicationService writingRiskApplicationService,
            ObjectMapper objectMapper
    ) {
        this.draftVersionRepository = draftVersionRepository;
        this.requirementSnapshotRepository = requirementSnapshotRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.appealCaseRepository = appealCaseRepository;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.reviewRecheckLogRepository = reviewRecheckLogRepository;
        this.openAiReviewService = openAiReviewService;
        this.writingRiskApplicationService = writingRiskApplicationService;
        this.objectMapper = objectMapper;
    }

    public List<ReviewItemResponse> list(UUID draftId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));

        List<ReviewItemEntity> stored = reviewItemRepository.findByDraftVersionIdOrderByCreatedAtAsc(draftId);
        if (stored.isEmpty()) {
            stored = refreshForDraft(draft);
        }

        return stored.stream().map(this::toResponse).toList();
    }

    public AppealResponse createAppeal(UUID reviewItemId, AppealRequest request) {
        ReviewItemEntity reviewItem = reviewItemRepository.findById(reviewItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "review item 不存在"));
        DraftVersionEntity draft = draftVersionRepository.findById(reviewItem.getDraftVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));

        Map<String, Object> reviewItemMap = new LinkedHashMap<>();
        reviewItemMap.put("reviewType", reviewItem.getReviewType());
        reviewItemMap.put("reviewImpactLevel", reviewItem.getReviewImpactLevel().name());
        reviewItemMap.put("targetRange", readMap(reviewItem.getTargetRangeJson()));
        reviewItemMap.put("message", reviewItem.getMessage());
        reviewItemMap.put("suggestedFix", reviewItem.getSuggestedFix());

        AppealReviewResult reviewed = openAiReviewService.reviewAppeal(
                reviewItemMap,
                draft.getDraftText(),
                request.userReason(),
                request.evidence() == null ? Map.of() : request.evidence()
        );

        if ("withdrawn".equalsIgnoreCase(reviewed.reviewOutcome())) {
            reviewItem.setReviewStatus("RESOLVED");
            reviewItem.setResolutionNote("复审已撤销该审查项：" + reviewed.reviewOutcome());
            reviewItem.setResolvedAt(OffsetDateTime.now());
            reviewItemRepository.save(reviewItem);
        } else if (reviewed.downgradedImpactLevel() != null && !reviewed.downgradedImpactLevel().isBlank()) {
            reviewItem.setReviewImpactLevel(parseImpactLevel(reviewed.downgradedImpactLevel()));
            reviewItem.setResolutionNote("复审后降级：" + reviewed.reviewOutcome());
            reviewItemRepository.save(reviewItem);
        }

        AppealCaseEntity entity = new AppealCaseEntity();
        entity.setId(UUID.randomUUID());
        entity.setReviewItemId(reviewItemId);
        entity.setUserReason(request.userReason());
        entity.setEvidenceJson(writeJson(request.evidence() == null ? Map.of() : request.evidence()));
        entity.setReviewOutcome(reviewed.reviewOutcome());
        entity.setResolvedAt(OffsetDateTime.now());
        entity.setCreatedAt(OffsetDateTime.now());
        appealCaseRepository.save(entity);

        return new AppealResponse(
                entity.getId(),
                entity.getReviewItemId(),
                entity.getUserReason(),
                request.evidence() == null ? Map.of() : request.evidence(),
                entity.getReviewOutcome(),
                entity.getResolvedAt(),
                entity.getCreatedAt()
        );
    }

    public AppealResponse getAppeal(UUID appealId) {
        AppealCaseEntity entity = appealCaseRepository.findById(appealId)
                .orElseThrow(() -> new BusinessException(ErrorCode.APPEAL_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "appeal 不存在"));
        return new AppealResponse(
                entity.getId(),
                entity.getReviewItemId(),
                entity.getUserReason(),
                readMap(entity.getEvidenceJson()),
                entity.getReviewOutcome(),
                entity.getResolvedAt(),
                entity.getCreatedAt()
        );
    }

    public ReviewItemResponse updateStatus(UUID reviewItemId, UpdateReviewStatusRequest request) {
        ReviewItemEntity reviewItem = reviewItemRepository.findById(reviewItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "review item 不存在"));
        String status = normalizeReviewStatus(request.status());
        reviewItem.setReviewStatus(status);
        reviewItem.setResolutionNote(cleanNote(request.resolutionNote()));
        if ("OPEN".equals(status)) {
            reviewItem.setResolvedAt(null);
        } else {
            reviewItem.setResolvedAt(OffsetDateTime.now());
        }
        return toResponse(reviewItemRepository.save(reviewItem));
    }

    public ReviewItemResponse recheck(UUID reviewItemId) {
        ReviewItemEntity reviewItem = reviewItemRepository.findById(reviewItemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_ITEM_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "review item 不存在"));
        DraftVersionEntity draft = draftVersionRepository.findById(reviewItem.getDraftVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));

        Map<String, Object> reviewItemMap = new LinkedHashMap<>();
        reviewItemMap.put("reviewType", reviewItem.getReviewType());
        reviewItemMap.put("reviewImpactLevel", reviewItem.getReviewImpactLevel().name());
        reviewItemMap.put("targetRange", readMap(reviewItem.getTargetRangeJson()));
        reviewItemMap.put("message", reviewItem.getMessage());
        reviewItemMap.put("suggestedFix", reviewItem.getSuggestedFix());
        reviewItemMap.put("currentStatus", reviewItem.getReviewStatus());

        ReviewRecheckResult result = openAiReviewService.recheckReviewItem(reviewItemMap, draft.getDraftText());
        String outcome = normalizeRecheckOutcome(result.outcome());
        OffsetDateTime now = OffsetDateTime.now();
        String previousStatus = reviewItem.getReviewStatus() == null ? "OPEN" : reviewItem.getReviewStatus();
        String previousImpactLevel = reviewItem.getReviewImpactLevel().name();

        if ("RESOLVED".equals(outcome)) {
            reviewItem.setReviewStatus("RESOLVED");
            reviewItem.setResolvedAt(now);
        } else if ("STILL_OPEN".equals(outcome) || "NEEDS_MORE_EVIDENCE".equals(outcome)) {
            reviewItem.setReviewStatus("OPEN");
            reviewItem.setResolvedAt(null);
        } else if ("DOWNGRADED".equals(outcome)) {
            reviewItem.setReviewImpactLevel(parseImpactLevel(result.downgradedImpactLevel()));
            reviewItem.setReviewStatus("OPEN");
            reviewItem.setResolvedAt(null);
        }

        reviewItem.setLastRecheckedAt(now);
        String note = cleanNote(result.note());
        reviewItem.setRecheckNote(outcome + "：" + (note == null ? "无补充说明" : note));
        ReviewItemEntity saved = reviewItemRepository.save(reviewItem);
        saveRecheckLog(saved, outcome, previousStatus, previousImpactLevel, note, now);
        return toResponse(saved);
    }

    public List<ReviewItemEntity> refreshForDraft(DraftVersionEntity draft) {
        RequirementSnapshotEntity snapshot = requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(draft.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.REQUIREMENT_SNAPSHOT_MISSING,
                        HttpStatus.NOT_FOUND.value(),
                        "requirement snapshot 不存在"
                ));

        Map<String, Object> sourceTraceMap = readMap(draft.getSourceTraceMapJson());
        ReviewGenerationResult generated = openAiReviewService.generateReview(
                draft.getDraftText(),
                buildRequirementContext(snapshot),
                sourceTraceMap
        );

        reviewItemRepository.deleteByDraftVersionId(draft.getId());

        List<Map<String, Object>> reviewItems = new ArrayList<>(generated.items() == null ? List.of() : generated.items());
        reviewItems.addAll(buildCitationReviewItems(draft.getDraftText(), snapshot.getCitationStyle(), sourceTraceMap));
        reviewItems.addAll(writingRiskApplicationService.reviewItems(draft));

        List<ReviewItemEntity> entities = reviewItems.stream().map(item -> {
            ReviewItemEntity entity = new ReviewItemEntity();
            entity.setId(UUID.randomUUID());
            entity.setWorkspaceId(draft.getWorkspaceId());
            entity.setDraftVersionId(draft.getId());
            entity.setReviewType(String.valueOf(item.getOrDefault("reviewType", "unknown")));
            entity.setReviewImpactLevel(parseImpactLevel(String.valueOf(item.getOrDefault("reviewImpactLevel", "NOTICE"))));
            entity.setTargetRangeJson(writeJson(item.getOrDefault("targetRange", Map.of())));
            entity.setMessage(String.valueOf(item.getOrDefault("message", "")));
            entity.setSuggestedFix(String.valueOf(item.getOrDefault("suggestedFix", "")));
            entity.setCanBypass(Boolean.parseBoolean(String.valueOf(item.getOrDefault("canBypass", true))));
            entity.setReviewStatus("OPEN");
            entity.setCreatedAt(OffsetDateTime.now());
            return entity;
        }).toList();

        return reviewItemRepository.saveAll(entities);
    }

    private List<Map<String, Object>> buildCitationReviewItems(
            String draftText,
            String citationStyle,
            Map<String, Object> sourceTraceMap
    ) {
        String text = draftText == null ? "" : draftText;
        LinkedHashSet<String> sourceIds = collectSourceIds(sourceTraceMap);
        boolean hasSources = !sourceIds.isEmpty();
        Matcher apaMatcher = APA_CITATION_PATTERN.matcher(text);
        Matcher gbtMatcher = GBT_CITATION_PATTERN.matcher(text);
        boolean hasApaCitation = apaMatcher.find();
        boolean hasGbtCitation = gbtMatcher.find();
        boolean expectsGbt = isGbtStyle(citationStyle);
        List<Map<String, Object>> items = new ArrayList<>();

        if (expectsGbt && hasApaCitation) {
            apaMatcher.reset();
            apaMatcher.find();
            items.add(reviewMap(
                    "citation_format_mismatch",
                    "LOCAL_FIX",
                    targetRange(text, apaMatcher.start(), apaMatcher.end()),
                    "老师要求或当前设置为 GB/T 7714 编号制，但正文中出现了作者-年份式引用。",
                    "请将该处改为编号式引用，例如 [1]，并确认参考文献列表顺序一致。",
                    true
            ));
        }
        if (!expectsGbt && hasGbtCitation) {
            gbtMatcher.reset();
            gbtMatcher.find();
            items.add(reviewMap(
                    "citation_format_mismatch",
                    "LOCAL_FIX",
                    targetRange(text, gbtMatcher.start(), gbtMatcher.end()),
                    "老师要求或当前设置为 APA 作者-年份格式，但正文中出现了编号式引用。",
                    "请将该处改为作者-年份式引用，例如（作者，年份）。",
                    true
            ));
        }
        if (hasSources && !hasApaCitation && !hasGbtCitation) {
            items.add(reviewMap(
                    "citation_missing",
                    "LOCAL_FIX",
                    targetRange(text, 0, Math.min(text.length(), 240)),
                    "正文来源追溯中已经绑定了材料，但正文里没有发现可识别的引用标记。",
                    "请在使用材料支撑的句子或段落末尾插入引用，避免参考文献与正文脱节。",
                    true
            ));
        }
        if (!hasSources && (hasApaCitation || hasGbtCitation)) {
            int[] range = firstCitationRange(text);
            items.add(reviewMap(
                    "reference_orphan",
                    "MUST_CONFIRM",
                    targetRange(text, range[0], range[1]),
                    "正文中出现了引用标记，但当前草稿没有对应的材料来源追溯。",
                    "请确认该引用来自已上传材料；如果不是，请上传对应文献并重新解析/生成，避免疑似编造来源。",
                    false
            ));
        }
        if (hasGbtCitation) {
            int maxCitationNo = maxGbtCitationNo(text);
            if (maxCitationNo > sourceIds.size() && hasSources) {
                int[] range = firstCitationRange(text);
                items.add(reviewMap(
                        "reference_orphan",
                        "MUST_CONFIRM",
                        targetRange(text, range[0], range[1]),
                        "正文中的编号引用数量超过了当前可追溯材料数量。",
                        "请检查编号是否对应真实上传材料，必要时补充材料或删除无来源引用。",
                        false
                ));
            }
        }
        if (hasReferenceEntries(text) && !hasApaCitation && !hasGbtCitation) {
            int start = referenceSectionStart(text);
            items.add(reviewMap(
                    "reference_not_cited",
                    "LOCAL_FIX",
                    targetRange(text, start, Math.min(text.length(), start + 240)),
                    "参考文献区存在条目，但正文中没有发现对应引用。",
                    "请在正文相关论点处插入引用，或者删除未实际使用的参考文献。",
                    true
            ));
        }
        items.addAll(buildMetadataReviewItems(sourceIds));
        return items;
    }

    private List<Map<String, Object>> buildMetadataReviewItems(Set<String> sourceIds) {
        List<UUID> materialIds = sourceIds.stream()
                .map(this::parseUuid)
                .filter(Objects::nonNull)
                .toList();
        if (materialIds.isEmpty()) {
            return List.of();
        }
        List<AiSemanticParseResultEntity> parseResults = aiSemanticParseResultRepository.findByMaterialIdIn(materialIds);
        long incompleteCount = materialIds.size() - parseResults.size() + parseResults.stream()
                .filter(this::isBibliographicMetadataIncomplete)
                .count();
        if (incompleteCount == 0) {
            return List.of();
        }
        return List.of(reviewMap(
                "reference_metadata_incomplete",
                "NOTICE",
                Map.of(),
                "有 " + incompleteCount + " 份已引用材料缺少作者、年份或题名等关键文献信息。",
                "请在导出页编辑文献信息，补全作者、年份、题名、期刊/出版社、DOI 或链接。",
                true
        ));
    }

    private boolean isBibliographicMetadataIncomplete(AiSemanticParseResultEntity parseResult) {
        if (parseResult.getBibliographicMetadataJson() == null || parseResult.getBibliographicMetadataJson().isBlank()) {
            return true;
        }
        try {
            Map<String, Object> metadata = readMap(parseResult.getBibliographicMetadataJson());
            Object authors = metadata.get("authors");
            boolean hasAuthor = authors instanceof List<?> authorList && authorList.stream().anyMatch(item -> item != null && !String.valueOf(item).isBlank());
            boolean hasYear = hasText(metadata.get("year"));
            boolean hasTitle = hasText(metadata.get("title"));
            return !hasAuthor || !hasYear || !hasTitle;
        } catch (BusinessException e) {
            return true;
        }
    }

    private Map<String, Object> buildRequirementContext(RequirementSnapshotEntity snapshot) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("topic", snapshot.getTopic());
        context.put("wordCount", snapshot.getWordCount());
        context.put("deadline", snapshot.getDeadline() == null ? null : snapshot.getDeadline().toString());
        context.put("citationStyle", snapshot.getCitationStyle());
        context.put("specialRequirements", readMap(snapshot.getSpecialRequirementsJson()));
        return context;
    }

    private LinkedHashSet<String> collectSourceIds(Map<String, Object> sourceTraceMap) {
        LinkedHashSet<String> sourceIds = new LinkedHashSet<>();
        if (sourceTraceMap == null || sourceTraceMap.isEmpty()) {
            return sourceIds;
        }
        sourceTraceMap.values().forEach(value -> collectSourceIds(value, sourceIds));
        return sourceIds;
    }

    private void collectSourceIds(Object value, LinkedHashSet<String> sourceIds) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            sourceIds.add(stringValue);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                collectSourceIds(item, sourceIds);
            }
            return;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Object nested = mapValue.get("materialIds");
            if (nested == null) {
                nested = mapValue.get("materials");
            }
            if (nested == null) {
                nested = mapValue.get("sources");
            }
            collectSourceIds(nested, sourceIds);
        }
    }

    private Map<String, Object> reviewMap(
            String reviewType,
            String reviewImpactLevel,
            Map<String, Object> targetRange,
            String message,
            String suggestedFix,
            boolean canBypass
    ) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("reviewType", reviewType);
        item.put("reviewImpactLevel", reviewImpactLevel);
        item.put("targetRange", targetRange);
        item.put("message", message);
        item.put("suggestedFix", suggestedFix);
        item.put("canBypass", canBypass);
        return item;
    }

    private Map<String, Object> targetRange(String text, int start, int end) {
        String source = text == null ? "" : text;
        int safeStart = Math.max(0, Math.min(start, source.length()));
        int safeEnd = Math.max(safeStart, Math.min(end, source.length()));
        return Map.of(
                "start", safeStart,
                "end", safeEnd,
                "selectedText", source.substring(safeStart, safeEnd)
        );
    }

    private int[] firstCitationRange(String text) {
        Matcher gbtMatcher = GBT_CITATION_PATTERN.matcher(text);
        if (gbtMatcher.find()) {
            return new int[]{gbtMatcher.start(), gbtMatcher.end()};
        }
        Matcher apaMatcher = APA_CITATION_PATTERN.matcher(text);
        if (apaMatcher.find()) {
            return new int[]{apaMatcher.start(), apaMatcher.end()};
        }
        return new int[]{0, Math.min(text.length(), 120)};
    }

    private boolean isGbtStyle(String citationStyle) {
        String normalized = citationStyle == null ? "" : citationStyle.toLowerCase();
        return normalized.contains("gb") || normalized.contains("7714");
    }

    private int maxGbtCitationNo(String text) {
        Matcher matcher = GBT_CITATION_PATTERN.matcher(text);
        int max = 0;
        while (matcher.find()) {
            max = Math.max(max, Integer.parseInt(matcher.group(1)));
        }
        return max;
    }

    private boolean hasReferenceEntries(String text) {
        return REFERENCE_SECTION_PATTERN.matcher(text).find() && REFERENCE_ENTRY_PATTERN.matcher(text).find();
    }

    private int referenceSectionStart(String text) {
        Matcher matcher = REFERENCE_SECTION_PATTERN.matcher(text);
        return matcher.find() ? matcher.start() : Math.max(0, text.length() - 240);
    }

    private UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean hasText(Object value) {
        return value != null && !String.valueOf(value).isBlank();
    }

    private ReviewItemResponse toResponse(ReviewItemEntity entity) {
        return new ReviewItemResponse(
                entity.getId(),
                entity.getReviewType(),
                entity.getReviewImpactLevel(),
                readMap(entity.getTargetRangeJson()),
                entity.getMessage(),
                entity.getSuggestedFix(),
                entity.isCanBypass(),
                entity.getReviewStatus() == null ? "OPEN" : entity.getReviewStatus(),
                entity.getResolutionNote(),
                entity.getResolvedAt(),
                entity.getLastRecheckedAt(),
                entity.getRecheckNote(),
                recheckHistory(entity.getId()),
                entity.getCreatedAt()
        );
    }

    private List<ReviewRecheckLogResponse> recheckHistory(UUID reviewItemId) {
        List<ReviewRecheckLogEntity> logs = reviewRecheckLogRepository.findByReviewItemIdOrderByCreatedAtDesc(reviewItemId);
        return (logs == null ? List.<ReviewRecheckLogEntity>of() : logs).stream()
                .limit(5)
                .map(this::toRecheckLogResponse)
                .toList();
    }

    private void saveRecheckLog(
            ReviewItemEntity reviewItem,
            String outcome,
            String previousStatus,
            String previousImpactLevel,
            String note,
            OffsetDateTime createdAt
    ) {
        ReviewRecheckLogEntity log = new ReviewRecheckLogEntity();
        log.setId(UUID.randomUUID());
        log.setReviewItemId(reviewItem.getId());
        log.setDraftVersionId(reviewItem.getDraftVersionId());
        log.setOutcome(outcome);
        log.setPreviousStatus(previousStatus);
        log.setNextStatus(reviewItem.getReviewStatus() == null ? "OPEN" : reviewItem.getReviewStatus());
        log.setPreviousImpactLevel(previousImpactLevel);
        log.setNextImpactLevel(reviewItem.getReviewImpactLevel().name());
        log.setNote(note);
        log.setBasisJson(writeJson(Map.of(
                "reviewType", reviewItem.getReviewType(),
                "targetRange", readMap(reviewItem.getTargetRangeJson()),
                "message", reviewItem.getMessage(),
                "suggestedFix", reviewItem.getSuggestedFix() == null ? "" : reviewItem.getSuggestedFix()
        )));
        log.setCreatedAt(createdAt);
        reviewRecheckLogRepository.save(log);
    }

    private ReviewRecheckLogResponse toRecheckLogResponse(ReviewRecheckLogEntity entity) {
        return new ReviewRecheckLogResponse(
                entity.getId(),
                entity.getOutcome(),
                entity.getPreviousStatus(),
                entity.getNextStatus(),
                entity.getPreviousImpactLevel(),
                entity.getNextImpactLevel(),
                entity.getNote(),
                readMap(entity.getBasisJson()),
                entity.getCreatedAt()
        );
    }

    private String normalizeReviewStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase();
        if (List.of("OPEN", "RESOLVED", "IGNORED").contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(
                ErrorCode.INVALID_REQUEST_BODY,
                HttpStatus.BAD_REQUEST.value(),
                "review status 仅支持 OPEN / RESOLVED / IGNORED"
        );
    }

    private String cleanNote(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        return note.trim();
    }

    private String normalizeRecheckOutcome(String outcome) {
        String normalized = outcome == null ? "" : outcome.trim().toUpperCase();
        if (List.of("RESOLVED", "STILL_OPEN", "DOWNGRADED", "NEEDS_MORE_EVIDENCE").contains(normalized)) {
            return normalized;
        }
        return "STILL_OPEN";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 反序列化失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    private ReviewImpactLevel parseImpactLevel(String value) {
        try {
            return ReviewImpactLevel.valueOf(value == null ? "NOTICE" : value.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ReviewImpactLevel.NOTICE;
        }
    }
}
