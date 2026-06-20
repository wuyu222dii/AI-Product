package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.CoWriteResult;
import com.aipm.cowriting.application.dto.cowrite.CoWritePreviewResponse;
import com.aipm.cowriting.application.dto.cowrite.CoWriteRequest;
import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.CoWritePreviewEntity;
import com.aipm.cowriting.domain.entity.CoWritePreviewReviewLinkEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.GenerationStatus;
import com.aipm.cowriting.domain.repository.CoWritePreviewRepository;
import com.aipm.cowriting.domain.repository.CoWritePreviewReviewLinkRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CoWritePreviewApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final DraftVersionRepository draftVersionRepository;
    private final CoWritePreviewRepository coWritePreviewRepository;
    private final CoWritePreviewReviewLinkRepository coWritePreviewReviewLinkRepository;
    private final ReviewItemRepository reviewItemRepository;
    private final OpenAiCoWriteService openAiCoWriteService;
    private final ReviewApplicationService reviewApplicationService;
    private final EvidenceBindingApplicationService evidenceBindingApplicationService;
    private final ObjectMapper objectMapper;

    public CoWritePreviewApplicationService(
            WorkspaceRepository workspaceRepository,
            DraftVersionRepository draftVersionRepository,
            CoWritePreviewRepository coWritePreviewRepository,
            CoWritePreviewReviewLinkRepository coWritePreviewReviewLinkRepository,
            ReviewItemRepository reviewItemRepository,
            OpenAiCoWriteService openAiCoWriteService,
            ReviewApplicationService reviewApplicationService,
            EvidenceBindingApplicationService evidenceBindingApplicationService,
            ObjectMapper objectMapper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.draftVersionRepository = draftVersionRepository;
        this.coWritePreviewRepository = coWritePreviewRepository;
        this.coWritePreviewReviewLinkRepository = coWritePreviewReviewLinkRepository;
        this.reviewItemRepository = reviewItemRepository;
        this.openAiCoWriteService = openAiCoWriteService;
        this.reviewApplicationService = reviewApplicationService;
        this.evidenceBindingApplicationService = evidenceBindingApplicationService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CoWritePreviewResponse preview(UUID workspaceId, CoWriteRequest request) {
        workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在"));
        DraftVersionEntity current = draftVersionRepository.findById(request.draftVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        if (!workspaceId.equals(current.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不属于当前 workspace");
        }

        Map<String, Object> targetRange = normalizeTargetRange(request.targetRange());
        Map<String, Object> controls = request.controls() == null ? Map.of() : request.controls();
        CoWriteResult rewritten = openAiCoWriteService.coWrite(
                request.action(),
                request.instruction(),
                controls,
                targetRange,
                current.getTitleSuggestion(),
                current.getDraftText(),
                readMap(current.getOutlineJson()),
                readMap(current.getSourceTraceMapJson())
        );

        List<ReviewItemEntity> currentReviews = reviewItemRepository.findByDraftVersionIdOrderByCreatedAtAsc(current.getId());

        CoWritePreviewEntity entity = new CoWritePreviewEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setDraftVersionId(current.getId());
        entity.setAction(request.action());
        entity.setTargetRangeJson(writeJson(targetRange));
        entity.setInstruction(cleanInstruction(request.instruction()));
        entity.setControlsJson(writeJson(controls));
        entity.setCandidateTitleSuggestion(rewritten.titleSuggestion());
        entity.setCandidateDraftText(rewritten.draftText());
        entity.setCandidateSourceTraceMapJson(writeJson(rewritten.sourceTraceMap()));
        entity.setDiffSummaryJson(writeJson(diffSummary(
                current.getDraftText(),
                rewritten.draftText(),
                targetRange,
                controls,
                currentReviews
        )));
        entity.setStatus("READY");
        entity.setCreatedAt(OffsetDateTime.now());

        CoWritePreviewEntity saved = coWritePreviewRepository.save(entity);
        saveReviewLinks(saved.getId(), currentReviews, targetRange);
        return toResponse(saved);
    }

    @Transactional
    public DraftResponse apply(UUID previewId) {
        CoWritePreviewEntity preview = coWritePreviewRepository.findById(previewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "co-write preview 不存在"));
        if (!"READY".equals(preview.getStatus())) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATUS_CONFLICT, HttpStatus.CONFLICT.value(), "该共写预览已经处理，不能重复应用");
        }
        DraftVersionEntity current = draftVersionRepository.findById(preview.getDraftVersionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        WorkspaceEntity workspace = workspaceRepository.findById(preview.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在"));
        int nextVersion = draftVersionRepository.findFirstByWorkspaceIdOrderByVersionNoDesc(preview.getWorkspaceId())
                .map(draft -> draft.getVersionNo() + 1)
                .orElse(current.getVersionNo() + 1);

        DraftVersionEntity next = new DraftVersionEntity();
        next.setId(UUID.randomUUID());
        next.setWorkspaceId(preview.getWorkspaceId());
        next.setVersionNo(nextVersion);
        next.setTitleSuggestion(preview.getCandidateTitleSuggestion());
        next.setOutlineJson(current.getOutlineJson());
        next.setParagraphSkeletonsJson(current.getParagraphSkeletonsJson());
        next.setDraftText(preview.getCandidateDraftText());
        next.setSourceTraceMapJson(preview.getCandidateSourceTraceMapJson());
        next.setGenerationStatus(GenerationStatus.SUCCESS);
        next.setCreatedBy("system-ai");
        next.setCreatedAt(OffsetDateTime.now());
        draftVersionRepository.save(next);

        reviewApplicationService.refreshForDraft(next);
        evidenceBindingApplicationService.rebuild(next.getId());

        workspace.setCurrentDraftVersionId(next.getId());
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);

        preview.setStatus("APPLIED");
        preview.setAppliedAt(OffsetDateTime.now());
        coWritePreviewRepository.save(preview);

        return toDraftResponse(next);
    }

    public CoWritePreviewResponse discard(UUID previewId) {
        CoWritePreviewEntity preview = coWritePreviewRepository.findById(previewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "co-write preview 不存在"));
        if (!"READY".equals(preview.getStatus())) {
            return toResponse(preview);
        }
        preview.setStatus("DISCARDED");
        return toResponse(coWritePreviewRepository.save(preview));
    }

    private Map<String, Object> diffSummary(
            String original,
            String candidate,
            Map<String, Object> targetRange,
            Map<String, Object> controls,
            List<ReviewItemEntity> currentReviews
    ) {
        String before = original == null ? "" : original;
        String after = candidate == null ? "" : candidate;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("originalLength", before.length());
        summary.put("candidateLength", after.length());
        summary.put("lengthDelta", after.length() - before.length());
        summary.put("changed", !before.equals(after));
        summary.put("targetMode", String.valueOf(targetRange.getOrDefault("mode", "full_draft")));
        summary.put("guardrails", Map.of(
                "keepCitations", Boolean.parseBoolean(String.valueOf(controls.getOrDefault("keepCitations", false))),
                "keepData", Boolean.parseBoolean(String.valueOf(controls.getOrDefault("keepData", false))),
                "noNewSources", Boolean.parseBoolean(String.valueOf(controls.getOrDefault("noNewSources", false))),
                "keepStudentVoice", Boolean.parseBoolean(String.valueOf(controls.getOrDefault("keepStudentVoice", false))),
                "rewriteDepth", String.valueOf(controls.getOrDefault("rewriteDepth", "balanced"))
        ));
        summary.put("paragraphDiffs", paragraphDiffs(before, after));
        summary.put("conflictWarnings", conflictWarnings(before, after, controls));
        summary.put("recheckSuggestion", recheckSuggestion(currentReviews, targetRange));
        return summary;
    }

    private List<Map<String, Object>> paragraphDiffs(String original, String candidate) {
        List<String> before = splitParagraphs(original);
        List<String> after = splitParagraphs(candidate);
        int max = Math.max(before.size(), after.size());
        List<Map<String, Object>> rows = new ArrayList<>();
        for (int index = 0; index < max; index++) {
            String originalParagraph = index < before.size() ? before.get(index) : "";
            String candidateParagraph = index < after.size() ? after.get(index) : "";
            if (originalParagraph.equals(candidateParagraph)) {
                continue;
            }
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("paragraphId", "p" + (index + 1));
            row.put("changeType", originalParagraph.isBlank() ? "added" : candidateParagraph.isBlank() ? "removed" : "modified");
            row.put("originalText", originalParagraph);
            row.put("candidateText", candidateParagraph);
            row.put("intentLabel", inferIntentLabel(originalParagraph, candidateParagraph));
            row.put("selectedByDefault", !candidateParagraph.isBlank());
            rows.add(row);
            if (rows.size() >= 12) {
                break;
            }
        }
        return rows;
    }

    private List<Map<String, Object>> conflictWarnings(String original, String candidate, Map<String, Object> controls) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        boolean keepCitations = Boolean.parseBoolean(String.valueOf(controls.getOrDefault("keepCitations", false)));
        boolean keepData = Boolean.parseBoolean(String.valueOf(controls.getOrDefault("keepData", false)));
        boolean noNewSources = Boolean.parseBoolean(String.valueOf(controls.getOrDefault("noNewSources", false)));
        int citationDelta = citationCount(candidate) - citationCount(original);
        int numberDelta = numberCount(candidate) - numberCount(original);
        if (keepCitations && citationDelta < 0) {
            warnings.add(conflict("CITATION_LOST", "可能丢失引用", "候选正文比原文少 " + Math.abs(citationDelta) + " 个引用标记，应用前请核对。", "HIGH"));
        }
        if (noNewSources && citationDelta > 1) {
            warnings.add(conflict("NEW_SOURCE_RISK", "可能新增来源", "候选正文新增了较多引用标记，请确认不是 AI 编造来源。", "HIGH"));
        }
        if (keepData && numberDelta < 0) {
            warnings.add(conflict("DATA_CHANGED", "数字可能变化", "候选正文数字数量减少，涉及数据或年份时建议人工核对。", "MEDIUM"));
        }
        if (warnings.isEmpty()) {
            warnings.add(conflict("NO_MAJOR_CONFLICT", "未发现明显冲突", "引用、数字和来源数量未发现明显异常，仍建议预览后再应用。", "LOW"));
        }
        return warnings;
    }

    private Map<String, Object> recheckSuggestion(List<ReviewItemEntity> currentReviews, Map<String, Object> targetRange) {
        List<Map<String, Object>> relatedItems = relatedReviewItems(currentReviews, targetRange);
        long openCount = currentReviews == null ? 0 : currentReviews.stream()
                .filter(item -> "OPEN".equalsIgnoreCase(item.getReviewStatus()))
                .count();
        return Map.of(
                "shouldRecheck", !relatedItems.isEmpty(),
                "reviewItemCount", relatedItems.size(),
                "openReviewItemCount", openCount,
                "relatedReviewItems", relatedItems,
                "message", !relatedItems.isEmpty()
                        ? "应用后建议复查本次共写可能影响的待处理审查项；复查为单项 AI 调用，不会自动全篇重审。"
                        : "当前没有明显待处理审查项，应用后可按需手动复查。"
        );
    }

    private List<Map<String, Object>> relatedReviewItems(List<ReviewItemEntity> currentReviews, Map<String, Object> targetRange) {
        if (currentReviews == null || currentReviews.isEmpty()) {
            return List.of();
        }
        return currentReviews.stream()
                .filter(item -> "OPEN".equalsIgnoreCase(item.getReviewStatus()))
                .map(item -> reviewLinkCandidate(item, targetRange))
                .filter(candidate -> Boolean.TRUE.equals(candidate.get("related")))
                .limit(6)
                .map(candidate -> Map.of(
                        "reviewItemId", candidate.get("reviewItemId"),
                        "relationType", candidate.get("relationType"),
                        "reason", candidate.get("reason")
                ))
                .toList();
    }

    private Map<String, Object> reviewLinkCandidate(ReviewItemEntity item, Map<String, Object> targetRange) {
        Map<String, Object> reviewRange = readMap(item.getTargetRangeJson());
        String targetMode = String.valueOf(targetRange.getOrDefault("mode", "full_draft"));
        boolean fullDraft = "full_draft".equals(targetMode);
        boolean overlap = rangesOverlap(targetRange, reviewRange);
        if (fullDraft) {
            return Map.of(
                    "related", true,
                    "reviewItemId", item.getId(),
                    "relationType", "FULL_DRAFT_MAY_ADDRESS",
                    "reason", "本次共写作用于全文，可能影响该待处理审查项。"
            );
        }
        if (overlap) {
            return Map.of(
                    "related", true,
                    "reviewItemId", item.getId(),
                    "relationType", "TARGET_RANGE_OVERLAP",
                    "reason", "本次共写范围与该审查项定位范围重叠。"
            );
        }
        return Map.of(
                "related", false,
                "reviewItemId", item.getId(),
                "relationType", "NO_DIRECT_MATCH",
                "reason", "本次共写范围未直接命中该审查项。"
        );
    }

    private void saveReviewLinks(UUID previewId, List<ReviewItemEntity> currentReviews, Map<String, Object> targetRange) {
        List<CoWritePreviewReviewLinkEntity> links = relatedReviewItems(currentReviews, targetRange).stream()
                .map(item -> {
                    CoWritePreviewReviewLinkEntity entity = new CoWritePreviewReviewLinkEntity();
                    entity.setId(UUID.randomUUID());
                    entity.setCoWritePreviewId(previewId);
                    entity.setReviewItemId((UUID) item.get("reviewItemId"));
                    entity.setRelationType(String.valueOf(item.get("relationType")));
                    entity.setRelationReason(String.valueOf(item.get("reason")));
                    entity.setRecheckPrompted(false);
                    entity.setCreatedAt(OffsetDateTime.now());
                    return entity;
                })
                .toList();
        if (!links.isEmpty()) {
            coWritePreviewReviewLinkRepository.saveAll(links);
        }
    }

    private boolean rangesOverlap(Map<String, Object> targetRange, Map<String, Object> reviewRange) {
        Integer targetStart = asInt(targetRange.get("start"));
        Integer targetEnd = asInt(targetRange.get("end"));
        Integer reviewStart = asInt(reviewRange.get("start"));
        Integer reviewEnd = asInt(reviewRange.get("end"));
        if (targetStart == null || targetEnd == null || reviewStart == null || reviewEnd == null) {
            return false;
        }
        return targetStart < reviewEnd && reviewStart < targetEnd;
    }

    private Integer asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || String.valueOf(value).isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<String, Object> conflict(String code, String title, String message, String level) {
        return Map.of("code", code, "title", title, "message", message, "level", level);
    }

    private List<String> splitParagraphs(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(text.split("\\R\\R+"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }

    private String inferIntentLabel(String original, String candidate) {
        if (candidate.length() > original.length() + 80) {
            return "补充论证";
        }
        if (original.length() > candidate.length() + 80) {
            return "压缩重复";
        }
        if (citationCount(candidate) > citationCount(original)) {
            return "补充引用";
        }
        if (numberCount(candidate) != numberCount(original)) {
            return "数据相关修改";
        }
        return "表达优化";
    }

    private int citationCount(String text) {
        String source = text == null ? "" : text;
        java.util.regex.Matcher apa = java.util.regex.Pattern.compile("[（(][^）)]{1,80}[，,]\\s*((19|20)\\d{2}|n\\.d\\.)[）)]").matcher(source);
        java.util.regex.Matcher gbt = java.util.regex.Pattern.compile("\\[(\\d{1,3})]").matcher(source);
        int count = 0;
        while (apa.find()) count++;
        while (gbt.find()) count++;
        return count;
    }

    private int numberCount(String text) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("\\d+(?:\\.\\d+)?%?").matcher(text == null ? "" : text);
        int count = 0;
        while (matcher.find()) count++;
        return count;
    }

    private CoWritePreviewResponse toResponse(CoWritePreviewEntity entity) {
        return new CoWritePreviewResponse(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getDraftVersionId(),
                entity.getAction(),
                readMap(entity.getTargetRangeJson()),
                entity.getInstruction(),
                readMap(entity.getControlsJson()),
                entity.getCandidateTitleSuggestion(),
                entity.getCandidateDraftText(),
                readMap(entity.getCandidateSourceTraceMapJson()),
                readMap(entity.getDiffSummaryJson()),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getAppliedAt()
        );
    }

    private DraftResponse toDraftResponse(DraftVersionEntity entity) {
        return new DraftResponse(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getVersionNo(),
                entity.getTitleSuggestion(),
                readMap(entity.getOutlineJson()),
                readObject(entity.getParagraphSkeletonsJson()),
                entity.getDraftText(),
                readMap(entity.getSourceTraceMapJson()),
                entity.getGenerationStatus(),
                entity.getCreatedBy(),
                entity.getCreatedAt()
        );
    }

    private Map<String, Object> normalizeTargetRange(Map<String, Object> targetRange) {
        if (targetRange == null || targetRange.isEmpty()) {
            return Map.of("mode", "full_draft");
        }
        if (!targetRange.containsKey("mode") && targetRange.containsKey("start") && targetRange.containsKey("end")) {
            Map<String, Object> normalized = new LinkedHashMap<>(targetRange);
            normalized.put("mode", "selection");
            return normalized;
        }
        return targetRange;
    }

    private String cleanInstruction(String instruction) {
        return instruction == null || instruction.isBlank() ? null : instruction.trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(defaultJson(json, "{}"), Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 反序列化失败");
        }
    }

    private Object readObject(String json) {
        try {
            return objectMapper.readValue(defaultJson(json, "[]"), Object.class);
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

    private String defaultJson(String json, String fallback) {
        return json == null || json.isBlank() ? fallback : json;
    }
}
