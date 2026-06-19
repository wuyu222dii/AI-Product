package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.knowledge.KnowledgeBuildResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeChunkResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchRequest;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchResponse;
import com.aipm.cowriting.application.service.KnowledgeBaseApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.api.PagedResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class KnowledgeBaseController {

    private final KnowledgeBaseApplicationService knowledgeBaseApplicationService;

    public KnowledgeBaseController(KnowledgeBaseApplicationService knowledgeBaseApplicationService) {
        this.knowledgeBaseApplicationService = knowledgeBaseApplicationService;
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/knowledge-base/build")
    public ResponseEntity<ApiResponse<KnowledgeBuildResponse>> build(
            @PathVariable("id") UUID workspaceId,
            HttpServletRequest httpServletRequest
    ) {
        KnowledgeBuildResponse response = knowledgeBaseApplicationService.build(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping(RestConstants.API_V1 + "/workspaces/{id}/knowledge-base/chunks")
    public ResponseEntity<ApiResponse<PagedResponse<KnowledgeChunkResponse>>> chunks(
            @PathVariable("id") UUID workspaceId,
            HttpServletRequest httpServletRequest
    ) {
        PagedResponse<KnowledgeChunkResponse> response = knowledgeBaseApplicationService.list(workspaceId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/knowledge-base/search")
    public ResponseEntity<ApiResponse<KnowledgeSearchResponse>> search(
            @PathVariable("id") UUID workspaceId,
            @Valid @RequestBody KnowledgeSearchRequest request,
            HttpServletRequest httpServletRequest
    ) {
        KnowledgeSearchResponse response = knowledgeBaseApplicationService.search(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }
}
