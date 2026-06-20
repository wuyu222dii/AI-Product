package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.SemanticParseResult;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.material.MaterialResponse;
import com.aipm.cowriting.application.dto.material.MaterialPreviewResponse;
import com.aipm.cowriting.application.dto.material.UpdateBibliographicMetadataRequest;
import com.aipm.cowriting.application.dto.material.UpdateMaterialCategoryRequest;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class MaterialApplicationService {

    private final MaterialRepository materialRepository;
    private final WorkspaceRepository workspaceRepository;
    private final JobApplicationService jobApplicationService;
    private final OpenAiSemanticParsingService openAiSemanticParsingService;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final LocalMaterialStorageService localMaterialStorageService;
    private final MaterialExtractionService materialExtractionService;
    private final ParseQualityService parseQualityService;
    private final ObjectMapper objectMapper;

    public MaterialApplicationService(
            MaterialRepository materialRepository,
            WorkspaceRepository workspaceRepository,
            JobApplicationService jobApplicationService,
            OpenAiSemanticParsingService openAiSemanticParsingService,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            LocalMaterialStorageService localMaterialStorageService,
            MaterialExtractionService materialExtractionService,
            ParseQualityService parseQualityService,
            ObjectMapper objectMapper
    ) {
        this.materialRepository = materialRepository;
        this.workspaceRepository = workspaceRepository;
        this.jobApplicationService = jobApplicationService;
        this.openAiSemanticParsingService = openAiSemanticParsingService;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.localMaterialStorageService = localMaterialStorageService;
        this.materialExtractionService = materialExtractionService;
        this.parseQualityService = parseQualityService;
        this.objectMapper = objectMapper;
    }

    public List<MaterialResponse> list(UUID workspaceId) {
        assertWorkspaceExists(workspaceId);
        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        Map<UUID, AiSemanticParseResultEntity> parseResults = buildParseResultMap(materials);
        return materials.stream()
                .map(material -> toResponse(material, parseResults.get(material.getId())))
                .toList();
    }

    public MaterialPreviewResponse preview(UUID materialId) {
        MaterialEntity material = getMaterial(materialId);
        String downloadUrl = material.getStoragePath() == null || material.getStoragePath().isBlank()
                ? null
                : "/api/v1/materials/" + material.getId() + "/file";
        String previewType = downloadUrl != null
                ? "file"
                : material.getExternalLink() != null && !material.getExternalLink().isBlank()
                ? "external_link"
                : "text";
        String text = firstNonBlank(material.getPlainTextContent(), material.getSupplementText(), material.getExternalLink());
        return new MaterialPreviewResponse(
                material.getId(),
                material.getFilename(),
                material.getFileType(),
                previewType,
                snippet(text, 2400),
                downloadUrl,
                material.getExternalLink()
        );
    }

    public MaterialEntity getMaterialForFile(UUID materialId) {
        MaterialEntity material = getMaterial(materialId);
        if (material.getStoragePath() == null || material.getStoragePath().isBlank()) {
            throw new BusinessException(
                    ErrorCode.MATERIAL_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value(),
                    "该材料没有可预览的原始文件"
            );
        }
        return material;
    }

    public MaterialResponse updateCategory(UUID materialId, UpdateMaterialCategoryRequest request) {
        MaterialEntity material = getMaterial(materialId);
        AiSemanticParseResultEntity parseResult = aiSemanticParseResultRepository.findByMaterialId(materialId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MATERIAL_PARSE_INCOMPLETE,
                        HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "材料尚未完成 AI 解析，无法手动纠正分类"
                ));
        parseResult.setManualMaterialCategory(request.materialCategory());
        aiSemanticParseResultRepository.save(parseResult);
        return toResponse(material, parseResult);
    }

    public MaterialResponse updateBibliographicMetadata(UUID materialId, UpdateBibliographicMetadataRequest request) {
        MaterialEntity material = getMaterial(materialId);
        AiSemanticParseResultEntity parseResult = aiSemanticParseResultRepository.findByMaterialId(materialId)
                .orElseGet(AiSemanticParseResultEntity::new);
        parseResult.setId(parseResult.getId() == null ? UUID.randomUUID() : parseResult.getId());
        parseResult.setMaterialId(materialId);
        parseResult.setMaterialCategory(parseResult.getMaterialCategory() == null
                ? MaterialCategory.UNKNOWN
                : parseResult.getMaterialCategory());
        parseResult.setDetectedClaimsJson(defaultJson(parseResult.getDetectedClaimsJson(), "[]"));
        parseResult.setDetectedEvidenceJson(defaultJson(parseResult.getDetectedEvidenceJson(), "[]"));
        parseResult.setDetectedRequirementsJson(defaultJson(parseResult.getDetectedRequirementsJson(), "[]"));
        parseResult.setBibliographicMetadataJson(writeJson(new BibliographicMetadata(
                request.authors(),
                request.year(),
                request.title(),
                request.sourceTitle(),
                request.publisher(),
                request.url(),
                request.doi(),
                request.publicationType()
        )));
        parseResult.setConfidenceScore(parseResult.getConfidenceScore() == null
                ? BigDecimal.valueOf(0.5d)
                : parseResult.getConfidenceScore());
        parseResult.setCreatedAt(parseResult.getCreatedAt() == null ? OffsetDateTime.now() : parseResult.getCreatedAt());
        aiSemanticParseResultRepository.save(parseResult);

        if (material.getParseStage() != ParseStage.AI_PARSED) {
            material.setParseStage(ParseStage.AI_PARTIAL);
        }
        materialRepository.save(material);
        return toResponse(material, parseResult);
    }

    public MaterialResponse createStub(
            UUID workspaceId,
            String filename,
            String fileType,
            String sourceType,
            boolean isKeyMaterial,
            String storagePath,
            String plainText,
            String externalLink
    ) {
        WorkspaceEntity workspace = assertWorkspaceExists(workspaceId);
        MaterialEntity entity = new MaterialEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setFilename(filename);
        entity.setFileType(fileType);
        entity.setSourceType(sourceType);
        entity.setRawFileUrl("/mock/" + entity.getId());
        entity.setStoragePath(storagePath);
        entity.setPlainTextContent(plainText);
        entity.setExternalLink(externalLink);
        entity.setKeyMaterial(isKeyMaterial);
        entity.setParseStage(ParseStage.PREPROCESSED);
        entity.setConfidenceScore(BigDecimal.ZERO);
        entity.setCreatedAt(OffsetDateTime.now());
        materialRepository.save(entity);

        workspace.setStatus(WorkspaceStatus.PROCESSING);
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);

        return toResponse(entity, null);
    }

    private Map<UUID, AiSemanticParseResultEntity> buildParseResultMap(List<MaterialEntity> materials) {
        if (materials.isEmpty()) {
            return Map.of();
        }
        List<AiSemanticParseResultEntity> parseResults = aiSemanticParseResultRepository.findByMaterialIdIn(
                materials.stream().map(MaterialEntity::getId).toList()
        );
        Map<UUID, AiSemanticParseResultEntity> resultMap = new LinkedHashMap<>();
        for (AiSemanticParseResultEntity parseResult : parseResults) {
            resultMap.put(parseResult.getMaterialId(), parseResult);
        }
        return resultMap;
    }

    public JobResponse triggerPreprocess(UUID materialId) {
        MaterialEntity material = getMaterial(materialId);
        if ((material.getPlainTextContent() == null || material.getPlainTextContent().isBlank())
                && material.getStoragePath() != null && !material.getStoragePath().isBlank()) {
            String extracted = materialExtractionService.extract(
                    localMaterialStorageService.resolve(material.getStoragePath()),
                    material.getFileType()
            );
            material.setPlainTextContent(extracted);
        }
        material.setParseStage(ParseStage.PREPROCESSED);
        materialRepository.save(material);
        UUID jobId = jobApplicationService.createJob("preprocess", "success", material.getWorkspaceId());
        return new JobResponse(jobId.toString(), "success");
    }

    public JobResponse triggerAiParse(UUID materialId) {
        MaterialEntity material = getMaterial(materialId);
        String aiInput = buildAiInput(material);
        SemanticParseResult result = openAiSemanticParsingService.parse(aiInput, material.getFilename());

        AiSemanticParseResultEntity parseResult = aiSemanticParseResultRepository.findByMaterialId(materialId)
                .orElseGet(AiSemanticParseResultEntity::new);
        parseResult.setId(parseResult.getId() == null ? UUID.randomUUID() : parseResult.getId());
        parseResult.setMaterialId(materialId);
        parseResult.setMaterialCategory(parseMaterialCategory(result.materialCategory(), aiInput));
        parseResult.setSummary(result.summary());
        parseResult.setTopicRelation(result.topicRelation());
        parseResult.setDetectedClaimsJson(writeJson(result.detectedClaims()));
        parseResult.setDetectedEvidenceJson(writeJson(result.detectedEvidence()));
        parseResult.setDetectedRequirementsJson(writeJson(result.detectedRequirements()));
        parseResult.setBibliographicMetadataJson(writeJson(result.bibliographicMetadata() == null
                ? BibliographicMetadata.empty()
                : result.bibliographicMetadata()));
        parseResult.setConfidenceScore(result.confidenceScore());
        parseResult.setCreatedAt(OffsetDateTime.now());
        aiSemanticParseResultRepository.save(parseResult);

        material.setParseStage(ParseStage.AI_PARSED);
        material.setConfidenceScore(result.confidenceScore() == null ? BigDecimal.valueOf(0.5d) : result.confidenceScore());
        materialRepository.save(material);
        UUID jobId = jobApplicationService.createJob("semantic_parse", "success", material.getWorkspaceId());
        return new JobResponse(jobId.toString(), "success");
    }

    public void supplement(UUID materialId, String supplementText, Integer pageRef) {
        MaterialEntity material = getMaterial(materialId);
        String existing = material.getSupplementText();
        String addition = pageRef == null
                ? supplementText
                : "[补充页码 " + pageRef + "] " + supplementText;
        material.setSupplementText(
                existing == null || existing.isBlank()
                        ? addition
                        : existing + "\n" + addition
        );
        material.setParseStage(ParseStage.PREPROCESSED);
        materialRepository.save(material);
    }

    private String buildAiInput(MaterialEntity material) {
        StringBuilder builder = new StringBuilder();
        builder.append("Material filename: ").append(material.getFilename() == null ? "" : material.getFilename().trim());
        if (material.getExternalLink() != null && !material.getExternalLink().isBlank()) {
            builder.append("\nExternal reference link: ").append(material.getExternalLink().trim());
        }
        if (material.getPlainTextContent() != null && !material.getPlainTextContent().isBlank()) {
            builder.append("\n\nExtracted text:\n");
            builder.append(material.getPlainTextContent().trim());
        }
        if (material.getSupplementText() != null && !material.getSupplementText().isBlank()) {
            if (!builder.isEmpty()) {
                builder.append("\n\n");
            }
            builder.append("User supplemental context:\n").append(material.getSupplementText().trim());
        }
        return builder.toString();
    }

    private WorkspaceEntity assertWorkspaceExists(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        "workspace 不存在"
                ));
    }

    private MaterialEntity getMaterial(UUID materialId) {
        return materialRepository.findById(materialId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.MATERIAL_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        "material 不存在"
                ));
    }

    private MaterialCategory parseMaterialCategory(String category, String content) {
        String normalizedContent = content == null ? "" : content;

        if (looksLikeAssignmentRequirement(normalizedContent)) {
            return MaterialCategory.ASSIGNMENT_REQUIREMENT;
        }
        if (looksLikeReferenceMaterial(normalizedContent)) {
            return MaterialCategory.REFERENCE_MATERIAL;
        }
        if (looksLikeResearchResult(normalizedContent)) {
            return MaterialCategory.RESEARCH_RESULT;
        }

        try {
            return MaterialCategory.valueOf(category == null ? "UNKNOWN" : category.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return MaterialCategory.UNKNOWN;
        }
    }

    private boolean looksLikeAssignmentRequirement(String content) {
        return content.contains("课程论文要求")
                || content.contains("作业要求")
                || (content.contains("字数") && content.contains("格式"))
                || (content.contains("引用") && content.contains("参考资料"));
    }

    private boolean looksLikeReferenceMaterial(String content) {
        return content.contains("参考文献")
                || content.contains("文献摘录")
                || content.contains("已有研究")
                || content.contains("研究指出");
    }

    private boolean looksLikeResearchResult(String content) {
        return content.contains("研究结果")
                || content.contains("调研")
                || content.contains("数据显示")
                || content.contains("%")
                || content.contains("样本");
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    private String defaultJson(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    private String snippet(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> rawItems = objectMapper.readValue(json, List.class);
            List<String> items = new ArrayList<>();
            for (Object item : rawItems) {
                if (item != null && !String.valueOf(item).isBlank()) {
                    items.add(String.valueOf(item));
                }
            }
            return items;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private BibliographicMetadata readBibliographicMetadata(AiSemanticParseResultEntity parseResult) {
        if (parseResult == null
                || parseResult.getBibliographicMetadataJson() == null
                || parseResult.getBibliographicMetadataJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(parseResult.getBibliographicMetadataJson(), BibliographicMetadata.class);
        } catch (JsonProcessingException e) {
            return BibliographicMetadata.empty();
        }
    }

    private MaterialCategory effectiveCategory(AiSemanticParseResultEntity parseResult) {
        if (parseResult == null) {
            return null;
        }
        return parseResult.getManualMaterialCategory() != null
                ? parseResult.getManualMaterialCategory()
                : parseResult.getMaterialCategory();
    }

    private MaterialResponse toResponse(MaterialEntity entity, AiSemanticParseResultEntity parseResult) {
        MaterialCategory effectiveCategory = effectiveCategory(parseResult);
        List<String> claims = parseResult == null ? List.of() : readStringList(parseResult.getDetectedClaimsJson());
        List<String> evidence = parseResult == null ? List.of() : readStringList(parseResult.getDetectedEvidenceJson());
        List<String> requirements = parseResult == null ? List.of() : readStringList(parseResult.getDetectedRequirementsJson());
        BibliographicMetadata bibliographicMetadata = readBibliographicMetadata(parseResult);
        return new MaterialResponse(
                entity.getId(),
                entity.getFilename(),
                entity.getFileType(),
                entity.getSourceType(),
                entity.isKeyMaterial(),
                entity.getParseStage(),
                entity.getConfidenceScore(),
                entity.getCreatedAt(),
                parseResult == null ? null : parseResult.getMaterialCategory(),
                effectiveCategory,
                parseResult != null && parseResult.getManualMaterialCategory() != null,
                parseResult == null ? null : parseResult.getSummary(),
                parseResult == null ? null : parseResult.getTopicRelation(),
                claims,
                evidence,
                requirements,
                bibliographicMetadata,
                parseQualityService.evaluate(entity, parseResult, effectiveCategory, claims, evidence, requirements, bibliographicMetadata)
        );
    }
}
