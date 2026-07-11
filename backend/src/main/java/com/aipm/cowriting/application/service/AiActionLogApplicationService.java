package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.AiActionLogResponse;
import com.aipm.cowriting.config.OpenAiProperties;
import com.aipm.cowriting.domain.entity.AiActionLogEntity;
import com.aipm.cowriting.domain.repository.AiActionLogRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AiActionLogApplicationService {

    private final AiActionLogRepository repository;
    private final OpenAiProperties openAiProperties;

    public AiActionLogApplicationService(
            AiActionLogRepository repository,
            OpenAiProperties openAiProperties
    ) {
        this.repository = repository;
        this.openAiProperties = openAiProperties;
    }

    public AiActionLogEntity create(
            UUID workspaceId,
            UUID documentId,
            UUID sectionId,
            String actionType,
            List<UUID> evidenceMaterialIds,
            String requestSummary,
            String outputSummary,
            boolean disclosureRequired
    ) {
        AiActionLogEntity entity = new AiActionLogEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setDocumentId(documentId);
        entity.setSectionId(sectionId);
        entity.setActionType(actionType);
        entity.setModelName(openAiProperties.getModel());
        entity.setEvidenceMaterialIdsJson(evidenceMaterialIds == null ? List.of() : List.copyOf(evidenceMaterialIds));
        entity.setRequestSummary(requestSummary);
        entity.setOutputSummary(outputSummary);
        entity.setDisclosureRequired(disclosureRequired);
        entity.setCreatedAt(OffsetDateTime.now());
        return repository.save(entity);
    }

    public void markAccepted(UUID logId, boolean accepted) {
        if (logId == null) return;
        repository.findById(logId).ifPresent(entity -> {
            entity.setAccepted(accepted);
            repository.save(entity);
        });
    }

    public List<AiActionLogResponse> listByDocument(UUID documentId) {
        return repository.findByDocumentIdOrderByCreatedAtDesc(documentId).stream()
                .map(this::toResponse)
                .toList();
    }

    private AiActionLogResponse toResponse(AiActionLogEntity entity) {
        return new AiActionLogResponse(
                entity.getId(), entity.getWorkspaceId(), entity.getDocumentId(), entity.getSectionId(),
                entity.getActionType(), entity.getModelName(), entity.getEvidenceMaterialIdsJson() == null ? List.of() : entity.getEvidenceMaterialIdsJson(),
                entity.getRequestSummary(), entity.getOutputSummary(), entity.getAccepted(),
                entity.isDisclosureRequired(), entity.getCreatedAt()
        );
    }

}
