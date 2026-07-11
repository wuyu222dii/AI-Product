package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RequirementApplicationServiceTest {

    @Mock private RequirementSnapshotRepository snapshotRepository;
    @Mock private WorkspaceRepository workspaceRepository;

    private RequirementApplicationService service;
    private UUID workspaceId;

    @BeforeEach
    void setUp() {
        service = new RequirementApplicationService(snapshotRepository, workspaceRepository, new ObjectMapper());
        workspaceId = UUID.randomUUID();
        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(snapshotRepository.findFirstByWorkspaceIdAndDocumentIdIsNullOrderByVersionDesc(workspaceId))
                .thenReturn(Optional.empty());
        when(snapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId))
                .thenReturn(Optional.empty());
    }

    @Test
    void optionalReadShouldReturnNullWhenSnapshotDoesNotExist() {
        assertThat(service.latestOptional(workspaceId)).isNull();
    }

    @Test
    void requiredReadShouldKeepMissingSnapshotError() {
        assertThatThrownBy(() -> service.latest(workspaceId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("requirement snapshot 不存在");
    }
}
