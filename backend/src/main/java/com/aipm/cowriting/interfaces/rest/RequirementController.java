package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.requirement.CreateRequirementSnapshotRequest;
import com.aipm.cowriting.application.dto.requirement.RequirementSnapshotResponse;
import com.aipm.cowriting.application.service.RequirementApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.API_V1 + "/workspaces/{id}/requirement-snapshot")
public class RequirementController {

    private final RequirementApplicationService requirementApplicationService;

    public RequirementController(RequirementApplicationService requirementApplicationService) {
        this.requirementApplicationService = requirementApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RequirementSnapshotResponse>> create(
            @PathVariable("id") UUID workspaceId,
            @Valid @RequestBody CreateRequirementSnapshotRequest request,
            HttpServletRequest httpServletRequest
    ) {
        RequirementSnapshotResponse response = requirementApplicationService.create(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<RequirementSnapshotResponse>> latest(
            @PathVariable("id") UUID workspaceId,
            HttpServletRequest httpServletRequest
    ) {
        RequirementSnapshotResponse response = requirementApplicationService.latest(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }
}
