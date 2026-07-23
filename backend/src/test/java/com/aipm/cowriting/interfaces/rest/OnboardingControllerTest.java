package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.guide.OnboardingCompleteResponse;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.application.service.OnboardingApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = OnboardingController.class)
@Import(GlobalExceptionHandler.class)
@AuthenticatedApiTest
class OnboardingControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private OnboardingApplicationService service;

    @Test
    void completeShouldCreateFirstProject() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceResponse workspace = new WorkspaceResponse(
                workspaceId, "我的研究", WorkspaceStatus.DRAFT, null,
                OffsetDateTime.now(), OffsetDateTime.now()
        );
        when(service.complete(any())).thenReturn(new OnboardingCompleteResponse(null, workspace, null));

        mockMvc.perform(post("/api/v1/onboarding/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "onboardingVersion": "v1",
                                  "workspace": {
                                    "title": "我的研究",
                                    "guideProfile": {
                                      "currentProgress": "IDEA_ONLY",
                                      "availableMaterials": [],
                                      "preferredMode": "GUIDED"
                                    }
                                  }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.workspace.id").value(workspaceId.toString()));
    }
}
