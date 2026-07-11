package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.academic.AcademicDocumentResponse;
import com.aipm.cowriting.application.dto.academic.AiActionLogResponse;
import com.aipm.cowriting.application.dto.academic.AssembledDocumentResponse;
import com.aipm.cowriting.application.dto.academic.CreateAcademicDocumentRequest;
import com.aipm.cowriting.application.dto.academic.CreateDocumentSectionRequest;
import com.aipm.cowriting.application.dto.academic.DocumentMaterialLinkRequest;
import com.aipm.cowriting.application.dto.academic.DocumentMaterialLinkResponse;
import com.aipm.cowriting.application.dto.academic.DocumentReadinessResponse;
import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.ReorderDocumentSectionsRequest;
import com.aipm.cowriting.application.dto.academic.UpdateAcademicDocumentRequest;
import com.aipm.cowriting.application.dto.export.ExportRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.AcademicDocumentApplicationService;
import com.aipm.cowriting.application.service.AcademicDocumentExportService;
import com.aipm.cowriting.application.service.AcademicReadinessApplicationService;
import com.aipm.cowriting.application.service.AiActionLogApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
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
public class AcademicDocumentController {

    private final AcademicDocumentApplicationService documentService;
    private final AcademicReadinessApplicationService readinessService;
    private final AiActionLogApplicationService actionLogService;
    private final AcademicDocumentExportService exportService;

    public AcademicDocumentController(
            AcademicDocumentApplicationService documentService,
            AcademicReadinessApplicationService readinessService,
            AiActionLogApplicationService actionLogService,
            AcademicDocumentExportService exportService
    ) {
        this.documentService = documentService;
        this.readinessService = readinessService;
        this.actionLogService = actionLogService;
        this.exportService = exportService;
    }

    @PostMapping(RestConstants.API_V1 + "/workspaces/{id}/documents")
    public ResponseEntity<ApiResponse<AcademicDocumentResponse>> create(
            @PathVariable UUID id,
            @Valid @RequestBody CreateAcademicDocumentRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(documentService.create(id, body), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/workspaces/{id}/documents")
    public ResponseEntity<ApiResponse<List<AcademicDocumentResponse>>> list(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.list(id), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}")
    public ResponseEntity<ApiResponse<AcademicDocumentResponse>> get(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(documentService.get(id), RequestMetaUtil.meta(request)));
    }

    @PatchMapping(RestConstants.API_V1 + "/documents/{id}")
    public ResponseEntity<ApiResponse<AcademicDocumentResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateAcademicDocumentRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.update(id, body), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/activate")
    public ResponseEntity<ApiResponse<AcademicDocumentResponse>> activate(@PathVariable UUID id, HttpServletRequest request) {
        return ResponseEntity.ok(ApiResponse.success(documentService.activate(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/sections")
    public ResponseEntity<ApiResponse<DocumentSectionResponse>> createSection(
            @PathVariable UUID id,
            @Valid @RequestBody CreateDocumentSectionRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(documentService.createSection(id, body), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/sections")
    public ResponseEntity<ApiResponse<List<DocumentSectionResponse>>> listSections(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.listSections(id), RequestMetaUtil.meta(request)));
    }

    @PatchMapping(RestConstants.API_V1 + "/documents/{id}/sections/order")
    public ResponseEntity<ApiResponse<List<DocumentSectionResponse>>> reorderSections(
            @PathVariable UUID id,
            @Valid @RequestBody ReorderDocumentSectionsRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                documentService.reorderSections(id, body),
                RequestMetaUtil.meta(request)
        ));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/materials")
    public ResponseEntity<ApiResponse<DocumentMaterialLinkResponse>> linkMaterial(
            @PathVariable UUID id,
            @Valid @RequestBody DocumentMaterialLinkRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.linkMaterial(id, body), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/materials")
    public ResponseEntity<ApiResponse<List<DocumentMaterialLinkResponse>>> listMaterials(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.listMaterialLinks(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/readiness-check")
    public ResponseEntity<ApiResponse<DocumentReadinessResponse>> readiness(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(readinessService.check(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/assemble")
    public ResponseEntity<ApiResponse<AssembledDocumentResponse>> assemble(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(documentService.assemble(id), RequestMetaUtil.meta(request)));
    }

    @PostMapping(RestConstants.API_V1 + "/documents/{id}/export")
    public ResponseEntity<ApiResponse<JobResponse>> export(
            @PathVariable UUID id,
            @Valid @RequestBody ExportRequest body,
            HttpServletRequest request
    ) {
        return ResponseEntity.accepted().body(ApiResponse.success(exportService.export(id, body), RequestMetaUtil.meta(request)));
    }

    @GetMapping(RestConstants.API_V1 + "/documents/{id}/ai-actions")
    public ResponseEntity<ApiResponse<List<AiActionLogResponse>>> aiActions(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        documentService.get(id);
        return ResponseEntity.ok(ApiResponse.success(actionLogService.listByDocument(id), RequestMetaUtil.meta(request)));
    }
}
