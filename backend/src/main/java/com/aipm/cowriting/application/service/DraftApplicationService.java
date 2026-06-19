package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.CoWriteResult;
import com.aipm.cowriting.application.dto.ai.DraftGenerationResult;
import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.draft.UpdateDraftRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.MaterialSufficiencyResultEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.GenerationStatus;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.MaterialSufficiencyResultRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class DraftApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final RequirementSnapshotRepository snapshotRepository;
    private final MaterialSufficiencyResultRepository sufficiencyResultRepository;
    private final DraftVersionRepository draftVersionRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final OpenAiDraftGenerationService openAiDraftGenerationService;
    private final OpenAiCoWriteService openAiCoWriteService;
    private final ReviewApplicationService reviewApplicationService;
    private final EvidenceBindingApplicationService evidenceBindingApplicationService;
    private final ObjectMapper objectMapper;
    private final JobApplicationService jobApplicationService;

    public DraftApplicationService(
            WorkspaceRepository workspaceRepository,
            RequirementSnapshotRepository snapshotRepository,
            MaterialSufficiencyResultRepository sufficiencyResultRepository,
            DraftVersionRepository draftVersionRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            OpenAiDraftGenerationService openAiDraftGenerationService,
            OpenAiCoWriteService openAiCoWriteService,
            ReviewApplicationService reviewApplicationService,
            EvidenceBindingApplicationService evidenceBindingApplicationService,
            ObjectMapper objectMapper,
            JobApplicationService jobApplicationService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.snapshotRepository = snapshotRepository;
        this.sufficiencyResultRepository = sufficiencyResultRepository;
        this.draftVersionRepository = draftVersionRepository;
        this.materialRepository = materialRepository;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.openAiDraftGenerationService = openAiDraftGenerationService;
        this.openAiCoWriteService = openAiCoWriteService;
        this.reviewApplicationService = reviewApplicationService;
        this.evidenceBindingApplicationService = evidenceBindingApplicationService;
        this.objectMapper = objectMapper;
        this.jobApplicationService = jobApplicationService;
    }

    public JobResponse generate(UUID workspaceId, UUID requirementSnapshotId, String mode) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在"));
        RequirementSnapshotEntity snapshot = snapshotRepository.findById(requirementSnapshotId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REQUIREMENT_SNAPSHOT_MISSING, HttpStatus.NOT_FOUND.value(), "requirement snapshot 不存在"));
        MaterialSufficiencyResultEntity sufficiency = sufficiencyResultRepository.findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INSUFFICIENT_MATERIAL, HttpStatus.UNPROCESSABLE_ENTITY.value(), "尚未完成材料充足性检查"));

        if (!sufficiency.isGenerationEligible()) {
            throw new BusinessException(
                    ErrorCode.GENERATION_NOT_ELIGIBLE,
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    "根据当前上传内容，暂时无法生成正文框架/初稿"
            );
        }

        int nextVersion = draftVersionRepository.findFirstByWorkspaceIdOrderByVersionNoDesc(workspaceId)
                .map(draft -> draft.getVersionNo() + 1)
                .orElse(1);

        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<AiSemanticParseResultEntity> parseResults = aiSemanticParseResultRepository.findByMaterialIdIn(
                materials.stream().map(MaterialEntity::getId).toList()
        );

        Map<String, Object> requirementContext = new LinkedHashMap<>();
        requirementContext.put("topic", snapshot.getTopic());
        requirementContext.put("wordCount", snapshot.getWordCount());
        requirementContext.put("deadline", snapshot.getDeadline() == null ? null : snapshot.getDeadline().toString());
        requirementContext.put("citationStyle", snapshot.getCitationStyle());
        requirementContext.put("specialRequirements", readMap(snapshot.getSpecialRequirementsJson()));

        List<Map<String, Object>> materialContext = parseResults.stream()
                .map(parseResult -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("materialId", parseResult.getMaterialId().toString());
                    item.put("materialCategory", effectiveCategory(parseResult).name());
                    item.put("summary", defaultString(parseResult.getSummary()));
                    item.put("topicRelation", defaultString(parseResult.getTopicRelation()));
                    item.put("detectedClaims", readObject(parseResult.getDetectedClaimsJson()));
                    item.put("detectedEvidence", readObject(parseResult.getDetectedEvidenceJson()));
                    item.put("detectedRequirements", readObject(parseResult.getDetectedRequirementsJson()));
                    item.put("bibliographicMetadata", readObject(defaultJson(parseResult.getBibliographicMetadataJson(), "{}")));
                    item.put("confidenceScore", parseResult.getConfidenceScore() == null ? null : parseResult.getConfidenceScore().doubleValue());
                    return item;
                })
                .toList();

        DraftGenerationResult generated = openAiDraftGenerationService.generate(
                requirementContext,
                materialContext,
                normalizeMode(mode)
        );

        DraftVersionEntity draft = new DraftVersionEntity();
        draft.setId(UUID.randomUUID());
        draft.setWorkspaceId(workspaceId);
        draft.setVersionNo(nextVersion);
        draft.setTitleSuggestion(generated.titleSuggestion());
        draft.setOutlineJson(writeJson(generated.outline()));
        draft.setParagraphSkeletonsJson(writeJson(generated.paragraphSkeletons()));
        draft.setDraftText(generated.draftText());
        draft.setSourceTraceMapJson(writeJson(generated.sourceTraceMap()));
        draft.setGenerationStatus(GenerationStatus.SUCCESS);
        draft.setCreatedBy("system-ai");
        draft.setCreatedAt(OffsetDateTime.now());
        draftVersionRepository.save(draft);
        reviewApplicationService.refreshForDraft(draft);
        evidenceBindingApplicationService.rebuild(draft.getId());

        workspace.setCurrentDraftVersionId(draft.getId());
        workspace.setStatus(WorkspaceStatus.READY);
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);

        UUID jobId = jobApplicationService.createJob("draft_generate", "success", workspaceId);
        return new JobResponse(jobId.toString(), "success");
    }

    public DraftResponse get(UUID draftId) {
        DraftVersionEntity entity = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return toResponse(entity);
    }

    public DraftResponse updateDraft(UUID draftId, UpdateDraftRequest request) {
        DraftVersionEntity entity = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        if (request.titleSuggestion() != null && !request.titleSuggestion().isBlank()) {
            entity.setTitleSuggestion(request.titleSuggestion().trim());
        }
        entity.setDraftText(request.draftText());
        draftVersionRepository.save(entity);
        return toResponse(entity);
    }

    public DraftResponse restore(UUID draftId) {
        DraftVersionEntity entity = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        WorkspaceEntity workspace = workspaceRepository.findById(entity.getWorkspaceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在"));

        workspace.setCurrentDraftVersionId(entity.getId());
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);

        return toResponse(entity);
    }

    public List<DraftResponse> listByWorkspace(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在");
        }
        return draftVersionRepository.findByWorkspaceIdOrderByVersionNoDesc(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    public JobResponse coWrite(UUID workspaceId, UUID draftVersionId, String action, Map<String, Object> targetRange, String instruction, Map<String, Object> controls) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在"));
        DraftVersionEntity current = draftVersionRepository.findById(draftVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));

        int nextVersion = draftVersionRepository.findFirstByWorkspaceIdOrderByVersionNoDesc(workspaceId)
                .map(draft -> draft.getVersionNo() + 1)
                .orElse(current.getVersionNo() + 1);

        CoWriteResult rewritten = openAiCoWriteService.coWrite(
                action,
                instruction,
                controls == null ? Map.of() : controls,
                normalizeTargetRange(targetRange),
                current.getTitleSuggestion(),
                current.getDraftText(),
                readMap(current.getOutlineJson()),
                readMap(current.getSourceTraceMapJson())
        );

        DraftVersionEntity next = new DraftVersionEntity();
        next.setId(UUID.randomUUID());
        next.setWorkspaceId(workspaceId);
        next.setVersionNo(nextVersion);
        next.setTitleSuggestion(rewritten.titleSuggestion());
        next.setOutlineJson(current.getOutlineJson());
        next.setParagraphSkeletonsJson(current.getParagraphSkeletonsJson());
        next.setDraftText(rewritten.draftText());
        next.setSourceTraceMapJson(writeJson(rewritten.sourceTraceMap()));
        next.setGenerationStatus(GenerationStatus.SUCCESS);
        next.setCreatedBy("system-ai");
        next.setCreatedAt(OffsetDateTime.now());
        draftVersionRepository.save(next);
        reviewApplicationService.refreshForDraft(next);
        evidenceBindingApplicationService.rebuild(next.getId());

        workspace.setCurrentDraftVersionId(next.getId());
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);

        UUID jobId = jobApplicationService.createJob("co_write_" + action, "success", workspaceId);
        return new JobResponse(jobId.toString(), "success");
    }

    private Map<String, Object> normalizeTargetRange(Map<String, Object> targetRange) {
        if (targetRange == null || targetRange.isEmpty()) {
            return Map.of("mode", "full_draft");
        }
        return targetRange;
    }

    private DraftResponse toResponse(DraftVersionEntity entity) {
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

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 反序列化失败");
        }
    }

    private Object readObject(String json) {
        try {
            return objectMapper.readValue(json, Object.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 反序列化失败");
        }
    }

    private String defaultJson(String json, String fallback) {
        return json == null || json.isBlank() ? fallback : json;
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }

    private MaterialCategory effectiveCategory(AiSemanticParseResultEntity parseResult) {
        return parseResult.getManualMaterialCategory() != null
                ? parseResult.getManualMaterialCategory()
                : parseResult.getMaterialCategory();
    }

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank() || "default".equalsIgnoreCase(mode)) {
            return "stable";
        }
        return mode.trim().toLowerCase();
    }
}
