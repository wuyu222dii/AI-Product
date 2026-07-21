package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.application.service.WorkspaceApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WorkspaceController.class)
@Import(GlobalExceptionHandler.class)
@AuthenticatedApiTest
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkspaceApplicationService workspaceApplicationService;

    @Test
    void createShouldReturnCreatedWorkspace() throws Exception {
        WorkspaceResponse response = new WorkspaceResponse(
                UUID.randomUUID(),
                "AI 课程论文项目",
                WorkspaceStatus.DRAFT,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(workspaceApplicationService.create(any(CreateWorkspaceRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/workspaces")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "AI 课程论文项目"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("AI 课程论文项目"))
                .andExpect(jsonPath("$.data.status").value("DRAFT"));
    }

    @Test
    void listShouldReturnWorkspaceItems() throws Exception {
        WorkspaceResponse response = new WorkspaceResponse(
                UUID.randomUUID(),
                "现有项目",
                WorkspaceStatus.READY,
                UUID.randomUUID(),
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
        when(workspaceApplicationService.list()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].title").value("现有项目"))
                .andExpect(jsonPath("$.data.items[0].status").value("READY"));
    }
}
