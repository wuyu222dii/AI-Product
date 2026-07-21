package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.user.CurrentUserResponse;
import com.aipm.cowriting.application.dto.user.UpdateCurrentUserRequest;
import com.aipm.cowriting.application.service.UserProfileApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CurrentUserController {

    private final UserProfileApplicationService service;

    public CurrentUserController(UserProfileApplicationService service) {
        this.service = service;
    }

    @GetMapping(RestConstants.API_V1 + "/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> get(HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(service.getCurrent(), RequestMetaUtil.meta(request)));
    }

    @PatchMapping(RestConstants.API_V1 + "/me")
    public ResponseEntity<ApiResponse<CurrentUserResponse>> update(
            @Valid @RequestBody UpdateCurrentUserRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(service.update(body), RequestMetaUtil.meta(request)));
    }
}
