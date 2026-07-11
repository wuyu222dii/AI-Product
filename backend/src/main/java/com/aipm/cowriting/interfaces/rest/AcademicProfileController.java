package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.academic.AcademicProfileRequest;
import com.aipm.cowriting.application.dto.academic.AcademicProfileResponse;
import com.aipm.cowriting.application.service.AcademicProfileApplicationService;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AcademicProfileController {

    private final AcademicProfileApplicationService service;

    public AcademicProfileController(AcademicProfileApplicationService service) {
        this.service = service;
    }

    @GetMapping(RestConstants.API_V1 + "/workspaces/{id}/academic-profile")
    public ResponseEntity<ApiResponse<AcademicProfileResponse>> get(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.getOrCreateDefault(id), RequestMetaUtil.meta(request)));
    }

    @PatchMapping(RestConstants.API_V1 + "/workspaces/{id}/academic-profile")
    public ResponseEntity<ApiResponse<AcademicProfileResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody AcademicProfileRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.upsert(id, body), RequestMetaUtil.meta(request)));
    }
}
