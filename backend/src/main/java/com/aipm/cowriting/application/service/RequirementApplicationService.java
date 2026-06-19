package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.requirement.CreateRequirementSnapshotRequest;
import com.aipm.cowriting.application.dto.requirement.RequirementSnapshotResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class RequirementApplicationService {

    private final RequirementSnapshotRepository snapshotRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public RequirementApplicationService(
            RequirementSnapshotRepository snapshotRepository,
            WorkspaceRepository workspaceRepository,
            ObjectMapper objectMapper
    ) {
        this.snapshotRepository = snapshotRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    public RequirementSnapshotResponse create(UUID workspaceId, CreateRequirementSnapshotRequest request) {
        assertWorkspaceExists(workspaceId);
        int nextVersion = snapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId)
                .map(snapshot -> snapshot.getVersion() + 1)
                .orElse(1);

        RequirementSnapshotEntity entity = new RequirementSnapshotEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setTopic(request.topic());
        entity.setWordCount(request.wordCount());
        entity.setDeadline(request.deadline());
        entity.setCitationStyle(request.citationStyle());
        entity.setSpecialRequirementsJson(writeJson(request.specialRequirements() == null ? Map.of() : request.specialRequirements()));
        entity.setVersion(nextVersion);
        entity.setCreatedAt(OffsetDateTime.now());
        snapshotRepository.save(entity);

        return toResponse(entity);
    }

    public RequirementSnapshotResponse latest(UUID workspaceId) {
        assertWorkspaceExists(workspaceId);
        RequirementSnapshotEntity entity = snapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.REQUIREMENT_SNAPSHOT_MISSING,
                        HttpStatus.NOT_FOUND.value(),
                        "requirement snapshot 不存在"
                ));
        return toResponse(entity);
    }

    private void assertWorkspaceExists(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value(),
                    "workspace 不存在"
            );
        }
    }

    private RequirementSnapshotResponse toResponse(RequirementSnapshotEntity entity) {
        return new RequirementSnapshotResponse(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getTopic(),
                entity.getWordCount(),
                entity.getDeadline(),
                entity.getCitationStyle(),
                readJson(entity.getSpecialRequirementsJson()),
                entity.getVersion(),
                entity.getCreatedAt()
        );
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readJson(String json) {
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 反序列化失败");
        }
    }
}
