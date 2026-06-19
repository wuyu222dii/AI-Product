package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.workspace.CreateWorkspaceRequest;
import com.aipm.cowriting.application.dto.workspace.WorkspaceResponse;
import com.aipm.cowriting.application.service.WorkspaceApplicationService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.API_V1 + "/workspaces")
public class WorkspaceController {

    private final WorkspaceApplicationService workspaceApplicationService;

    public WorkspaceController(WorkspaceApplicationService workspaceApplicationService) {
        this.workspaceApplicationService = workspaceApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceResponse>> create(
            @Valid @RequestBody CreateWorkspaceRequest request,
            HttpServletRequest httpServletRequest
    ) {
        WorkspaceResponse response = workspaceApplicationService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<WorkspaceResponse>>> list(HttpServletRequest httpServletRequest) {
        List<WorkspaceResponse> items = workspaceApplicationService.list();
        PagedResponse<WorkspaceResponse> response = new PagedResponse<>(
                items,
                new Pagination(1, items.size(), items.size())
        );
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<WorkspaceResponse>> get(
            @PathVariable UUID id,
            HttpServletRequest httpServletRequest
    ) {
        WorkspaceResponse response = workspaceApplicationService.get(id);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }
}
