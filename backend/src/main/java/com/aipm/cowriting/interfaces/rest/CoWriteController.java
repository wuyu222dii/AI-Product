package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.cowrite.CoWriteRequest;
import com.aipm.cowriting.application.dto.cowrite.CoWritePreviewResponse;
import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.CoWritePreviewApplicationService;
import com.aipm.cowriting.application.service.DraftApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CoWriteController {

    private final DraftApplicationService draftApplicationService;
    private final CoWritePreviewApplicationService coWritePreviewApplicationService;

    public CoWriteController(
            DraftApplicationService draftApplicationService,
            CoWritePreviewApplicationService coWritePreviewApplicationService
    ) {
        this.draftApplicationService = draftApplicationService;
        this.coWritePreviewApplicationService = coWritePreviewApplicationService;
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/co-write")
    public ResponseEntity<ApiResponse<JobResponse>> coWrite(
            @PathVariable("id") UUID workspaceId,
            @Valid @RequestBody CoWriteRequest requestBody,
            HttpServletRequest request
    ) {
        JobResponse response = draftApplicationService.coWrite(
                workspaceId,
                requestBody.draftVersionId(),
                requestBody.action(),
                requestBody.targetRange(),
                requestBody.instruction(),
                requestBody.controls()
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/co-write/preview")
    public ResponseEntity<ApiResponse<CoWritePreviewResponse>> preview(
            @PathVariable("id") UUID workspaceId,
            @Valid @RequestBody CoWriteRequest requestBody,
            HttpServletRequest request
    ) {
        CoWritePreviewResponse response = coWritePreviewApplicationService.preview(workspaceId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/co-write-previews/{id}/apply")
    public ResponseEntity<ApiResponse<DraftResponse>> applyPreview(
            @PathVariable("id") UUID previewId,
            HttpServletRequest request
    ) {
        DraftResponse response = coWritePreviewApplicationService.apply(previewId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/co-write-previews/{id}/discard")
    public ResponseEntity<ApiResponse<CoWritePreviewResponse>> discardPreview(
            @PathVariable("id") UUID previewId,
            HttpServletRequest request
    ) {
        CoWritePreviewResponse response = coWritePreviewApplicationService.discard(previewId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }
}
