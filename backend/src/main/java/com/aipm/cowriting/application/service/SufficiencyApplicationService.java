package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.sufficiency.MaterialSufficiencyResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.MaterialSufficiencyResultEntity;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.MaterialSufficiencyResultRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SufficiencyApplicationService {

    private final WorkspaceRepository workspaceRepository;
    private final RequirementSnapshotRepository snapshotRepository;
    private final MaterialRepository materialRepository;
    private final MaterialSufficiencyResultRepository resultRepository;
    private final AiSemanticParseResultRepository parseResultRepository;
    private final ObjectMapper objectMapper;

    public SufficiencyApplicationService(
            WorkspaceRepository workspaceRepository,
            RequirementSnapshotRepository snapshotRepository,
            MaterialRepository materialRepository,
            MaterialSufficiencyResultRepository resultRepository,
            AiSemanticParseResultRepository parseResultRepository,
            ObjectMapper objectMapper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.snapshotRepository = snapshotRepository;
        this.materialRepository = materialRepository;
        this.resultRepository = resultRepository;
        this.parseResultRepository = parseResultRepository;
        this.objectMapper = objectMapper;
    }

    public MaterialSufficiencyResponse check(UUID workspaceId, UUID requirementSnapshotId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在");
        }
        var snapshot = snapshotRepository.findById(requirementSnapshotId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.REQUIREMENT_SNAPSHOT_MISSING,
                        HttpStatus.NOT_FOUND.value(),
                        "requirement snapshot 不存在"
                ));
        if (!workspaceId.equals(snapshot.getWorkspaceId())) {
            throw new BusinessException(
                    ErrorCode.REQUIREMENT_SNAPSHOT_MISSING,
                    HttpStatus.NOT_FOUND.value(),
                    "requirement snapshot 不存在"
            );
        }

        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        List<UUID> materialIds = materials.stream().map(MaterialEntity::getId).toList();
        List<AiSemanticParseResultEntity> parseResults = materialIds.isEmpty()
                ? List.of()
                : parseResultRepository.findByMaterialIdIn(materialIds);
        List<Map<String, Object>> missingItems = new ArrayList<>();
        List<Map<String, Object>> recommendedSupplements = new ArrayList<>();

        boolean hasKeyMaterialParsed = materials.stream()
                .anyMatch(material -> material.isKeyMaterial() && material.getParseStage() == ParseStage.AI_PARSED);
        boolean hasReferenceMaterial = parseResults.stream()
                .anyMatch(result -> effectiveCategory(result) == MaterialCategory.REFERENCE_MATERIAL);
        boolean hasRequirementMaterial = parseResults.stream()
                .anyMatch(result -> effectiveCategory(result) == MaterialCategory.ASSIGNMENT_REQUIREMENT);
        boolean hasResearchOrDraftMaterial = parseResults.stream()
                .anyMatch(result ->
                        effectiveCategory(result) == MaterialCategory.RESEARCH_RESULT
                                || effectiveCategory(result) == MaterialCategory.USER_DRAFT
                                || effectiveCategory(result) == MaterialCategory.CHART_OR_DATA
                );

        if (!hasKeyMaterialParsed) {
            missingItems.add(Map.of(
                    "type", "key_material",
                    "label", "核心材料尚未完成 AI 解析",
                    "message", "系统还没有找到任何一份“已标记为关键材料，并且已完成 AI 语义解析”的上传内容。",
                    "action", "请在上传或解析页面确认 1-2 份最能支撑当前学术文档的材料，例如写作要求、研究笔记、已有草稿、数据图表或核心文献，并等待 AI 解析完成。"
            ));
            recommendedSupplements.add(Map.of(
                    "type", "key_material",
                    "suggestedCount", "1-2",
                    "label", "补充或确认核心材料",
                    "message", "请至少准备 1-2 份可作为论文写作依据的核心材料，并确保它们被标记为关键材料且解析状态为 AI 已解析。"
            ));
        }

        if (!hasRequirementMaterial) {
            missingItems.add(Map.of(
                    "type", "assignment_requirement",
                    "label", "缺少明确的写作与提交要求",
                    "message", "系统还没有从已解析材料中识别到明确的题目、篇幅、格式、引用规范、提交或投稿要求。",
                    "action", "请上传学校规范、导师说明、课程要求、期刊指南等文件，或在文本框中粘贴用户确认的要求。"
            ));
            recommendedSupplements.add(Map.of(
                    "type", "assignment_requirement",
                    "suggestedCount", "1",
                    "label", "补充写作要求",
                    "message", "请补充 1 份明确的学校、导师、课程、期刊或用户确认要求。"
            ));
        }

        if (!hasReferenceMaterial) {
            missingItems.add(Map.of(
                    "type", "reference_material",
                    "label", "缺少可引用参考资料",
                    "message", "系统还没有从已解析材料中识别到可用于引用或支撑观点的文献、报告、网页资料或参考资料。",
                    "action", "建议上传 3-5 篇与主题相关的核心文献、研究报告、课程指定阅读材料或可靠网页资料。"
            ));
            recommendedSupplements.add(Map.of(
                    "type", "reference_material",
                    "suggestedCount", "3-5",
                    "label", "补充参考资料",
                    "message", "建议补充 3-5 篇可引用的核心参考资料。"
            ));
        }

        if (!hasResearchOrDraftMaterial) {
            missingItems.add(Map.of(
                    "type", "research_result",
                    "label", "缺少你的研究内容或写作基础",
                    "message", "系统还没有识别到可直接支撑正文生成的个人研究结果、分析记录、已有草稿、调研数据或图表。",
                    "action", "请上传你的研究笔记、访谈/问卷结果、数据表、图表、已有段落或初稿，让 AI 有足够依据生成正文。"
            ));
            recommendedSupplements.add(Map.of(
                    "type", "research_result",
                    "suggestedCount", "1-2",
                    "label", "补充研究成果",
                    "message", "请补充 1-2 份研究结果、草稿、图表数据或分析笔记。"
            ));
        }

        boolean eligible = missingItems.isEmpty();
        String reason = eligible ? "当前材料足以支撑生成" : "根据当前上传内容，暂时无法生成正文框架/初稿";

        MaterialSufficiencyResultEntity entity = new MaterialSufficiencyResultEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setGenerationEligible(eligible);
        entity.setMissingItemsJson(writeJson(missingItems));
        entity.setRecommendedSupplementsJson(writeJson(recommendedSupplements));
        entity.setReason(reason);
        entity.setCreatedAt(OffsetDateTime.now());
        resultRepository.save(entity);

        return new MaterialSufficiencyResponse(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.isGenerationEligible(),
                missingItems,
                recommendedSupplements,
                entity.getReason()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    private MaterialCategory effectiveCategory(AiSemanticParseResultEntity result) {
        return result.getManualMaterialCategory() != null
                ? result.getManualMaterialCategory()
                : result.getMaterialCategory();
    }
}
