package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.application.dto.draft.UpdateDraftRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.DraftApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.domain.model.GenerationStatus;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = DraftController.class)
@Import(GlobalExceptionHandler.class)
class DraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DraftApplicationService draftApplicationService;

    @Test
    void generateShouldReturnAcceptedJob() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(draftApplicationService.generate(eq(workspaceId), any(UUID.class), eq("default")))
                .thenReturn(new JobResponse(UUID.randomUUID().toString(), "success"));

        mockMvc.perform(post("/api/v1/workspaces/{id}/generate-draft", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "requirementSnapshotId": "%s",
                                  "mode": "default"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    void updateShouldPersistDraftText() throws Exception {
        UUID draftId = UUID.randomUUID();
        DraftResponse response = new DraftResponse(
                draftId,
                UUID.randomUUID(),
                2,
                "新标题",
                Map.of(),
                List.of(),
                "新的正文内容",
                Map.of(),
                GenerationStatus.SUCCESS,
                "system-ai",
                OffsetDateTime.now()
        );
        when(draftApplicationService.updateDraft(eq(draftId), any(UpdateDraftRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/api/v1/drafts/{id}", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titleSuggestion": "新标题",
                                  "draftText": "新的正文内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.titleSuggestion").value("新标题"))
                .andExpect(jsonPath("$.data.draftText").value("新的正文内容"));
    }

    @Test
    void restoreShouldReturnRestoredDraft() throws Exception {
        UUID draftId = UUID.randomUUID();
        DraftResponse response = new DraftResponse(
                draftId,
                UUID.randomUUID(),
                1,
                "旧标题",
                Map.of(),
                List.of(),
                "旧版本正文",
                Map.of(),
                GenerationStatus.SUCCESS,
                "system-ai",
                OffsetDateTime.now()
        );
        when(draftApplicationService.restore(draftId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/drafts/{id}/restore", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(draftId.toString()))
                .andExpect(jsonPath("$.data.draftText").value("旧版本正文"));
    }

    @Test
    void listShouldReturnDrafts() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        DraftResponse response = new DraftResponse(
                UUID.randomUUID(),
                workspaceId,
                1,
                "标题",
                Map.of(),
                List.of(),
                "正文",
                Map.of(),
                GenerationStatus.SUCCESS,
                "system-ai",
                OffsetDateTime.now()
        );
        when(draftApplicationService.listByWorkspace(workspaceId)).thenReturn(List.of(response));

        mockMvc.perform(get("/api/v1/workspaces/{id}/drafts", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].versionNo").value(1))
                .andExpect(jsonPath("$.data.items[0].titleSuggestion").value("标题"));
    }
}
