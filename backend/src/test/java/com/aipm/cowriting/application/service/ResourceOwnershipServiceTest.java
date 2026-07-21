package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.EvidenceBindingEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.AppealCaseRepository;
import com.aipm.cowriting.domain.repository.CoWritePreviewRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionVersionRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.EvidenceBindingRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.SectionCoWritePreviewRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ResourceOwnershipServiceTest {

    @Mock WorkspaceRepository workspaces;
    @Mock MaterialRepository materials;
    @Mock AcademicDocumentRepository documents;
    @Mock DocumentSectionRepository sections;
    @Mock DocumentSectionVersionRepository sectionVersions;
    @Mock DraftVersionRepository drafts;
    @Mock ReviewItemRepository reviews;
    @Mock AppealCaseRepository appeals;
    @Mock EvidenceBindingRepository evidenceBindings;
    @Mock CoWritePreviewRepository coWritePreviews;
    @Mock SectionCoWritePreviewRepository sectionCoWritePreviews;
    @Mock CurrentUserService currentUser;
    @Mock LegacyDemoAccessPolicy legacyPolicy;
    @Mock JobApplicationService jobs;
    @InjectMocks ResourceOwnershipService ownership;

    @Test
    void ownerShouldAccessWorkspace() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(currentUser.userId()).thenReturn(userId);
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace(workspaceId, userId, false)));
        ownership.requireWorkspace(workspaceId);
    }

    @Test
    void foreignWorkspaceShouldLookMissing() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        when(currentUser.userId()).thenReturn(userId);
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace(workspaceId, UUID.randomUUID(), false)));
        assertThatThrownBy(() -> ownership.requireWorkspace(workspaceId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getHttpStatus())
                .isEqualTo(404);
    }

    @Test
    void foreignMaterialShouldLookMissing() {
        UUID userId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        MaterialEntity material = new MaterialEntity();
        material.setId(materialId);
        material.setWorkspaceId(workspaceId);
        when(materials.findById(materialId)).thenReturn(Optional.of(material));
        when(currentUser.userId()).thenReturn(userId);
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(workspace(workspaceId, UUID.randomUUID(), false)));
        assertThatThrownBy(() -> ownership.requireMaterial(materialId))
                .isInstanceOf(BusinessException.class)
                .hasMessage("资源不存在");
    }

    @Test
    void jobOwnershipShouldBeDelegatedToJobStore() {
        UUID jobId = UUID.randomUUID();
        ownership.requireJob(jobId);
        verify(jobs).requireCurrentUser(jobId);
    }

    @Test
    void foreignDocumentShouldLookMissing() {
        UUID documentId = UUID.randomUUID();
        UUID workspaceId = foreignWorkspace();
        AcademicDocumentEntity document = new AcademicDocumentEntity();
        document.setId(documentId);
        document.setWorkspaceId(workspaceId);
        when(documents.findById(documentId)).thenReturn(Optional.of(document));

        assertNotFound(() -> ownership.requireDocument(documentId));
    }

    @Test
    void foreignSectionShouldLookMissing() {
        UUID sectionId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID workspaceId = foreignWorkspace();
        DocumentSectionEntity section = new DocumentSectionEntity();
        section.setId(sectionId);
        section.setDocumentId(documentId);
        AcademicDocumentEntity document = new AcademicDocumentEntity();
        document.setId(documentId);
        document.setWorkspaceId(workspaceId);
        when(sections.findById(sectionId)).thenReturn(Optional.of(section));
        when(documents.findById(documentId)).thenReturn(Optional.of(document));

        assertNotFound(() -> ownership.requireSection(sectionId));
    }

    @Test
    void foreignDraftShouldLookMissing() {
        UUID draftId = UUID.randomUUID();
        UUID workspaceId = foreignWorkspace();
        DraftVersionEntity draft = new DraftVersionEntity();
        draft.setId(draftId);
        draft.setWorkspaceId(workspaceId);
        when(drafts.findById(draftId)).thenReturn(Optional.of(draft));

        assertNotFound(() -> ownership.requireDraft(draftId));
    }

    @Test
    void foreignReviewShouldLookMissing() {
        UUID reviewId = UUID.randomUUID();
        UUID workspaceId = foreignWorkspace();
        ReviewItemEntity review = new ReviewItemEntity();
        review.setId(reviewId);
        review.setWorkspaceId(workspaceId);
        when(reviews.findById(reviewId)).thenReturn(Optional.of(review));

        assertNotFound(() -> ownership.requireReview(reviewId));
    }

    @Test
    void documentScopedEvidenceShouldFollowDocumentOwnership() {
        UUID bindingId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID workspaceId = foreignWorkspace();
        EvidenceBindingEntity binding = new EvidenceBindingEntity();
        binding.setId(bindingId);
        binding.setDocumentId(documentId);
        AcademicDocumentEntity document = new AcademicDocumentEntity();
        document.setId(documentId);
        document.setWorkspaceId(workspaceId);
        when(evidenceBindings.findById(bindingId)).thenReturn(Optional.of(binding));
        when(documents.findById(documentId)).thenReturn(Optional.of(document));

        assertNotFound(() -> ownership.requireEvidenceBinding(bindingId));
    }

    private UUID foreignWorkspace() {
        UUID workspaceId = UUID.randomUUID();
        when(currentUser.userId()).thenReturn(UUID.randomUUID());
        when(workspaces.findById(workspaceId)).thenReturn(Optional.of(
                workspace(workspaceId, UUID.randomUUID(), false)
        ));
        return workspaceId;
    }

    private void assertNotFound(org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getHttpStatus())
                .isEqualTo(404);
    }

    private WorkspaceEntity workspace(UUID id, UUID userId, boolean legacy) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(id);
        entity.setUserId(userId);
        entity.setLegacyUnowned(legacy);
        return entity;
    }
}
