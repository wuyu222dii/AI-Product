package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.draft.DraftResponse;
import com.aipm.cowriting.application.dto.draft.GenerateDraftRequest;
import com.aipm.cowriting.application.dto.draft.UpdateDraftRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.DraftApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.api.PagedResponse;
import com.aipm.cowriting.common.api.Pagination;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DraftController {

    private final DraftApplicationService draftApplicationService;

    public DraftController(DraftApplicationService draftApplicationService) {
        this.draftApplicationService = draftApplicationService;
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/generate-draft")
    public ResponseEntity<ApiResponse<JobResponse>> generate(
            @PathVariable("id") UUID workspaceId,
            @Valid @RequestBody GenerateDraftRequest request,
            HttpServletRequest httpServletRequest
    ) {
        JobResponse response = draftApplicationService.generate(workspaceId, request.requirementSnapshotId(), request.mode());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping(RestConstants.API_V1 + "/drafts/{id}")
    public ResponseEntity<ApiResponse<DraftResponse>> get(
            @PathVariable("id") UUID draftId,
            HttpServletRequest httpServletRequest
    ) {
        DraftResponse response = draftApplicationService.get(draftId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @PatchMapping(RestConstants.API_V1 + "/drafts/{id}")
    public ResponseEntity<ApiResponse<DraftResponse>> update(
            @PathVariable("id") UUID draftId,
            @Valid @RequestBody UpdateDraftRequest request,
            HttpServletRequest httpServletRequest
    ) {
        DraftResponse response = draftApplicationService.updateDraft(draftId, request);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @PostMapping(RestConstants.API_V1 + "/drafts/{id}/restore")
    public ResponseEntity<ApiResponse<DraftResponse>> restore(
            @PathVariable("id") UUID draftId,
            HttpServletRequest httpServletRequest
    ) {
        DraftResponse response = draftApplicationService.restore(draftId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping(RestConstants.API_V1 + "/workspaces/{id}/drafts")
    public ResponseEntity<ApiResponse<PagedResponse<DraftResponse>>> list(
            @PathVariable("id") UUID workspaceId,
            HttpServletRequest httpServletRequest
    ) {
        List<DraftResponse> items = draftApplicationService.listByWorkspace(workspaceId);
        PagedResponse<DraftResponse> response = new PagedResponse<>(items, new Pagination(1, items.size(), items.size()));
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }
}
