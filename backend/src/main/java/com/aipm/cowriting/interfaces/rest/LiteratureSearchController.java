package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.literature.LiteratureCandidateRequest;
import com.aipm.cowriting.application.dto.literature.LiteratureCandidateResponse;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchRequest;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchResponse;
import com.aipm.cowriting.application.service.LiteratureCandidateApplicationService;
import com.aipm.cowriting.application.service.LiteratureSearchService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LiteratureSearchController {

    private final LiteratureSearchService literatureSearchService;
    private final LiteratureCandidateApplicationService literatureCandidateApplicationService;

    public LiteratureSearchController(
            LiteratureSearchService literatureSearchService,
            LiteratureCandidateApplicationService literatureCandidateApplicationService
    ) {
        this.literatureSearchService = literatureSearchService;
        this.literatureCandidateApplicationService = literatureCandidateApplicationService;
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/literature-search")
    public ResponseEntity<ApiResponse<LiteratureSearchResponse>> search(
            @PathVariable("id") UUID workspaceId,
            @RequestBody(required = false) LiteratureSearchRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LiteratureSearchResponse response = literatureSearchService.search(workspaceId, request);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/literature-candidates")
    public ResponseEntity<ApiResponse<LiteratureCandidateResponse>> saveCandidate(
            @PathVariable("id") UUID workspaceId,
            @RequestBody LiteratureCandidateRequest request,
            HttpServletRequest httpServletRequest
    ) {
        LiteratureCandidateResponse response = literatureCandidateApplicationService.save(workspaceId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping(RestConstants.API_V1 + "/workspaces/{id}/literature-candidates")
    public ResponseEntity<ApiResponse<List<LiteratureCandidateResponse>>> listCandidates(
            @PathVariable("id") UUID workspaceId,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                literatureCandidateApplicationService.list(workspaceId),
                RequestMetaUtil.meta(httpServletRequest)
        ));
    }
}
