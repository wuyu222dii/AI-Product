package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.ai.CoWriteResult;
import com.aipm.cowriting.application.dto.ai.DraftGenerationResult;
import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.application.dto.draft.UpdateDraftRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.common.error.BusinessException;
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
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DraftApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private RequirementSnapshotRepository snapshotRepository;
    @Mock
    private MaterialSufficiencyResultRepository sufficiencyResultRepository;
    @Mock
    private DraftVersionRepository draftVersionRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private AiSemanticParseResultRepository aiSemanticParseResultRepository;
    @Mock
    private OpenAiDraftGenerationService openAiDraftGenerationService;
    @Mock
    private OpenAiCoWriteService openAiCoWriteService;
    @Mock
    private ReviewApplicationService reviewApplicationService;
    @Mock
    private EvidenceBindingApplicationService evidenceBindingApplicationService;
    @Mock
    private JobApplicationService jobApplicationService;

    @InjectMocks
    private DraftApplicationService draftApplicationService;

    @BeforeEach
    void setUp() {
        draftApplicationService = new DraftApplicationService(
                workspaceRepository,
                snapshotRepository,
                sufficiencyResultRepository,
                draftVersionRepository,
                materialRepository,
                aiSemanticParseResultRepository,
                openAiDraftGenerationService,
                openAiCoWriteService,
                reviewApplicationService,
                evidenceBindingApplicationService,
                new ObjectMapper(),
                jobApplicationService
        );
    }

    @Test
    void updateDraftShouldPersistEditedText() {
        UUID draftId = UUID.randomUUID();
        DraftVersionEntity entity = new DraftVersionEntity();
        entity.setId(draftId);
        entity.setWorkspaceId(UUID.randomUUID());
        entity.setVersionNo(1);
        entity.setTitleSuggestion("旧标题");
        entity.setOutlineJson("{}");
        entity.setParagraphSkeletonsJson("[]");
        entity.setDraftText("旧正文");
        entity.setSourceTraceMapJson("{}");
        entity.setGenerationStatus(GenerationStatus.SUCCESS);
        entity.setCreatedBy("system-ai");
        entity.setCreatedAt(OffsetDateTime.now());

        when(draftVersionRepository.findById(draftId)).thenReturn(Optional.of(entity));
        when(draftVersionRepository.save(any(DraftVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DraftResponse response = draftApplicationService.updateDraft(
                draftId,
                new UpdateDraftRequest("新标题", "新正文")
        );

        assertThat(response.titleSuggestion()).isEqualTo("新标题");
        assertThat(response.draftText()).isEqualTo("新正文");
        verify(draftVersionRepository).save(entity);
    }

    @Test
    void generateShouldPersistAiGeneratedDraft() {
        UUID workspaceId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(workspaceId);
        workspace.setStatus(WorkspaceStatus.PROCESSING);

        RequirementSnapshotEntity snapshot = new RequirementSnapshotEntity();
        snapshot.setId(snapshotId);
        snapshot.setWorkspaceId(workspaceId);
        snapshot.setTopic("人工智能对大学生学习方式的影响");
        snapshot.setWordCount(3000);
        snapshot.setCitationStyle("APA");
        snapshot.setSpecialRequirementsJson("{\"minReferences\":5}");

        MaterialSufficiencyResultEntity sufficiency = new MaterialSufficiencyResultEntity();
        sufficiency.setId(UUID.randomUUID());
        sufficiency.setWorkspaceId(workspaceId);
        sufficiency.setGenerationEligible(true);

        MaterialEntity material = new MaterialEntity();
        material.setId(UUID.randomUUID());
        material.setWorkspaceId(workspaceId);

        AiSemanticParseResultEntity parseResult = new AiSemanticParseResultEntity();
        parseResult.setId(UUID.randomUUID());
        parseResult.setMaterialId(material.getId());
        parseResult.setMaterialCategory(MaterialCategory.RESEARCH_RESULT);
        parseResult.setSummary("研究结果摘要");
        parseResult.setTopicRelation("与主题高度相关");
        parseResult.setDetectedClaimsJson("[\"claim\"]");
        parseResult.setDetectedEvidenceJson("[\"evidence\"]");
        parseResult.setDetectedRequirementsJson("[]");

        DraftGenerationResult generationResult = new DraftGenerationResult(
                "真实生成标题",
                Map.of("sections", List.of(Map.of("title", "引言", "purpose", "说明背景"))),
                List.of(Map.of("paragraphId", "p1", "goal", "说明问题")),
                "真实生成正文",
                Map.of("p1", List.of(material.getId().toString()))
        );

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(snapshotRepository.findById(snapshotId)).thenReturn(Optional.of(snapshot));
        when(sufficiencyResultRepository.findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(Optional.of(sufficiency));
        when(draftVersionRepository.findFirstByWorkspaceIdOrderByVersionNoDesc(workspaceId)).thenReturn(Optional.empty());
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(material));
        when(aiSemanticParseResultRepository.findByMaterialIdIn(any())).thenReturn(List.of(parseResult));
        when(openAiDraftGenerationService.generate(any(), any(), any())).thenReturn(generationResult);
        when(draftVersionRepository.save(any(DraftVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobApplicationService.createJob(any(), any(), any())).thenReturn(UUID.randomUUID());

        JobResponse response = draftApplicationService.generate(workspaceId, snapshotId, "default");

        assertThat(response.status()).isEqualTo("success");
        verify(reviewApplicationService).refreshForDraft(any(DraftVersionEntity.class));
        verify(evidenceBindingApplicationService).rebuild(any());
        verify(workspaceRepository).save(any(WorkspaceEntity.class));
    }

    @Test
    void coWriteShouldCreateNewVersion() {
        UUID workspaceId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(workspaceId);
        workspace.setStatus(WorkspaceStatus.READY);

        DraftVersionEntity current = new DraftVersionEntity();
        current.setId(draftId);
        current.setWorkspaceId(workspaceId);
        current.setVersionNo(1);
        current.setTitleSuggestion("原标题");
        current.setOutlineJson("{}");
        current.setParagraphSkeletonsJson("[]");
        current.setDraftText("当前正文");
        current.setSourceTraceMapJson("{}");
        current.setGenerationStatus(GenerationStatus.SUCCESS);
        current.setCreatedBy("system-ai");
        current.setCreatedAt(OffsetDateTime.now());

        CoWriteResult result = new CoWriteResult("新标题", "AI 重写后的正文", Map.of("p1", List.of("m1")));

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(draftVersionRepository.findById(draftId)).thenReturn(Optional.of(current));
        when(draftVersionRepository.findFirstByWorkspaceIdOrderByVersionNoDesc(workspaceId)).thenReturn(Optional.of(current));
        when(openAiCoWriteService.coWrite(any(), any(), any(), any(), any(), any(), any(), any())).thenReturn(result);
        when(draftVersionRepository.save(any(DraftVersionEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(jobApplicationService.createJob(any(), any(), any())).thenReturn(UUID.randomUUID());

        JobResponse response = draftApplicationService.coWrite(
                workspaceId,
                draftId,
                "improve_expression",
                Map.of("start", 0, "end", 4, "selectedText", "当前正文"),
                "更学术",
                Map.of("keepData", true)
        );

        assertThat(response.status()).isEqualTo("success");
        verify(reviewApplicationService).refreshForDraft(any(DraftVersionEntity.class));
        verify(evidenceBindingApplicationService).rebuild(any());
        verify(workspaceRepository).save(any(WorkspaceEntity.class));
    }

    @Test
    void restoreShouldSetWorkspaceCurrentDraft() {
        UUID workspaceId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(workspaceId);
        workspace.setStatus(WorkspaceStatus.READY);
        workspace.setCurrentDraftVersionId(UUID.randomUUID());

        DraftVersionEntity entity = new DraftVersionEntity();
        entity.setId(draftId);
        entity.setWorkspaceId(workspaceId);
        entity.setVersionNo(1);
        entity.setTitleSuggestion("恢复标题");
        entity.setOutlineJson("{}");
        entity.setParagraphSkeletonsJson("[]");
        entity.setDraftText("恢复正文");
        entity.setSourceTraceMapJson("{}");
        entity.setGenerationStatus(GenerationStatus.SUCCESS);
        entity.setCreatedBy("system-ai");
        entity.setCreatedAt(OffsetDateTime.now());

        when(draftVersionRepository.findById(draftId)).thenReturn(Optional.of(entity));
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(WorkspaceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        DraftResponse response = draftApplicationService.restore(draftId);

        assertThat(response.id()).isEqualTo(draftId);
        assertThat(response.draftText()).isEqualTo("恢复正文");
        assertThat(workspace.getCurrentDraftVersionId()).isEqualTo(draftId);
        verify(workspaceRepository).save(workspace);
    }

    @Test
    void updateDraftShouldThrowWhenDraftMissing() {
        UUID draftId = UUID.randomUUID();
        when(draftVersionRepository.findById(draftId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> draftApplicationService.updateDraft(draftId, new UpdateDraftRequest("标题", "正文")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("draft 不存在");
    }
}
