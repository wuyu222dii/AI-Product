package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.guide.ProjectGuideResponse;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.ProjectGuideEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.GuideMode;
import com.aipm.cowriting.domain.model.GuideProgress;
import com.aipm.cowriting.domain.model.GuideTaskStatus;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.MaterialSufficiencyResultRepository;
import com.aipm.cowriting.domain.repository.ProjectGuideRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProjectGuideApplicationServiceTest {

    @Mock private ProjectGuideRepository guideRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private AcademicDocumentRepository documentRepository;
    @Mock private DocumentSectionRepository sectionRepository;
    @Mock private KnowledgeChunkRepository knowledgeRepository;
    @Mock private MaterialSufficiencyResultRepository sufficiencyRepository;
    @Mock private ReviewItemRepository reviewRepository;
    @Mock private ResourceOwnershipService ownership;

    @InjectMocks private ProjectGuideApplicationService service;

    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        workspaceId = UUID.randomUUID();
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(workspaceId);
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId)).thenReturn(List.of());
        when(sufficiencyRepository.findFirstByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(Optional.empty());
    }

    @Test
    void noMaterialsShouldMakeUploadTheCurrentTask() {
        when(guideRepository.findById(workspaceId)).thenReturn(Optional.of(guide()));
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of());

        ProjectGuideResponse response = service.get(workspaceId);

        assertThat(response.currentTaskId()).isEqualTo("materials");
        assertThat(taskStatus(response, "materials")).isEqualTo(GuideTaskStatus.CURRENT);
        assertThat(taskStatus(response, "knowledge")).isEqualTo(GuideTaskStatus.UPCOMING);
        verify(ownership).requireWorkspace(workspaceId);
    }

    @Test
    void failedKeyMaterialShouldReceiveAttentionBeforeOtherTasks() {
        when(guideRepository.findById(workspaceId)).thenReturn(Optional.of(guide()));
        MaterialEntity material = new MaterialEntity();
        material.setKeyMaterial(true);
        material.setParseStage(ParseStage.AI_FAILED);
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(material));

        ProjectGuideResponse response = service.get(workspaceId);

        assertThat(response.currentTaskId()).isEqualTo("parsing");
        assertThat(taskStatus(response, "parsing")).isEqualTo(GuideTaskStatus.NEEDS_ATTENTION);
        assertThat(response.tasks().stream().filter(task -> task.id().equals("parsing")).findFirst().orElseThrow().blocking())
                .isTrue();
    }

    @Test
    void reportedWritingWithoutUploadedMaterialsShouldSurfaceTheMismatch() {
        ProjectGuideEntity guide = guide();
        guide.setCurrentProgress(GuideProgress.WRITING);
        when(guideRepository.findById(workspaceId)).thenReturn(Optional.of(guide));
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of());

        ProjectGuideResponse response = service.get(workspaceId);

        assertThat(response.currentTaskId()).isEqualTo("materials");
        assertThat(taskStatus(response, "materials")).isEqualTo(GuideTaskStatus.NEEDS_ATTENTION);
    }

    private GuideTaskStatus taskStatus(ProjectGuideResponse response, String id) {
        return response.tasks().stream()
                .filter(task -> task.id().equals(id))
                .findFirst()
                .orElseThrow()
                .status();
    }

    private ProjectGuideEntity guide() {
        ProjectGuideEntity guide = new ProjectGuideEntity();
        guide.setWorkspaceId(workspaceId);
        guide.setGuideVersion("v1");
        guide.setCurrentProgress(GuideProgress.IDEA_ONLY);
        guide.setAvailableMaterials(List.of());
        guide.setPreferredMode(GuideMode.GUIDED);
        guide.setCreatedAt(OffsetDateTime.now());
        guide.setUpdatedAt(OffsetDateTime.now());
        return guide;
    }
}
