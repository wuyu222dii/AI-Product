package com.aipm.cowriting.application.service;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.EvidenceBindingEntity;
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
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ResourceOwnershipService {

    private final WorkspaceRepository workspaces;
    private final MaterialRepository materials;
    private final AcademicDocumentRepository documents;
    private final DocumentSectionRepository sections;
    private final DocumentSectionVersionRepository sectionVersions;
    private final DraftVersionRepository drafts;
    private final ReviewItemRepository reviews;
    private final AppealCaseRepository appeals;
    private final EvidenceBindingRepository evidenceBindings;
    private final CoWritePreviewRepository coWritePreviews;
    private final SectionCoWritePreviewRepository sectionCoWritePreviews;
    private final CurrentUserService currentUser;
    private final LegacyDemoAccessPolicy legacyPolicy;
    private final JobApplicationService jobs;

    public ResourceOwnershipService(
            WorkspaceRepository workspaces,
            MaterialRepository materials,
            AcademicDocumentRepository documents,
            DocumentSectionRepository sections,
            DocumentSectionVersionRepository sectionVersions,
            DraftVersionRepository drafts,
            ReviewItemRepository reviews,
            AppealCaseRepository appeals,
            EvidenceBindingRepository evidenceBindings,
            CoWritePreviewRepository coWritePreviews,
            SectionCoWritePreviewRepository sectionCoWritePreviews,
            CurrentUserService currentUser,
            LegacyDemoAccessPolicy legacyPolicy,
            JobApplicationService jobs
    ) {
        this.workspaces = workspaces;
        this.materials = materials;
        this.documents = documents;
        this.sections = sections;
        this.sectionVersions = sectionVersions;
        this.drafts = drafts;
        this.reviews = reviews;
        this.appeals = appeals;
        this.evidenceBindings = evidenceBindings;
        this.coWritePreviews = coWritePreviews;
        this.sectionCoWritePreviews = sectionCoWritePreviews;
        this.currentUser = currentUser;
        this.legacyPolicy = legacyPolicy;
        this.jobs = jobs;
    }

    public void requireWorkspace(UUID workspaceId) {
        UUID userId = currentUser.userId();
        boolean accessible = workspaces.findById(workspaceId)
                .map(workspace -> userId.equals(workspace.getUserId())
                        || (workspace.isLegacyUnowned() && legacyPolicy.allows(userId)))
                .orElse(false);
        if (!accessible) throw notFound();
    }

    public void requireMaterial(UUID id) {
        requireWorkspace(materials.findById(id).map(item -> item.getWorkspaceId()).orElseThrow(this::notFound));
    }

    public void requireDocument(UUID id) {
        requireWorkspace(documents.findById(id).map(item -> item.getWorkspaceId()).orElseThrow(this::notFound));
    }

    public void requireSection(UUID id) {
        requireDocument(sections.findById(id).map(item -> item.getDocumentId()).orElseThrow(this::notFound));
    }

    public void requireSectionVersion(UUID id) {
        requireSection(sectionVersions.findById(id).map(item -> item.getSectionId()).orElseThrow(this::notFound));
    }

    public void requireDraft(UUID id) {
        requireWorkspace(drafts.findById(id).map(item -> item.getWorkspaceId()).orElseThrow(this::notFound));
    }

    public void requireReview(UUID id) {
        requireWorkspace(reviews.findById(id).map(item -> item.getWorkspaceId()).orElseThrow(this::notFound));
    }

    public void requireAppeal(UUID id) {
        requireReview(appeals.findById(id).map(item -> item.getReviewItemId()).orElseThrow(this::notFound));
    }

    public void requireEvidenceBinding(UUID id) {
        EvidenceBindingEntity binding = evidenceBindings.findById(id).orElseThrow(this::notFound);
        if (binding.getSectionId() != null) requireSection(binding.getSectionId());
        else if (binding.getDocumentId() != null) requireDocument(binding.getDocumentId());
        else if (binding.getDraftVersionId() != null) requireDraft(binding.getDraftVersionId());
        else throw notFound();
    }

    public void requireCoWritePreview(UUID id) {
        requireWorkspace(coWritePreviews.findById(id).map(item -> item.getWorkspaceId()).orElseThrow(this::notFound));
    }

    public void requireSectionCoWritePreview(UUID id) {
        requireSection(sectionCoWritePreviews.findById(id).map(item -> item.getSectionId()).orElseThrow(this::notFound));
    }

    public void requireJob(UUID id) {
        jobs.requireCurrentUser(id);
    }

    private BusinessException notFound() {
        return new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "资源不存在");
    }
}
