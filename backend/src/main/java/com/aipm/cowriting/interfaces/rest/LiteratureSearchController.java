package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.literature.LiteratureSearchRequest;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchResponse;
import com.aipm.cowriting.application.service.LiteratureSearchService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LiteratureSearchController {

    private final LiteratureSearchService literatureSearchService;

    public LiteratureSearchController(LiteratureSearchService literatureSearchService) {
        this.literatureSearchService = literatureSearchService;
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
}
