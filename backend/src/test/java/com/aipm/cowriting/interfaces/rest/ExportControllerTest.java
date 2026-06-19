package com.aipm.cowriting.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.ExportApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ExportController.class)
@Import(GlobalExceptionHandler.class)
class ExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExportApplicationService exportApplicationService;

    @TempDir
    private Path tempDir;

    @Test
    void exportShouldReturnAcceptedJob() throws Exception {
        UUID draftId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(exportApplicationService.export(draftId, new com.aipm.cowriting.application.dto.export.ExportRequest("docx", false, "APA")))
                .thenReturn(new JobResponse(jobId.toString(), "success"));

        mockMvc.perform(post("/api/v1/drafts/{id}/export", draftId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "format": "docx",
                                  "includeComments": false,
                                  "citationStyle": "APA"
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    void downloadShouldReturnGeneratedFile() throws Exception {
        UUID jobId = UUID.randomUUID();
        Path output = tempDir.resolve("draft.docx");
        Files.writeString(output, "fake docx content");
        when(exportApplicationService.loadExport(jobId)).thenReturn(new FileSystemResource(output));

        mockMvc.perform(get("/api/v1/exports/{jobId}/download", jobId))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"draft.docx\""));
    }
}
