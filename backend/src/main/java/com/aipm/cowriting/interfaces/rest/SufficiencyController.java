package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.sufficiency.MaterialSufficiencyCheckRequest;
import com.aipm.cowriting.application.dto.sufficiency.MaterialSufficiencyResponse;
import com.aipm.cowriting.application.service.SufficiencyApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(RestConstants.API_V1 + "/workspaces/{id}/material-sufficiency-check")
public class SufficiencyController {

    private final SufficiencyApplicationService sufficiencyApplicationService;

    public SufficiencyController(SufficiencyApplicationService sufficiencyApplicationService) {
        this.sufficiencyApplicationService = sufficiencyApplicationService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MaterialSufficiencyResponse>> check(
            @PathVariable("id") UUID workspaceId,
            @Valid @RequestBody MaterialSufficiencyCheckRequest request,
            HttpServletRequest httpServletRequest
    ) {
        MaterialSufficiencyResponse response = sufficiencyApplicationService.check(workspaceId, request.requirementSnapshotId());
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }
}
