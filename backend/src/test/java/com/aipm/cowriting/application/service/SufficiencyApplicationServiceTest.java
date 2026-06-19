package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.sufficiency.MaterialSufficiencyResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.MaterialSufficiencyResultEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.MaterialSufficiencyResultRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SufficiencyApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private RequirementSnapshotRepository snapshotRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private MaterialSufficiencyResultRepository resultRepository;
    @Mock
    private AiSemanticParseResultRepository parseResultRepository;

    private SufficiencyApplicationService sufficiencyApplicationService;

    @BeforeEach
    void setUp() {
        sufficiencyApplicationService = new SufficiencyApplicationService(
                workspaceRepository,
                snapshotRepository,
                materialRepository,
                resultRepository,
                parseResultRepository,
                new ObjectMapper()
        );
    }

    @Test
    void checkShouldReturnEligibleWhenAllMaterialCategoriesExist() {
        UUID workspaceId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(new RequirementSnapshotEntity()));

        MaterialEntity keyMaterial = new MaterialEntity();
        keyMaterial.setId(UUID.randomUUID());
        keyMaterial.setWorkspaceId(workspaceId);
        keyMaterial.setKeyMaterial(true);
        keyMaterial.setParseStage(ParseStage.AI_PARSED);

        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(keyMaterial));

        AiSemanticParseResultEntity requirement = parseResult(keyMaterial.getId(), MaterialCategory.ASSIGNMENT_REQUIREMENT);
        AiSemanticParseResultEntity reference = parseResult(UUID.randomUUID(), MaterialCategory.REFERENCE_MATERIAL);
        AiSemanticParseResultEntity research = parseResult(UUID.randomUUID(), MaterialCategory.RESEARCH_RESULT);
        when(parseResultRepository.findByMaterialIdIn(any())).thenReturn(List.of(requirement, reference, research));

        when(resultRepository.save(any(MaterialSufficiencyResultEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialSufficiencyResponse response = sufficiencyApplicationService.check(workspaceId, snapshotId);

        assertThat(response.isGenerationEligible()).isTrue();
        assertThat(response.missingItems()).isEmpty();
        assertThat(response.recommendedSupplements()).isEmpty();
    }

    @Test
    void checkShouldReportMissingCategoriesWhenMaterialsInsufficient() {
        UUID workspaceId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(new RequirementSnapshotEntity()));
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of());
        when(resultRepository.save(any(MaterialSufficiencyResultEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MaterialSufficiencyResponse response = sufficiencyApplicationService.check(workspaceId, snapshotId);

        assertThat(response.isGenerationEligible()).isFalse();
        assertThat(response.missingItems()).extracting(item -> item.get("type"))
                .contains("key_material", "assignment_requirement", "reference_material", "research_result");
        assertThat(response.missingItems()).extracting(item -> item.get("label"))
                .contains("核心材料尚未完成 AI 解析", "缺少老师要求或作业说明", "缺少可引用参考资料", "缺少你的研究内容或写作基础");
        assertThat(response.missingItems()).allSatisfy(item -> {
            assertThat(item.get("message")).isInstanceOf(String.class);
            assertThat((String) item.get("message")).doesNotContain("key_material", "assignment_requirement", "reference_material", "research_result");
        });
    }

    @Test
    void checkShouldThrowWhenSnapshotMissing() {
        UUID workspaceId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sufficiencyApplicationService.check(workspaceId, snapshotId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requirement snapshot 不存在");
    }

    private AiSemanticParseResultEntity parseResult(UUID materialId, MaterialCategory category) {
        AiSemanticParseResultEntity entity = new AiSemanticParseResultEntity();
        entity.setId(UUID.randomUUID());
        entity.setMaterialId(materialId);
        entity.setMaterialCategory(category);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setDetectedClaimsJson("[]");
        entity.setDetectedEvidenceJson("[]");
        entity.setDetectedRequirementsJson("[]");
        return entity;
    }
}
