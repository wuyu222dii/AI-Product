package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.CoWriteResult;
import com.aipm.cowriting.application.dto.cowrite.CoWritePreviewResponse;
import com.aipm.cowriting.application.dto.cowrite.CoWriteRequest;
import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.CoWritePreviewEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.GenerationStatus;
import com.aipm.cowriting.domain.repository.CoWritePreviewRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
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
    private final OpenAiCoWriteService openAiCoWriteService;
    private final ReviewApplicationService reviewApplicationService;
    private final EvidenceBindingApplicationService evidenceBindingApplicationService;
    private final ObjectMapper objectMapper;

    public CoWritePreviewApplicationService(
            WorkspaceRepository workspaceRepository,
            DraftVersionRepository draftVersionRepository,
            CoWritePreviewRepository coWritePreviewRepository,
            OpenAiCoWriteService openAiCoWriteService,
            ReviewApplicationService reviewApplicationService,
            EvidenceBindingApplicationService evidenceBindingApplicationService,
            ObjectMapper objectMapper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.draftVersionRepository = draftVersionRepository;
        this.coWritePreviewRepository = coWritePreviewRepository;
        this.openAiCoWriteService = openAiCoWriteService;
        this.reviewApplicationService = reviewApplicationService;
        this.evidenceBindingApplicationService = evidenceBindingApplicationService;
        this.objectMapper = objectMapper;
    }

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
        entity.setDiffSummaryJson(writeJson(diffSummary(current.getDraftText(), rewritten.draftText(), targetRange, controls)));
        entity.setStatus("READY");
        entity.setCreatedAt(OffsetDateTime.now());

        return toResponse(coWritePreviewRepository.save(entity));
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

    private Map<String, Object> diffSummary(String original, String candidate, Map<String, Object> targetRange, Map<String, Object> controls) {
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
        return summary;
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
