package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.application.dto.academic.AcademicProfileResponse;
import com.aipm.cowriting.application.dto.academic.CreateAcademicDocumentRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceApplicationService {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final WorkspaceRepository workspaceRepository;
    private final AcademicProfileApplicationService academicProfileService;
    private final AcademicDocumentApplicationService academicDocumentService;

    public WorkspaceApplicationService(
            WorkspaceRepository workspaceRepository,
            AcademicProfileApplicationService academicProfileService,
            AcademicDocumentApplicationService academicDocumentService
    ) {
        this.workspaceRepository = workspaceRepository;
        this.academicProfileService = academicProfileService;
        this.academicDocumentService = academicDocumentService;
    }

    public WorkspaceResponse create(CreateWorkspaceRequest request) {
        WorkspaceEntity entity = new WorkspaceEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(DEMO_USER_ID);
        entity.setTitle(request.title().trim());
        entity.setStatus(WorkspaceStatus.DRAFT);
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(entity);

        AcademicProfileResponse profile = request.academicProfile() == null
                ? academicProfileService.getOrCreateDefault(entity.getId())
                : academicProfileService.upsert(entity.getId(), request.academicProfile());
        CreateAcademicDocumentRequest initialDocument = request.initialDocument() == null
                ? defaultInitialDocument(entity.getTitle(), profile.academicStage(), request.academicProfile() != null)
                : request.initialDocument();
        academicDocumentService.create(entity.getId(), initialDocument);
        return toResponse(workspaceRepository.findById(entity.getId()).orElse(entity));
    }

    public List<WorkspaceResponse> list() {
        List<WorkspaceEntity> workspaces = workspaceRepository.findAll();
        Map<UUID, AcademicProfileResponse> profiles = academicProfileService.findExisting(
                workspaces.stream().map(WorkspaceEntity::getId).toList()
        );
        return workspaces.stream()
                .map(entity -> toResponse(entity, resolveProfile(entity.getId(), profiles)))
                .toList();
    }

    public WorkspaceResponse get(UUID id) {
        WorkspaceEntity entity = workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        "workspace 不存在"
                ));
        academicDocumentService.ensureLegacyPrimaryDocument(id);
        return toResponse(workspaceRepository.findById(id).orElse(entity));
    }

    private WorkspaceResponse toResponse(WorkspaceEntity entity) {
        return toResponse(entity, academicProfileService.getOrCreateDefault(entity.getId()));
    }

    private WorkspaceResponse toResponse(WorkspaceEntity entity, AcademicProfileResponse profile) {
        return new WorkspaceResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getCurrentDraftVersionId(),
                entity.getActiveDocumentId(),
                profile,
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private AcademicProfileResponse resolveProfile(
            UUID workspaceId,
            Map<UUID, AcademicProfileResponse> profiles
    ) {
        AcademicProfileResponse profile = profiles.get(workspaceId);
        return profile == null ? academicProfileService.getOrCreateDefault(workspaceId) : profile;
    }

    private CreateAcademicDocumentRequest defaultInitialDocument(
            String title,
            AcademicStage academicStage,
            boolean explicitProfile
    ) {
        AcademicDocumentType type = !explicitProfile
                ? AcademicDocumentType.COURSE_PAPER
                : switch (academicStage) {
                    case UNDERGRADUATE -> AcademicDocumentType.UNDERGRADUATE_THESIS;
                    case MASTER -> AcademicDocumentType.MASTER_THESIS;
                    case DOCTORAL -> AcademicDocumentType.DOCTORAL_DISSERTATION;
                    case RESEARCHER -> AcademicDocumentType.RESEARCH_PROPOSAL;
                };
        int targetLength = switch (type) {
            case COURSE_PAPER -> 3000;
            case UNDERGRADUATE_THESIS -> 12000;
            case MASTER_THESIS -> 40000;
            case DOCTORAL_DISSERTATION -> 80000;
            case RESEARCH_PROPOSAL -> 6000;
            default -> 8000;
        };
        return new CreateAcademicDocumentRequest(
                title, type, null, null, targetLength, "WORDS", "APA", java.util.Map.of(), true
        );
    }
}
