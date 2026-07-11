package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.AcademicProfileRequest;
import com.aipm.cowriting.application.dto.academic.AcademicProfileResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import com.aipm.cowriting.domain.repository.AcademicProjectProfileRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class AcademicProfileApplicationService {

    private final AcademicProjectProfileRepository profileRepository;
    private final WorkspaceRepository workspaceRepository;

    public AcademicProfileApplicationService(
            AcademicProjectProfileRepository profileRepository,
            WorkspaceRepository workspaceRepository
    ) {
        this.profileRepository = profileRepository;
        this.workspaceRepository = workspaceRepository;
    }

    public AcademicProfileResponse getOrCreateDefault(UUID workspaceId) {
        assertWorkspace(workspaceId);
        return profileRepository.findById(workspaceId)
                .map(this::toResponse)
                .orElseGet(() -> toResponse(saveDefault(workspaceId)));
    }

    public Map<UUID, AcademicProfileResponse> findExisting(Collection<UUID> workspaceIds) {
        Map<UUID, AcademicProfileResponse> profiles = new LinkedHashMap<>();
        profileRepository.findAllById(workspaceIds).forEach(entity ->
                profiles.put(entity.getWorkspaceId(), toResponse(entity))
        );
        return profiles;
    }

    public AcademicProfileResponse upsert(UUID workspaceId, AcademicProfileRequest request) {
        assertWorkspace(workspaceId);
        AcademicProjectProfileEntity entity = profileRepository.findById(workspaceId)
                .orElseGet(AcademicProjectProfileEntity::new);
        OffsetDateTime now = OffsetDateTime.now();
        if (entity.getWorkspaceId() == null) {
            entity.setWorkspaceId(workspaceId);
            entity.setCreatedAt(now);
        }
        entity.setAcademicStage(request.academicStage());
        entity.setDisciplineGroup(request.disciplineGroup());
        entity.setResearchParadigm(request.researchParadigm());
        entity.setPrimaryLanguage(defaultString(request.primaryLanguage(), "zh-CN"));
        entity.setDefaultCitationStyle(defaultString(request.defaultCitationStyle(), "APA"));
        entity.setInstitution(trimToNull(request.institution()));
        entity.setAiUsagePolicy(request.aiUsagePolicy() == null
                ? AiUsagePolicy.EVIDENCE_GROUNDED_DRAFTING
                : request.aiUsagePolicy());
        entity.setAiPolicyJson(request.aiPolicy() == null
                ? Map.of("humanReviewRequired", true, "disclosureRequired", true)
                : request.aiPolicy());
        entity.setUpdatedAt(now);
        profileRepository.save(entity);
        return toResponse(entity);
    }

    AcademicProjectProfileEntity getEntity(UUID workspaceId) {
        getOrCreateDefault(workspaceId);
        return profileRepository.findById(workspaceId).orElseThrow();
    }

    private AcademicProjectProfileEntity saveDefault(UUID workspaceId) {
        AcademicProjectProfileEntity entity = new AcademicProjectProfileEntity();
        OffsetDateTime now = OffsetDateTime.now();
        entity.setWorkspaceId(workspaceId);
        entity.setAcademicStage(AcademicStage.UNDERGRADUATE);
        entity.setDisciplineGroup(DisciplineGroup.INTERDISCIPLINARY);
        entity.setResearchParadigm(ResearchParadigm.OTHER);
        entity.setPrimaryLanguage("zh-CN");
        entity.setDefaultCitationStyle("APA");
        entity.setAiUsagePolicy(AiUsagePolicy.EVIDENCE_GROUNDED_DRAFTING);
        entity.setAiPolicyJson(Map.of("humanReviewRequired", true, "disclosureRequired", true));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return profileRepository.save(entity);
    }

    private AcademicProfileResponse toResponse(AcademicProjectProfileEntity entity) {
        return new AcademicProfileResponse(
                entity.getWorkspaceId(),
                entity.getAcademicStage(),
                entity.getDisciplineGroup(),
                entity.getResearchParadigm(),
                entity.getPrimaryLanguage(),
                entity.getDefaultCitationStyle(),
                entity.getInstitution(),
                entity.getAiUsagePolicy(),
                entity.getAiPolicyJson() == null ? Map.of() : entity.getAiPolicyJson(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void assertWorkspace(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在");
        }
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
