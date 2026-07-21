package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.common.error.BusinessException;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class JobApplicationServiceTest {

    @Mock CurrentUserService currentUser;

    @Test
    void anotherUserCannotReadJob() {
        UUID ownerId = UUID.randomUUID();
        UUID otherUserId = UUID.randomUUID();
        when(currentUser.userId()).thenReturn(ownerId, otherUserId);
        JobApplicationService service = new JobApplicationService(currentUser);
        UUID jobId = service.createJob("export_docx", "success", UUID.randomUUID());

        assertThatThrownBy(() -> service.getJobForCurrentUser(jobId))
                .isInstanceOf(BusinessException.class)
                .extracting(error -> ((BusinessException) error).getHttpStatus())
                .isEqualTo(404);
    }
}
