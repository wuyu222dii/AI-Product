package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.job.JobDetailResponse;
import com.aipm.cowriting.application.service.JobApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

    private final JobApplicationService jobApplicationService;

    public JobController(JobApplicationService jobApplicationService) {
        this.jobApplicationService = jobApplicationService;
    }

    @GetMapping(RestConstants.API_V1 + "/jobs/{id}")
    public ResponseEntity<ApiResponse<JobDetailResponse>> get(
            @PathVariable("id") UUID jobId,
            HttpServletRequest request
    ) {
        Map<String, Object> job = jobApplicationService.getJobForCurrentUser(jobId);
        JobDetailResponse response = new JobDetailResponse(
                (UUID) job.get("id"),
                (UUID) job.get("workspaceId"),
                (String) job.get("jobType"),
                (String) job.get("status"),
                (Integer) job.get("progress"),
                job.get("inputRef"),
                job.get("outputRef"),
                (String) job.get("errorMessage"),
                (String) job.get("createdAt"),
                (String) job.get("updatedAt")
        );
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }
}
