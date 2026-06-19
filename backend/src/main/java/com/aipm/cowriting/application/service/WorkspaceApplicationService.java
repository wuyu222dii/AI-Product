package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WorkspaceApplicationService {

    private static final UUID DEMO_USER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final WorkspaceRepository workspaceRepository;

    public WorkspaceApplicationService(WorkspaceRepository workspaceRepository) {
        this.workspaceRepository = workspaceRepository;
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
        return toResponse(entity);
    }

    public List<WorkspaceResponse> list() {
        return workspaceRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    public WorkspaceResponse get(UUID id) {
        WorkspaceEntity entity = workspaceRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        "workspace 不存在"
                ));
        return toResponse(entity);
    }

    private WorkspaceResponse toResponse(WorkspaceEntity entity) {
        return new WorkspaceResponse(
                entity.getId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getCurrentDraftVersionId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
