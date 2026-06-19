package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.review.AppealRequest;
import com.aipm.cowriting.application.dto.review.AppealResponse;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.review.UpdateReviewStatusRequest;
import com.aipm.cowriting.application.service.ReviewApplicationService;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReviewController {

    private final ReviewApplicationService reviewApplicationService;

    public ReviewController(ReviewApplicationService reviewApplicationService) {
        this.reviewApplicationService = reviewApplicationService;
    }

    @GetMapping(RestConstants.API_V1 + "/drafts/{id}/review-items")
    public ResponseEntity<ApiResponse<PagedResponse<ReviewItemResponse>>> list(
            @PathVariable("id") UUID draftId,
            HttpServletRequest request
    ) {
        List<ReviewItemResponse> items = reviewApplicationService.list(draftId);
        PagedResponse<ReviewItemResponse> response = new PagedResponse<>(items, new Pagination(1, items.size(), items.size()));
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PatchMapping(RestConstants.API_V1 + "/review-items/{id}/status")
    public ResponseEntity<ApiResponse<ReviewItemResponse>> updateStatus(
            @PathVariable("id") UUID reviewItemId,
            @Valid @RequestBody UpdateReviewStatusRequest requestBody,
            HttpServletRequest request
    ) {
        ReviewItemResponse response = reviewApplicationService.updateStatus(reviewItemId, requestBody);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/review-items/{id}/appeal")
    public ResponseEntity<ApiResponse<AppealResponse>> createAppeal(
            @PathVariable("id") UUID reviewItemId,
            @Valid @RequestBody AppealRequest requestBody,
            HttpServletRequest request
    ) {
        AppealResponse response = reviewApplicationService.createAppeal(reviewItemId, requestBody);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/review-items/{id}/recheck")
    public ResponseEntity<ApiResponse<ReviewItemResponse>> recheck(
            @PathVariable("id") UUID reviewItemId,
            HttpServletRequest request
    ) {
        ReviewItemResponse response = reviewApplicationService.recheck(reviewItemId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/appeals/{id}")
    public ResponseEntity<ApiResponse<AppealResponse>> getAppeal(
            @PathVariable("id") UUID appealId,
            HttpServletRequest request
    ) {
        AppealResponse response = reviewApplicationService.getAppeal(appealId);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }
}
