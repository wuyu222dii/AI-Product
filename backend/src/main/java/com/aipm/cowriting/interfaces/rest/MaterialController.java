package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.material.MaterialResponse;
import com.aipm.cowriting.application.dto.material.MaterialPreviewResponse;
import com.aipm.cowriting.application.dto.material.UpdateBibliographicMetadataRequest;
import com.aipm.cowriting.application.dto.material.UpdateMaterialCategoryRequest;
import com.aipm.cowriting.application.service.LocalMaterialStorageService;
import com.aipm.cowriting.application.service.MaterialApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.api.PagedResponse;
import com.aipm.cowriting.common.api.Pagination;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class MaterialController {

    private final MaterialApplicationService materialApplicationService;
    private final LocalMaterialStorageService localMaterialStorageService;

    public MaterialController(
            MaterialApplicationService materialApplicationService,
            LocalMaterialStorageService localMaterialStorageService
    ) {
        this.materialApplicationService = materialApplicationService;
        this.localMaterialStorageService = localMaterialStorageService;
    }

    @PostMapping(value = RestConstants.API_V1 + "/workspaces/{id}/materials", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, List<MaterialResponse>>>> upload(
            @PathVariable("id") UUID workspaceId,
            @RequestPart(name = "files", required = false) List<MultipartFile> files,
            @RequestParam(name = "plainText", required = false) String plainText,
            @RequestParam(name = "externalLink", required = false) String externalLink,
            @RequestParam(name = "sourceType") String sourceType,
            @RequestParam(name = "isKeyMaterial", defaultValue = "false") boolean isKeyMaterial,
            HttpServletRequest httpServletRequest
    ) {
        boolean emptyFiles = files == null || files.isEmpty();
        boolean emptyText = plainText == null || plainText.isBlank();
        boolean emptyLink = externalLink == null || externalLink.isBlank();
        if (emptyFiles && emptyText && emptyLink) {
            throw new BusinessException(
                    ErrorCode.MATERIAL_UPLOAD_EMPTY,
                    HttpStatus.BAD_REQUEST.value(),
                    "files / plainText / externalLink 至少传一个"
            );
        }

        String filename = emptyFiles ? (emptyText ? "external-link.txt" : "pasted-text.txt") : files.get(0).getOriginalFilename();
        String fileType = emptyFiles ? (emptyText ? "txt" : "link") : safeFileType(files.get(0));
        String storagePath = emptyFiles ? null : localMaterialStorageService.store(workspaceId, files.get(0));

        MaterialResponse material = materialApplicationService.createStub(
                workspaceId,
                filename,
                fileType,
                sourceType,
                isKeyMaterial,
                storagePath,
                plainText,
                externalLink
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(Map.of("items", List.of(material)), RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping(RestConstants.API_V1 + "/workspaces/{id}/materials")
    public ResponseEntity<ApiResponse<PagedResponse<MaterialResponse>>> list(
            @PathVariable("id") UUID workspaceId,
            HttpServletRequest httpServletRequest
    ) {
        List<MaterialResponse> items = materialApplicationService.list(workspaceId);
        PagedResponse<MaterialResponse> response = new PagedResponse<>(items, new Pagination(1, items.size(), items.size()));
        return ResponseEntity.ok(ApiResponse.success(response, RequestMetaUtil.meta(httpServletRequest)));
    }

    @GetMapping(RestConstants.API_V1 + "/materials/{id}/preview")
    public ResponseEntity<ApiResponse<MaterialPreviewResponse>> preview(
            @PathVariable("id") UUID materialId,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                materialApplicationService.preview(materialId),
                RequestMetaUtil.meta(httpServletRequest)
        ));
    }

    @GetMapping(RestConstants.API_V1 + "/materials/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable("id") UUID materialId) throws IOException {
        var material = materialApplicationService.getMaterialForFile(materialId);
        Resource resource = new FileSystemResource(localMaterialStorageService.resolve(material.getStoragePath()));
        String contentType = Files.probeContentType(resource.getFile().toPath());
        if (contentType == null) {
            contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + material.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource);
    }

    @PostMapping(RestConstants.API_V1 + "/materials/{id}/preprocess")
    public ResponseEntity<ApiResponse<JobResponse>> preprocess(
            @PathVariable("id") UUID materialId,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(materialApplicationService.triggerPreprocess(materialId), RequestMetaUtil.meta(httpServletRequest)));
    }

    @PostMapping(RestConstants.API_V1 + "/materials/{id}/ai-parse")
    public ResponseEntity<ApiResponse<JobResponse>> aiParse(
            @PathVariable("id") UUID materialId,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(materialApplicationService.triggerAiParse(materialId), RequestMetaUtil.meta(httpServletRequest)));
    }

    @PostMapping(RestConstants.API_V1 + "/materials/{id}/supplement")
    public ResponseEntity<ApiResponse<Map<String, Object>>> supplement(
            @PathVariable("id") UUID materialId,
            @RequestParam(name = "supplementText") String supplementText,
            @RequestParam(name = "pageRef", required = false) Integer pageRef,
            HttpServletRequest httpServletRequest
    ) {
        materialApplicationService.supplement(materialId, supplementText, pageRef);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", UUID.randomUUID());
        response.put("materialId", materialId);
        response.put("accepted", true);
        response.put("pageRef", pageRef);
        return ResponseEntity.ok(ApiResponse.success(
                response,
                RequestMetaUtil.meta(httpServletRequest)
        ));
    }

    @PatchMapping(RestConstants.API_V1 + "/materials/{id}/category")
    public ResponseEntity<ApiResponse<MaterialResponse>> updateCategory(
            @PathVariable("id") UUID materialId,
            @Valid @RequestBody UpdateMaterialCategoryRequest requestBody,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                materialApplicationService.updateCategory(materialId, requestBody),
                RequestMetaUtil.meta(httpServletRequest)
        ));
    }

    @PatchMapping(RestConstants.API_V1 + "/materials/{id}/bibliographic-metadata")
    public ResponseEntity<ApiResponse<MaterialResponse>> updateBibliographicMetadata(
            @PathVariable("id") UUID materialId,
            @Valid @RequestBody UpdateBibliographicMetadataRequest requestBody,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                materialApplicationService.updateBibliographicMetadata(materialId, requestBody),
                RequestMetaUtil.meta(httpServletRequest)
        ));
    }

    private String safeFileType(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            return "bin";
        }
        return originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
    }
}
