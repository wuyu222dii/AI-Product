package com.aipm.cowriting.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.service.JobApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = JobController.class)
@Import(GlobalExceptionHandler.class)
class JobControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JobApplicationService jobApplicationService;

    @Test
    void getShouldReturnJobDetail() throws Exception {
        UUID jobId = UUID.randomUUID();
        UUID workspaceId = UUID.randomUUID();
        Map<String, Object> job = new LinkedHashMap<>();
        job.put("id", jobId);
        job.put("workspaceId", workspaceId);
        job.put("jobType", "export_docx");
        job.put("status", "success");
        job.put("progress", 100);
        job.put("inputRef", Map.of());
        job.put("outputRef", Map.of("downloadUrl", "/api/v1/exports/%s/download".formatted(jobId)));
        job.put("errorMessage", null);
        job.put("createdAt", OffsetDateTime.now().toString());
        job.put("updatedAt", OffsetDateTime.now().toString());
        when(jobApplicationService.getJob(jobId)).thenReturn(job);

        mockMvc.perform(get("/api/v1/jobs/{id}", jobId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("success"))
                .andExpect(jsonPath("$.data.progress").value(100))
                .andExpect(jsonPath("$.data.outputRef.downloadUrl").exists());
    }
}
