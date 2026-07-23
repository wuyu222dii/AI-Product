package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.guide.ProjectGuideResponse;
import com.aipm.cowriting.application.dto.guide.ProjectGuideTaskResponse;
import com.aipm.cowriting.application.service.ProjectGuideApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.model.GuideMode;
import com.aipm.cowriting.domain.model.GuideProgress;
import com.aipm.cowriting.domain.model.GuideTaskStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ProjectGuideController.class)
@Import(GlobalExceptionHandler.class)
@AuthenticatedApiTest
class ProjectGuideControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProjectGuideApplicationService service;

    @Test
    void getShouldReturnDynamicTasks() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(service.get(workspaceId)).thenReturn(response(workspaceId));

        mockMvc.perform(get("/api/v1/workspaces/{id}/guide", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.currentTaskId").value("materials"))
                .andExpect(jsonPath("$.data.tasks[0].status").value("CURRENT"));
    }

    @Test
    void patchShouldUpdateGuidePreferences() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(service.update(any(), any())).thenReturn(response(workspaceId));

        mockMvc.perform(patch("/api/v1/workspaces/{id}/guide", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "currentProgress": "TOPIC_DEFINED",
                                  "availableMaterials": ["REFERENCE_MATERIAL"],
                                  "targetDeadline": "2026-12-01",
                                  "preferredMode": "GUIDED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.preferredMode").value("GUIDED"));
    }

    @Test
    void inaccessibleGuideShouldBeIndistinguishableFromMissingResource() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(service.get(workspaceId)).thenThrow(new BusinessException(
                ErrorCode.RESOURCE_NOT_FOUND,
                HttpStatus.NOT_FOUND.value(),
                "资源不存在"
        ));

        mockMvc.perform(get("/api/v1/workspaces/{id}/guide", workspaceId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    private ProjectGuideResponse response(UUID workspaceId) {
        return new ProjectGuideResponse(
                workspaceId, "v1", GuideProgress.IDEA_ONLY, List.of(), null, GuideMode.GUIDED,
                17, "materials", List.of(new ProjectGuideTaskResponse(
                        "materials", "研究准备", "添加研究材料", "上传真实材料", "写作需要输入",
                        "形成材料资产", GuideTaskStatus.CURRENT, "/app/projects/" + workspaceId + "/upload",
                        false, "0 份材料"
                )), OffsetDateTime.now()
        );
    }
}
