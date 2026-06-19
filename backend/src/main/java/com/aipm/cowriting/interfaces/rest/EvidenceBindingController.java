package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.evidence.EvidenceBindingItemResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.UpdateEvidenceBindingStatusRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.EvidenceBindingApplicationService;
import com.aipm.cowriting.application.service.EvidenceBindingRebuildJobService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class EvidenceBindingController {

    private final EvidenceBindingApplicationService evidenceBindingApplicationService;
    private final EvidenceBindingRebuildJobService evidenceBindingRebuildJobService;

    public EvidenceBindingController(
            EvidenceBindingApplicationService evidenceBindingApplicationService,
            EvidenceBindingRebuildJobService evidenceBindingRebuildJobService
    ) {
        this.evidenceBindingApplicationService = evidenceBindingApplicationService;
        this.evidenceBindingRebuildJobService = evidenceBindingRebuildJobService;
    }

    @GetMapping(RestConstants.API_V1 + "/drafts/{id}/evidence-bindings")
    public ResponseEntity<ApiResponse<EvidenceBindingSummaryResponse>> get(
            @PathVariable("id") UUID draftId,
            HttpServletRequest request
    ) {
        EvidenceBindingSummaryResponse response = evidenceBindingApplicationService.get(draftId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/drafts/{id}/evidence-bindings/rebuild")
    public ResponseEntity<ApiResponse<JobResponse>> rebuild(
            @PathVariable("id") UUID draftId,
            HttpServletRequest request
    ) {
        JobResponse response = evidenceBindingRebuildJobService.enqueue(draftId);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PatchMapping(RestConstants.API_V1 + "/evidence-bindings/{id}/status")
    public ResponseEntity<ApiResponse<EvidenceBindingItemResponse>> updateStatus(
            @PathVariable("id") UUID bindingId,
            @Valid @RequestBody UpdateEvidenceBindingStatusRequest requestBody,
            HttpServletRequest request
    ) {
        EvidenceBindingItemResponse response = evidenceBindingApplicationService.updateStatus(bindingId, requestBody.status());
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }
}
