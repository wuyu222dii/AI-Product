package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.application.dto.academic.AcademicProfileResponse;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private AcademicProfileApplicationService academicProfileService;
    @Mock
    private AcademicDocumentApplicationService academicDocumentService;

    @InjectMocks
    private WorkspaceApplicationService workspaceApplicationService;

    @Test
    void createShouldPersistDraftWorkspace() {
        when(workspaceRepository.save(any(WorkspaceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(academicProfileService.getOrCreateDefault(any())).thenReturn(profile());

        WorkspaceResponse response = workspaceApplicationService.create(new CreateWorkspaceRequest("  AI 课程论文项目  "));

        ArgumentCaptor<WorkspaceEntity> captor = ArgumentCaptor.forClass(WorkspaceEntity.class);
        verify(workspaceRepository).save(captor.capture());
        WorkspaceEntity saved = captor.getValue();

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getTitle()).isEqualTo("AI 课程论文项目");
        assertThat(saved.getStatus()).isEqualTo(WorkspaceStatus.DRAFT);
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();

        assertThat(response.title()).isEqualTo("AI 课程论文项目");
        assertThat(response.status()).isEqualTo(WorkspaceStatus.DRAFT);
    }

    @Test
    void listShouldMapRepositoryEntities() {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(UUID.randomUUID());
        entity.setTitle("现有项目");
        entity.setStatus(WorkspaceStatus.READY);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());

        when(workspaceRepository.findAll()).thenReturn(List.of(entity));
        AcademicProfileResponse profile = profile();
        when(academicProfileService.findExisting(List.of(entity.getId()))).thenReturn(Map.of(entity.getId(), profile));

        List<WorkspaceResponse> responses = workspaceApplicationService.list();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).title()).isEqualTo("现有项目");
        assertThat(responses.get(0).status()).isEqualTo(WorkspaceStatus.READY);
        verify(academicProfileService, never()).getOrCreateDefault(entity.getId());
        verify(academicDocumentService, never()).ensureLegacyPrimaryDocument(entity.getId());
    }

    private AcademicProfileResponse profile() {
        return new AcademicProfileResponse(
                UUID.randomUUID(), AcademicStage.UNDERGRADUATE, DisciplineGroup.INTERDISCIPLINARY,
                ResearchParadigm.OTHER, "zh-CN", "APA", null,
                AiUsagePolicy.EVIDENCE_GROUNDED_DRAFTING,
                java.util.Map.of("humanReviewRequired", true), OffsetDateTime.now(), OffsetDateTime.now()
        );
    }
}
