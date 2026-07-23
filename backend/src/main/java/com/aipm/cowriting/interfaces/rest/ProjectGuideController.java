package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.guide.ProjectGuideRequest;
import com.aipm.cowriting.application.dto.guide.ProjectGuideResponse;
import com.aipm.cowriting.application.service.ProjectGuideApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.API_V1 + "/workspaces/{workspaceId}/guide")
public class ProjectGuideController {

    private final ProjectGuideApplicationService service;

    public ProjectGuideController(ProjectGuideApplicationService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ProjectGuideResponse>> get(
            @PathVariable UUID workspaceId,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.get(workspaceId), RequestMetaUtil.meta(request)));
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<ProjectGuideResponse>> update(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody ProjectGuideRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                service.update(workspaceId, body),
                RequestMetaUtil.meta(request)
        ));
    }
}
