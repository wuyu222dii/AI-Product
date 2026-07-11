package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.ReviewGenerationResult;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.review.ReviewRecheckLogResponse;
import com.aipm.cowriting.application.model.ContentScope;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.entity.ReviewRecheckLogEntity;
import com.aipm.cowriting.domain.model.ReviewImpactLevel;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.ReviewRecheckLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicReviewApplicationService {

    private static final Pattern APA_CITATION_PATTERN = Pattern.compile("[（(][^）)]{1,80}[，,]\\s*((19|20)\\d{2}|n\\.d\\.)[）)]");
    private static final Pattern GBT_CITATION_PATTERN = Pattern.compile("\\[(\\d{1,3})]");

    private final ContentScopeResolverService scopeResolver;
    private final ReviewItemRepository reviewRepository;
    private final ReviewRecheckLogRepository recheckLogRepository;
    private final OpenAiReviewService openAiReviewService;
    private final WritingRiskApplicationService writingRiskService;
    private final ObjectMapper objectMapper;

    public AcademicReviewApplicationService(
            ContentScopeResolverService scopeResolver,
            ReviewItemRepository reviewRepository,
            ReviewRecheckLogRepository recheckLogRepository,
            OpenAiReviewService openAiReviewService,
            WritingRiskApplicationService writingRiskService,
            ObjectMapper objectMapper
    ) {
        this.scopeResolver = scopeResolver;
        this.reviewRepository = reviewRepository;
        this.recheckLogRepository = recheckLogRepository;
        this.openAiReviewService = openAiReviewService;
        this.writingRiskService = writingRiskService;
        this.objectMapper = objectMapper;
    }

    public List<ReviewItemResponse> listDocument(UUID documentId, UUID sectionId, String status, String scopeType) {
        List<ReviewItemEntity> items = reviewRepository.findByDocumentIdOrderByCreatedAtAsc(documentId).stream()
                .filter(item -> sectionId == null || sectionId.equals(item.getSectionId()))
                .filter(item -> scopeType == null || scopeType.isBlank() || scopeType.equalsIgnoreCase(item.getScopeType()))
                .filter(item -> status == null || status.isBlank() || status.equalsIgnoreCase(item.getReviewStatus()))
                .filter(item -> !"SUPERSEDED".equals(item.getReviewStatus()))
                .toList();
        if (items.isEmpty()) return List.of();
        Map<UUID, Integer> currentVersions = scopeResolver.documentSections(documentId).stream()
                .collect(Collectors.toMap(ContentScope::sectionId, ContentScope::revision));
        Map<UUID, List<ReviewRecheckLogResponse>> historyByReview = recheckLogRepository
                .findByReviewItemIdInOrderByCreatedAtDesc(items.stream().map(ReviewItemEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(
                        ReviewRecheckLogEntity::getReviewItemId,
                        LinkedHashMap::new,
                        Collectors.collectingAndThen(Collectors.toList(), logs -> logs.stream().limit(5).map(this::toRecheckLog).toList())
                ));
        return items.stream()
                .map(item -> toResponse(item, currentVersions.get(item.getSectionId()), historyByReview.getOrDefault(item.getId(), List.of())))
                .toList();
    }

    @Transactional
    public List<ReviewItemResponse> refreshSection(UUID sectionId) {
        return refresh(scopeResolver.section(sectionId));
    }

    public List<ReviewItemResponse> refreshDocument(UUID documentId) {
        List<ReviewItemResponse> result = new ArrayList<>();
        for (ContentScope scope : scopeResolver.documentSections(documentId)) {
            if (!scope.content().isBlank()) result.addAll(refresh(scope));
        }
        return result;
    }

    @Transactional
    public List<ReviewItemResponse> refresh(ContentScope scope) {
        ReviewGenerationResult generated = openAiReviewService.generateReview(
                scope.content(),
                scope.requirementContext(),
                scope.sourceTraceMap()
        );
        List<Map<String, Object>> proposed = new ArrayList<>(generated.items() == null ? List.of() : generated.items());
        proposed.addAll(citationItems(scope));
        proposed.addAll(writingRiskService.reviewItems(scope));
        return synchronize(scope, proposed).stream().map(this::toResponse).toList();
    }

    public List<ReviewItemEntity> relatedOpenItems(UUID sectionId, Map<String, Object> targetRange) {
        int start = integer(targetRange.get("start"), 0);
        int end = integer(targetRange.get("end"), Integer.MAX_VALUE);
        return reviewRepository.findBySectionIdOrderByCreatedAtAsc(sectionId).stream()
                .filter(item -> List.of("OPEN", "MODIFIED_PENDING_RECHECK").contains(item.getReviewStatus()))
                .filter(item -> overlaps(readMap(item.getTargetRangeJson()), start, end))
                .toList();
    }

    @Transactional
    public void markPendingRecheck(List<UUID> reviewItemIds) {
        if (reviewItemIds == null || reviewItemIds.isEmpty()) return;
        List<ReviewItemEntity> items = reviewRepository.findAllById(reviewItemIds);
        items.forEach(item -> {
            if (!List.of("RESOLVED", "IGNORED", "SUPERSEDED").contains(item.getReviewStatus())) {
                item.setReviewStatus("MODIFIED_PENDING_RECHECK");
                item.setResolutionNote("相关章节已应用 AI 修改，建议手动复查此项");
            }
        });
        reviewRepository.saveAll(items);
    }

    public ReviewItemResponse toResponse(ReviewItemEntity entity) {
        Integer currentRevision = entity.getSectionId() == null ? entity.getSectionVersionNo() : scopeResolver.section(entity.getSectionId()).revision();
        return toResponse(entity, currentRevision, recheckHistory(entity.getId()));
    }

    private ReviewItemResponse toResponse(
            ReviewItemEntity entity,
            Integer currentRevision,
            List<ReviewRecheckLogResponse> recheckHistory
    ) {
        String analysisState = entity.getSectionId() != null && !Objects.equals(currentRevision, entity.getSectionVersionNo())
                ? "STALE"
                : "CURRENT";
        return new ReviewItemResponse(
                entity.getId(), entity.getReviewType(), entity.getReviewImpactLevel(), readMap(entity.getTargetRangeJson()),
                entity.getMessage(), entity.getSuggestedFix(), entity.isCanBypass(), entity.getReviewStatus(),
                entity.getResolutionNote(), entity.getResolvedAt(), entity.getLastRecheckedAt(), entity.getRecheckNote(),
                recheckHistory, entity.getCreatedAt(), entity.getScopeType(), entity.getDocumentId(),
                entity.getSectionId(), entity.getSectionVersionNo(), entity.getIssueFingerprint(), analysisState
        );
    }

    private List<ReviewItemEntity> synchronize(ContentScope scope, List<Map<String, Object>> proposed) {
        List<ReviewItemEntity> existing = reviewRepository.findBySectionIdOrderByCreatedAtAsc(scope.sectionId());
        Map<String, ReviewItemEntity> existingByFingerprint = existing.stream()
                .filter(item -> item.getIssueFingerprint() != null)
                .collect(Collectors.toMap(ReviewItemEntity::getIssueFingerprint, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<ReviewItemEntity> active = new ArrayList<>();
        for (Map<String, Object> item : proposed) {
            Map<String, Object> range = normalizeRange(scope, map(item.get("targetRange")));
            String type = String.valueOf(item.getOrDefault("reviewType", "unknown"));
            String message = String.valueOf(item.getOrDefault("message", ""));
            String fingerprint = fingerprint(type, message, range);
            ReviewItemEntity entity = existingByFingerprint.remove(fingerprint);
            if (entity == null) {
                entity = new ReviewItemEntity();
                entity.setId(UUID.randomUUID());
                entity.setWorkspaceId(scope.workspaceId());
                entity.setDraftVersionId(null);
                entity.setScopeType("SECTION");
                entity.setDocumentId(scope.documentId());
                entity.setSectionId(scope.sectionId());
                entity.setCreatedAt(OffsetDateTime.now());
                entity.setReviewStatus("OPEN");
            } else if ("SUPERSEDED".equals(entity.getReviewStatus())) {
                entity.setReviewStatus("OPEN");
            }
            entity.setSectionVersionNo(scope.revision());
            entity.setIssueFingerprint(fingerprint);
            entity.setReviewType(type);
            entity.setReviewImpactLevel(impact(String.valueOf(item.getOrDefault("reviewImpactLevel", "NOTICE"))));
            entity.setTargetRangeJson(writeJson(range));
            entity.setMessage(message);
            entity.setSuggestedFix(String.valueOf(item.getOrDefault("suggestedFix", "")));
            entity.setCanBypass(Boolean.parseBoolean(String.valueOf(item.getOrDefault("canBypass", true))));
            active.add(entity);
        }
        existingByFingerprint.values().forEach(item -> {
            item.setReviewStatus("SUPERSEDED");
            item.setResolutionNote("当前章节版本不再检测到该问题");
        });
        reviewRepository.saveAll(existingByFingerprint.values());
        return reviewRepository.saveAll(active).stream()
                .sorted(Comparator.comparing(ReviewItemEntity::getCreatedAt))
                .toList();
    }

    private List<Map<String, Object>> citationItems(ContentScope scope) {
        String content = scope.content();
        boolean hasCitation = APA_CITATION_PATTERN.matcher(content).find() || GBT_CITATION_PATTERN.matcher(content).find();
        boolean hasSources = !scope.sourceTraceMap().isEmpty();
        List<Map<String, Object>> result = new ArrayList<>();
        if (hasSources && !hasCitation) {
            result.add(review("citation_missing", "LOCAL_FIX", scope, 0, Math.min(content.length(), 240),
                    "本章已经绑定材料，但正文中尚未发现可识别的引用标记。",
                    "请在使用材料支撑的论点后插入与当前引用格式一致的正文引用。", true));
        }
        if (!hasSources && hasCitation) {
            result.add(review("reference_orphan", "MUST_CONFIRM", scope, 0, Math.min(content.length(), 240),
                    "本章出现引用标记，但当前章节没有对应的材料来源追溯。",
                    "请上传或关联真实文献并重建可信链，避免保留无法核实的引用。", false));
        }
        return result;
    }

    private Map<String, Object> review(
            String type,
            String level,
            ContentScope scope,
            int start,
            int end,
            String message,
            String fix,
            boolean bypass
    ) {
        return Map.of(
                "reviewType", type,
                "reviewImpactLevel", level,
                "targetRange", Map.of("sectionId", scope.sectionId().toString(), "start", start, "end", end,
                        "selectedText", scope.content().substring(Math.min(start, scope.content().length()), Math.min(end, scope.content().length()))),
                "message", message,
                "suggestedFix", fix,
                "canBypass", bypass
        );
    }

    private Map<String, Object> normalizeRange(ContentScope scope, Map<String, Object> input) {
        int start = Math.max(0, integer(input.get("start"), 0));
        int end = Math.min(scope.content().length(), Math.max(start, integer(input.get("end"), Math.min(scope.content().length(), start + 240))));
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("sectionId", scope.sectionId().toString());
        value.put("start", start);
        value.put("end", end);
        value.put("selectedText", input.getOrDefault("selectedText", scope.content().substring(start, end)));
        return value;
    }

    private boolean overlaps(Map<String, Object> range, int start, int end) {
        int itemStart = integer(range.get("start"), 0);
        int itemEnd = integer(range.get("end"), Integer.MAX_VALUE);
        return itemStart < end && start < itemEnd;
    }

    private List<ReviewRecheckLogResponse> recheckHistory(UUID reviewItemId) {
        return recheckLogRepository.findByReviewItemIdOrderByCreatedAtDesc(reviewItemId).stream().limit(5)
                .map(this::toRecheckLog).toList();
    }

    private ReviewRecheckLogResponse toRecheckLog(ReviewRecheckLogEntity entity) {
        return new ReviewRecheckLogResponse(entity.getId(), entity.getOutcome(), entity.getPreviousStatus(),
                entity.getNextStatus(), entity.getPreviousImpactLevel(), entity.getNextImpactLevel(), entity.getNote(),
                readMap(entity.getBasisJson()), entity.getCreatedAt());
    }

    private String fingerprint(String type, String message, Map<String, Object> range) {
        String selectedText = String.valueOf(range.getOrDefault("selectedText", ""));
        String source = type + "|" + normalize(message) + "|" + normalize(selectedText);
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private ReviewImpactLevel impact(String value) {
        try { return ReviewImpactLevel.valueOf(value.toUpperCase()); } catch (IllegalArgumentException error) { return ReviewImpactLevel.NOTICE; }
    }

    private int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (RuntimeException error) { return fallback; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> map(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map : Map.of();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, Map.class); } catch (JsonProcessingException error) { return Map.of(); }
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException error) { return "{}"; }
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim().toLowerCase();
    }
}
