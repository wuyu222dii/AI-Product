package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.DocumentReadinessResponse;
import com.aipm.cowriting.application.dto.academic.DocumentSectionVersionResponse;
import com.aipm.cowriting.application.dto.academic.GenerateSectionRequest;
import com.aipm.cowriting.application.dto.academic.SectionCoWritePreviewResponse;
import com.aipm.cowriting.application.dto.academic.SectionCoWriteRequest;
import com.aipm.cowriting.application.dto.academic.ApplySectionSplitRequest;
import com.aipm.cowriting.application.dto.academic.SectionSplitPreviewResponse;
import com.aipm.cowriting.application.dto.academic.UpdateDocumentSectionRequest;
import com.aipm.cowriting.application.service.AcademicDocumentApplicationService;
import com.aipm.cowriting.application.service.AcademicSectionApplicationService;
import com.aipm.cowriting.application.service.AcademicReadinessApplicationService;
import com.aipm.cowriting.application.service.ScopedEvidenceBindingJobService;
import com.aipm.cowriting.application.service.LegacySectionSplitApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AcademicSectionController {

    private final AcademicDocumentApplicationService documentService;
    private final AcademicSectionApplicationService sectionService;
    private final AcademicReadinessApplicationService readinessService;
    private final ScopedEvidenceBindingJobService evidenceJobService;
    private final LegacySectionSplitApplicationService splitService;

    public AcademicSectionController(
            AcademicDocumentApplicationService documentService,
            AcademicSectionApplicationService sectionService,
            AcademicReadinessApplicationService readinessService,
            ScopedEvidenceBindingJobService evidenceJobService,
            LegacySectionSplitApplicationService splitService
    ) {
        this.documentService = documentService;
        this.sectionService = sectionService;
        this.readinessService = readinessService;
        this.evidenceJobService = evidenceJobService;
        this.splitService = splitService;
    }

    @PatchMapping(RestConstants.API_V1 + "/sections/{id}")
    public ResponseEntity<ApiResponse<DocumentSectionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateDocumentSectionRequest body,
            HttpServletRequest request
    ) {
        DocumentSectionResponse response = documentService.updateSection(id, body);
        evidenceJobService.enqueueSection(id);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/sections/{id}/versions")
    public ResponseEntity<ApiResponse<List<DocumentSectionVersionResponse>>> versions(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.listSectionVersions(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/section-versions/{id}/restore")
    public ResponseEntity<ApiResponse<DocumentSectionResponse>> restore(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        DocumentSectionResponse response = documentService.restoreSectionVersion(id);
        evidenceJobService.enqueueSection(response.id());
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/generate")
    public ResponseEntity<ApiResponse<DocumentSectionResponse>> generate(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) GenerateSectionRequest body,
            HttpServletRequest request
    ) {
        DocumentSectionResponse response = sectionService.generate(id, body);
        evidenceJobService.enqueueSection(id);
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/readiness-check")
    public ResponseEntity<ApiResponse<DocumentReadinessResponse>> readiness(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(readinessService.checkSection(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/co-write/preview")
    public ResponseEntity<ApiResponse<SectionCoWritePreviewResponse>> preview(
            @PathVariable UUID id,
            @Valid @RequestBody SectionCoWriteRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.previewCoWrite(id, body), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/section-co-write-previews/{id}/apply")
    public ResponseEntity<ApiResponse<DocumentSectionResponse>> apply(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) com.aipm.cowriting.application.dto.academic.ApplySectionCoWritePreviewRequest body,
            HttpServletRequest request
    ) {
        DocumentSectionResponse response = sectionService.applyPreview(id, body);
        evidenceJobService.enqueueSection(response.id());
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/section-co-write-previews/{id}/discard")
    public ResponseEntity<ApiResponse<SectionCoWritePreviewResponse>> discard(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(sectionService.discardPreview(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/split-preview")
    public ResponseEntity<ApiResponse<SectionSplitPreviewResponse>> splitPreview(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(splitService.preview(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/sections/{id}/split")
    public ResponseEntity<ApiResponse<List<DocumentSectionResponse>>> split(
            @PathVariable UUID id,
            @Valid @RequestBody ApplySectionSplitRequest body,
            HttpServletRequest request
    ) {
        List<DocumentSectionResponse> response = splitService.apply(id, body);
        response.forEach(section -> evidenceJobService.enqueueSection(section.id()));
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(request)));
    }
}
