package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.guide.OnboardingCompleteRequest;
import com.aipm.cowriting.application.dto.guide.OnboardingCompleteResponse;
import com.aipm.cowriting.application.service.OnboardingApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OnboardingController {

    private final OnboardingApplicationService service;

    public OnboardingController(OnboardingApplicationService service) {
        this.service = service;
    }

    @PostMapping(RestConstants.API_V1 + "/onboarding/complete")
    public ResponseEntity<ApiResponse<OnboardingCompleteResponse>> complete(
            @Valid @RequestBody OnboardingCompleteRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.complete(body), RequestMetaUtil.meta(request)));
    }
}
