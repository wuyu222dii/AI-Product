package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.cowrite.CoWritePreviewResponse;
import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.CoWritePreviewApplicationService;
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

@WebMvcTest(controllers = CoWriteController.class)
@Import(GlobalExceptionHandler.class)
class CoWriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DraftApplicationService draftApplicationService;

    @MockBean
    private CoWritePreviewApplicationService coWritePreviewApplicationService;

    @Test
    void previewShouldReturnCandidateWithoutApplyingDraft() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        UUID previewId = UUID.randomUUID();
        when(coWritePreviewApplicationService.preview(eq(workspaceId), any()))
                .thenReturn(new CoWritePreviewResponse(
                        previewId,
                        workspaceId,
                        draftId,
                        "improve_expression",
                        Map.of("mode", "full_draft"),
                        "更自然",
                        Map.of("keepStudentVoice", true),
                        "标题",
                        "候选正文",
                        Map.of(),
                        Map.of(
                                "changed", true,
                                "paragraphDiffs", List.of(Map.of("paragraphId", "p1", "intentLabel", "表达优化")),
                                "conflictWarnings", List.of(Map.of("code", "NO_MAJOR_CONFLICT", "level", "LOW")),
                                "recheckSuggestion", Map.of("shouldRecheck", true, "reviewItemCount", 1)
                        ),
                        "READY",
                        OffsetDateTime.now(),
                        null
                ));

        mockMvc.perform(post("/api/v1/workspaces/{id}/co-write/preview", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftVersionId": "%s",
                                  "action": "improve_expression",
                                  "instruction": "更自然",
                                  "controls": {
                                    "keepStudentVoice": true
                                  }
                                }
                                """.formatted(draftId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.candidateDraftText").value("候选正文"))
                .andExpect(jsonPath("$.data.diffSummary.paragraphDiffs[0].intentLabel").value("表达优化"))
                .andExpect(jsonPath("$.data.diffSummary.recheckSuggestion.shouldRecheck").value(true));
    }

    @Test
    void applyPreviewShouldReturnNewDraftVersion() throws Exception {
        UUID previewId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(coWritePreviewApplicationService.apply(previewId))
                .thenReturn(new DraftResponse(
                        draftId,
                        UUID.randomUUID(),
                        2,
                        "标题",
                        Map.of(),
                        List.of(),
                        "应用后的正文",
                        Map.of(),
                        GenerationStatus.SUCCESS,
                        "system-ai",
                        OffsetDateTime.now()
                ));

        mockMvc.perform(post("/api/v1/co-write-previews/{id}/apply", previewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.versionNo").value(2))
                .andExpect(jsonPath("$.data.draftText").value("应用后的正文"));
    }

    @Test
    void directCoWriteShouldRemainCompatible() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(draftApplicationService.coWrite(eq(workspaceId), any(), any(), any(), any(), any()))
                .thenReturn(new JobResponse(UUID.randomUUID().toString(), "success"));

        mockMvc.perform(post("/api/v1/workspaces/{id}/co-write", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftVersionId": "%s",
                                  "action": "improve_expression",
                                  "instruction": "更自然"
                                }
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("success"));
    }
}
