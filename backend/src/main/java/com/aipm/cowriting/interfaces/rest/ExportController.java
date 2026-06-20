package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.export.ExportRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.ExportApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import jakarta.validation.Valid;
import java.nio.file.Files;
import java.util.UUID;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExportController {

    private final ExportApplicationService exportApplicationService;

    public ExportController(ExportApplicationService exportApplicationService) {
        this.exportApplicationService = exportApplicationService;
    }

    @PostMapping(RestConstants.API_V1 + "/drafts/{id}/export")
    public ResponseEntity<ApiResponse<JobResponse>> export(
            @PathVariable("id") UUID draftId,
            @Valid @RequestBody ExportRequest requestBody,
            HttpServletRequest request
    ) {
        JobResponse response = exportApplicationService.export(draftId, requestBody);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/exports/{jobId}/download")
    public ResponseEntity<Resource> download(@PathVariable("jobId") UUID jobId) throws IOException {
        Resource resource = exportApplicationService.loadExport(jobId);
        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }
}
