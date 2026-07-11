package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.academic.DocumentQualitySummaryResponse;
import com.aipm.cowriting.application.dto.evidence.DocumentEvidenceSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import com.aipm.cowriting.application.service.AcademicQualityApplicationService;
import com.aipm.cowriting.application.service.AcademicReviewApplicationService;
import com.aipm.cowriting.application.service.AcademicReviewJobService;
import com.aipm.cowriting.application.service.ScopedEvidenceBindingApplicationService;
import com.aipm.cowriting.application.service.ScopedEvidenceBindingJobService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AcademicQualityController {

    private final ScopedEvidenceBindingApplicationService evidenceService;
    private final ScopedEvidenceBindingJobService evidenceJobService;
    private final AcademicQualityApplicationService qualityService;
    private final AcademicReviewApplicationService reviewService;
    private final AcademicReviewJobService reviewJobService;

    public AcademicQualityController(
            ScopedEvidenceBindingApplicationService evidenceService,
            ScopedEvidenceBindingJobService evidenceJobService,
            AcademicQualityApplicationService qualityService,
            AcademicReviewApplicationService reviewService,
            AcademicReviewJobService reviewJobService
    ) {
        this.evidenceService = evidenceService;
        this.evidenceJobService = evidenceJobService;
        this.qualityService = qualityService;
        this.reviewService = reviewService;
        this.reviewJobService = reviewJobService;
    }

    @GetMapping(RestConstants.API_V1 + "/sections/{id}/evidence-bindings")
    public ResponseEntity<ApiResponse<EvidenceBindingSummaryResponse>> sectionEvidence(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(evidenceService.getSection(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/evidence-bindings/rebuild")
    public ResponseEntity<ApiResponse<JobResponse>> rebuildSectionEvidence(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(evidenceJobService.enqueueSection(id), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/evidence-summary")
    public ResponseEntity<ApiResponse<DocumentEvidenceSummaryResponse>> documentEvidence(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(evidenceService.documentSummary(id), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/sections/{id}/writing-risks")
    public ResponseEntity<ApiResponse<WritingRiskSummaryResponse>> sectionWritingRisks(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(qualityService.sectionWritingRisks(id), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/writing-risks")
    public ResponseEntity<ApiResponse<WritingRiskSummaryResponse>> documentWritingRisks(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(qualityService.documentWritingRisks(id), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/quality-summary")
    public ResponseEntity<ApiResponse<DocumentQualitySummaryResponse>> qualitySummary(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(qualityService.qualitySummary(id), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/review-items")
    public ResponseEntity<ApiResponse<List<ReviewItemResponse>>> documentReviews(
            @PathVariable UUID id,
            @RequestParam(required = false) UUID sectionId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String scopeType,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                reviewService.listDocument(id, sectionId, status, scopeType),
                RequestMetaUtil.meta(request)
        ));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/review-items/refresh")
    public ResponseEntity<ApiResponse<JobResponse>> refreshSectionReviews(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(reviewJobService.enqueueSection(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/review-items/refresh")
    public ResponseEntity<ApiResponse<JobResponse>> refreshDocumentReviews(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(reviewJobService.enqueueDocument(id), RequestMetaUtil.meta(request)));
    }
}
