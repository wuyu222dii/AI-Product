package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.AcademicDocumentResponse;
import com.aipm.cowriting.application.dto.academic.AssembledDocumentResponse;
import com.aipm.cowriting.application.dto.academic.CreateAcademicDocumentRequest;
import com.aipm.cowriting.application.dto.academic.CreateDocumentSectionRequest;
import com.aipm.cowriting.application.dto.academic.DocumentMaterialLinkRequest;
import com.aipm.cowriting.application.dto.academic.DocumentMaterialLinkResponse;
import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.DocumentSectionVersionResponse;
import com.aipm.cowriting.application.dto.academic.ReorderDocumentSectionsRequest;
import com.aipm.cowriting.application.dto.academic.UpdateAcademicDocumentRequest;
import com.aipm.cowriting.application.dto.academic.UpdateDocumentSectionRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import com.aipm.cowriting.domain.entity.DocumentMaterialLinkEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionVersionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import com.aipm.cowriting.domain.model.DocumentSectionStatus;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.DocumentMaterialLinkRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionVersionRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicDocumentApplicationService {

    private final AcademicDocumentRepository documentRepository;
    private final DocumentSectionRepository sectionRepository;
    private final DocumentSectionVersionRepository sectionVersionRepository;
    private final DocumentMaterialLinkRepository materialLinkRepository;
    private final MaterialRepository materialRepository;
    private final WorkspaceRepository workspaceRepository;
    private final AcademicProfileApplicationService profileService;
    private final AcademicRuleCatalog ruleCatalog;

    public AcademicDocumentApplicationService(
            AcademicDocumentRepository documentRepository,
            DocumentSectionRepository sectionRepository,
            DocumentSectionVersionRepository sectionVersionRepository,
            DocumentMaterialLinkRepository materialLinkRepository,
            MaterialRepository materialRepository,
            WorkspaceRepository workspaceRepository,
            AcademicProfileApplicationService profileService,
            AcademicRuleCatalog ruleCatalog
    ) {
        this.documentRepository = documentRepository;
        this.sectionRepository = sectionRepository;
        this.sectionVersionRepository = sectionVersionRepository;
        this.materialLinkRepository = materialLinkRepository;
        this.materialRepository = materialRepository;
        this.workspaceRepository = workspaceRepository;
        this.profileService = profileService;
        this.ruleCatalog = ruleCatalog;
    }

    @Transactional
    public AcademicDocumentResponse create(UUID workspaceId, CreateAcademicDocumentRequest request) {
        WorkspaceEntity workspace = getWorkspace(workspaceId);
        AcademicProjectProfileEntity profile = profileService.getEntity(workspaceId);
        AcademicDocumentEntity entity = new AcademicDocumentEntity();
        OffsetDateTime now = OffsetDateTime.now();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        entity.setDocumentType(request.documentType());
        entity.setTitle(request.title().trim());
        entity.setStatus(AcademicDocumentStatus.PLANNING);
        entity.setTargetInstitution(trimToNull(request.targetInstitution()));
        entity.setTargetVenue(trimToNull(request.targetVenue()));
        entity.setTargetLength(request.targetLength());
        entity.setLengthUnit(normalizeLengthUnit(request.lengthUnit()));
        entity.setCitationStyle(defaultString(request.citationStyle(), profile.getDefaultCitationStyle()));
        entity.setRequirementProfileJson(request.requirementProfile() == null ? Map.of() : request.requirementProfile());
        boolean primary = Boolean.TRUE.equals(request.primaryDocument()) || !documentRepository.existsByWorkspaceId(workspaceId);
        entity.setPrimaryDocument(primary);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        documentRepository.save(entity);

        createDefaultSections(entity, profile);
        if (primary || workspace.getActiveDocumentId() == null) {
            workspace.setActiveDocumentId(entity.getId());
            workspace.setUpdatedAt(now);
            workspaceRepository.save(workspace);
        }
        return toDocumentResponse(entity);
    }

    @Transactional
    public AcademicDocumentResponse ensureLegacyPrimaryDocument(UUID workspaceId) {
        profileService.getOrCreateDefault(workspaceId);
        return documentRepository.findFirstByWorkspaceIdAndPrimaryDocumentTrue(workspaceId)
                .map(this::toDocumentResponse)
                .orElseGet(() -> {
                    WorkspaceEntity workspace = getWorkspace(workspaceId);
                    return create(workspaceId, new CreateAcademicDocumentRequest(
                            workspace.getTitle(),
                            AcademicDocumentType.COURSE_PAPER,
                            null,
                            null,
                            3000,
                            "WORDS",
                            "APA",
                            Map.of("legacyMode", true),
                            true
                    ));
                });
    }

    public List<AcademicDocumentResponse> list(UUID workspaceId) {
        getWorkspace(workspaceId);
        if (!documentRepository.existsByWorkspaceId(workspaceId)) ensureLegacyPrimaryDocument(workspaceId);
        return documentRepository.findByWorkspaceIdOrderByUpdatedAtDesc(workspaceId).stream()
                .map(this::toDocumentResponse)
                .toList();
    }

    public AcademicDocumentResponse get(UUID documentId) {
        return toDocumentResponse(getDocument(documentId));
    }

    @Transactional
    public AcademicDocumentResponse update(UUID documentId, UpdateAcademicDocumentRequest request) {
        AcademicDocumentEntity entity = getDocument(documentId);
        if (request.title() != null && !request.title().isBlank()) entity.setTitle(request.title().trim());
        if (request.status() != null) entity.setStatus(request.status());
        if (request.targetInstitution() != null) entity.setTargetInstitution(trimToNull(request.targetInstitution()));
        if (request.targetVenue() != null) entity.setTargetVenue(trimToNull(request.targetVenue()));
        if (request.targetLength() != null) entity.setTargetLength(request.targetLength());
        if (request.lengthUnit() != null) entity.setLengthUnit(normalizeLengthUnit(request.lengthUnit()));
        if (request.citationStyle() != null && !request.citationStyle().isBlank()) entity.setCitationStyle(request.citationStyle().trim());
        if (request.requirementProfile() != null) entity.setRequirementProfileJson(request.requirementProfile());
        entity.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(entity);
        return toDocumentResponse(entity);
    }

    @Transactional
    public AcademicDocumentResponse activate(UUID documentId) {
        AcademicDocumentEntity entity = getDocument(documentId);
        WorkspaceEntity workspace = getWorkspace(entity.getWorkspaceId());
        workspace.setActiveDocumentId(entity.getId());
        workspace.setUpdatedAt(OffsetDateTime.now());
        workspaceRepository.save(workspace);
        return toDocumentResponse(entity);
    }

    @Transactional
    public DocumentSectionResponse createSection(UUID documentId, CreateDocumentSectionRequest request) {
        AcademicDocumentEntity document = getDocument(documentId);
        if (request.parentSectionId() != null) {
            DocumentSectionEntity parent = getSectionEntity(request.parentSectionId());
            if (!parent.getDocumentId().equals(documentId)) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST_BODY, HttpStatus.BAD_REQUEST.value(), "父章节不属于当前文档");
            }
        }
        DocumentSectionEntity section = new DocumentSectionEntity();
        OffsetDateTime now = OffsetDateTime.now();
        section.setId(UUID.randomUUID());
        section.setDocumentId(documentId);
        section.setParentSectionId(request.parentSectionId());
        section.setSortOrder(request.sortOrder() == null ? sectionRepository.countByDocumentId(documentId) + 1 : request.sortOrder());
        section.setSectionType(request.sectionType().trim().toUpperCase());
        section.setTitle(request.title().trim());
        section.setContent(defaultString(request.content(), ""));
        section.setTargetLength(request.targetLength());
        section.setStatus(section.getContent().isBlank() ? DocumentSectionStatus.EMPTY : DocumentSectionStatus.DRAFTING);
        section.setSourceTraceMapJson(Map.of());
        section.setVersionNo(1);
        section.setCreatedAt(now);
        section.setUpdatedAt(now);
        sectionRepository.save(section);
        saveVersion(section, "USER", "创建章节");
        touchDocument(document);
        return toSectionResponse(section);
    }

    public List<DocumentSectionResponse> listSections(UUID documentId) {
        getDocument(documentId);
        return sectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId).stream()
                .map(this::toSectionResponse)
                .toList();
    }

    @Transactional
    public List<DocumentSectionResponse> reorderSections(
            UUID documentId,
            ReorderDocumentSectionsRequest request
    ) {
        AcademicDocumentEntity document = getDocument(documentId);
        List<DocumentSectionEntity> sections = sectionRepository.findByDocumentIdForUpdate(documentId);
        List<UUID> orderedIds = request.sectionIds();
        if (orderedIds.size() != sections.size()
                || new HashSet<>(orderedIds).size() != orderedIds.size()
                || !new HashSet<>(orderedIds).equals(sections.stream()
                        .map(DocumentSectionEntity::getId)
                        .collect(java.util.stream.Collectors.toSet()))) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST_BODY,
                    HttpStatus.BAD_REQUEST.value(),
                    "章节排序必须包含当前文档的全部章节，且不能重复"
            );
        }

        Map<UUID, DocumentSectionEntity> sectionById = sections.stream()
                .collect(java.util.stream.Collectors.toMap(DocumentSectionEntity::getId, section -> section));
        OffsetDateTime now = OffsetDateTime.now();
        List<DocumentSectionEntity> reordered = new ArrayList<>(orderedIds.size());
        for (int index = 0; index < orderedIds.size(); index += 1) {
            DocumentSectionEntity section = sectionById.get(orderedIds.get(index));
            section.setSortOrder(index + 1);
            section.setUpdatedAt(now);
            reordered.add(section);
        }
        sectionRepository.saveAll(reordered);
        touchDocument(document);
        return reordered.stream().map(this::toSectionResponse).toList();
    }

    @Transactional
    public DocumentSectionResponse updateSection(UUID sectionId, UpdateDocumentSectionRequest request) {
        DocumentSectionEntity section = getSectionEntityForUpdate(sectionId);
        if (request.title() != null && !request.title().isBlank()) section.setTitle(request.title().trim());
        if (request.content() != null) section.setContent(request.content());
        if (request.sortOrder() != null) section.setSortOrder(request.sortOrder());
        if (request.targetLength() != null) section.setTargetLength(request.targetLength());
        if (request.status() != null) section.setStatus(request.status());
        if (request.sourceTraceMap() != null) section.setSourceTraceMapJson(request.sourceTraceMap());
        section.setVersionNo(nextVersionNo(section));
        section.setUpdatedAt(OffsetDateTime.now());
        if (request.status() == null && !section.getContent().isBlank() && section.getStatus() == DocumentSectionStatus.EMPTY) {
            section.setStatus(DocumentSectionStatus.DRAFTING);
        }
        sectionRepository.save(section);
        saveVersion(section, "USER", defaultString(request.changeSummary(), "手动编辑章节"));
        touchDocument(getDocument(section.getDocumentId()));
        return toSectionResponse(section);
    }

    public List<DocumentSectionVersionResponse> listSectionVersions(UUID sectionId) {
        getSectionEntity(sectionId);
        return sectionVersionRepository.findBySectionIdOrderByVersionNoDesc(sectionId).stream()
                .map(this::toVersionResponse)
                .toList();
    }

    @Transactional
    public DocumentSectionResponse restoreSectionVersion(UUID versionId) {
        DocumentSectionVersionEntity version = sectionVersionRepository.findById(versionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "章节版本不存在"));
        DocumentSectionEntity section = getSectionEntityForUpdate(version.getSectionId());
        section.setTitle(version.getTitle());
        section.setContent(version.getContent());
        section.setSourceTraceMapJson(version.getSourceTraceMapJson());
        section.setVersionNo(nextVersionNo(section));
        section.setStatus(section.getContent().isBlank() ? DocumentSectionStatus.EMPTY : DocumentSectionStatus.DRAFTING);
        section.setUpdatedAt(OffsetDateTime.now());
        sectionRepository.save(section);
        saveVersion(section, "RESTORE", "恢复章节版本 v" + version.getVersionNo());
        return toSectionResponse(section);
    }

    @Transactional
    public DocumentMaterialLinkResponse linkMaterial(UUID documentId, DocumentMaterialLinkRequest request) {
        AcademicDocumentEntity document = getDocument(documentId);
        MaterialEntity material = materialRepository.findById(request.materialId())
                .orElseThrow(() -> new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "material 不存在"));
        if (!material.getWorkspaceId().equals(document.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN.value(), "材料不属于当前研究项目");
        }
        DocumentMaterialLinkEntity link = materialLinkRepository.findByDocumentIdAndMaterialId(documentId, material.getId())
                .orElseGet(DocumentMaterialLinkEntity::new);
        OffsetDateTime now = OffsetDateTime.now();
        if (link.getId() == null) {
            link.setId(UUID.randomUUID());
            link.setDocumentId(documentId);
            link.setMaterialId(material.getId());
            link.setCreatedAt(now);
        }
        link.setRole(defaultString(request.role(), "SUPPORTING").toUpperCase());
        link.setIncluded(request.included() == null || request.included());
        link.setUpdatedAt(now);
        materialLinkRepository.save(link);
        return toLinkResponse(link, material);
    }

    public List<DocumentMaterialLinkResponse> listMaterialLinks(UUID documentId) {
        getDocument(documentId);
        return materialLinkRepository.findByDocumentIdOrderByCreatedAtAsc(documentId).stream()
                .map(link -> toLinkResponse(link, materialRepository.findById(link.getMaterialId()).orElse(null)))
                .toList();
    }

    public List<MaterialEntity> resolveMaterials(AcademicDocumentEntity document) {
        if (!materialLinkRepository.existsByDocumentId(document.getId())) {
            return materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(document.getWorkspaceId());
        }
        List<UUID> materialIds = materialLinkRepository.findByDocumentIdAndIncludedTrue(document.getId()).stream()
                .map(DocumentMaterialLinkEntity::getMaterialId)
                .toList();
        return materialIds.isEmpty() ? List.of() : materialRepository.findAllById(materialIds);
    }

    public AssembledDocumentResponse assemble(UUID documentId) {
        AcademicDocumentEntity document = getDocument(documentId);
        List<DocumentSectionEntity> sections = sectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId);
        StringBuilder content = new StringBuilder();
        Map<String, Object> trace = new LinkedHashMap<>();
        List<UUID> sectionIds = new ArrayList<>();
        for (DocumentSectionEntity section : sections) {
            sectionIds.add(section.getId());
            if (!content.isEmpty()) content.append("\n\n");
            content.append("# ").append(section.getTitle()).append("\n\n").append(section.getContent());
            trace.put(section.getId().toString(), section.getSourceTraceMapJson() == null ? Map.of() : section.getSourceTraceMapJson());
        }
        return new AssembledDocumentResponse(
                document.getId(),
                document.getTitle(),
                content.toString(),
                content.length(),
                sectionIds,
                trace
        );
    }

    AcademicDocumentEntity getDocument(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "学术文档不存在"));
    }

    DocumentSectionEntity getSectionEntity(UUID sectionId) {
        return sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "文档章节不存在"));
    }

    DocumentSectionEntity getSectionEntityForUpdate(UUID sectionId) {
        return sectionRepository.findByIdForUpdate(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "文档章节不存在"));
    }

    int currentSectionVersion(DocumentSectionEntity section) {
        int entityVersion = section.getVersionNo() == null ? 0 : section.getVersionNo();
        int historyVersion = sectionVersionRepository.findFirstBySectionIdOrderByVersionNoDesc(section.getId())
                .map(DocumentSectionVersionEntity::getVersionNo)
                .orElse(0);
        return Math.max(entityVersion, historyVersion);
    }

    @Transactional
    public DocumentSectionResponse applyAiSectionContent(
            UUID sectionId,
            int expectedVersionNo,
            String content,
            Map<String, Object> sourceTraceMap,
            String changeSource,
            String changeSummary
    ) {
        DocumentSectionEntity section = getSectionEntityForUpdate(sectionId);
        int currentVersionNo = currentSectionVersion(section);
        if (currentVersionNo != expectedVersionNo) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_STATUS_CONFLICT,
                    HttpStatus.CONFLICT.value(),
                    "章节在 AI 处理期间已被修改，请刷新后重新生成或共写"
            );
        }
        section.setContent(content == null ? "" : content);
        section.setSourceTraceMapJson(sourceTraceMap == null ? Map.of() : sourceTraceMap);
        section.setStatus(section.getContent().isBlank() ? DocumentSectionStatus.EMPTY : DocumentSectionStatus.DRAFTING);
        section.setVersionNo(currentVersionNo + 1);
        section.setUpdatedAt(OffsetDateTime.now());
        sectionRepository.save(section);
        saveVersion(section, changeSource, changeSummary);
        touchDocument(getDocument(section.getDocumentId()));
        return toSectionResponse(section);
    }

    DocumentSectionResponse toSectionResponse(DocumentSectionEntity section) {
        return new DocumentSectionResponse(
                section.getId(), section.getDocumentId(), section.getParentSectionId(), section.getSortOrder(),
                section.getSectionType(), section.getTitle(), section.getContent(), section.getTargetLength(),
                section.getStatus(), section.getSourceTraceMapJson() == null ? Map.of() : section.getSourceTraceMapJson(), section.getVersionNo(),
                section.getCreatedAt(), section.getUpdatedAt()
        );
    }

    private void createDefaultSections(AcademicDocumentEntity document, AcademicProjectProfileEntity profile) {
        for (AcademicRuleCatalog.SectionTemplate template : ruleCatalog.defaultSections(document.getDocumentType(), profile.getResearchParadigm())) {
            createSection(document.getId(), new CreateDocumentSectionRequest(
                    null, template.sortOrder(), template.sectionType(), template.title(), "", template.targetLength()
            ));
        }
        document.setStatus(AcademicDocumentStatus.PLANNING);
        document.setUpdatedAt(OffsetDateTime.now());
        documentRepository.save(document);
    }

    private AcademicDocumentResponse toDocumentResponse(AcademicDocumentEntity entity) {
        return new AcademicDocumentResponse(
                entity.getId(), entity.getWorkspaceId(), entity.getDocumentType(), entity.getTitle(), entity.getStatus(),
                entity.getTargetInstitution(), entity.getTargetVenue(), entity.getTargetLength(), entity.getLengthUnit(),
                entity.getCitationStyle(), entity.getRequirementProfileJson() == null ? Map.of() : entity.getRequirementProfileJson(), entity.isPrimaryDocument(),
                sectionRepository.countByDocumentId(entity.getId()), entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }

    private DocumentSectionVersionResponse toVersionResponse(DocumentSectionVersionEntity entity) {
        return new DocumentSectionVersionResponse(
                entity.getId(), entity.getSectionId(), entity.getVersionNo(), entity.getTitle(), entity.getContent(),
                entity.getSourceTraceMapJson() == null ? Map.of() : entity.getSourceTraceMapJson(), entity.getChangeSource(), entity.getChangeSummary(), entity.getCreatedAt()
        );
    }

    private DocumentMaterialLinkResponse toLinkResponse(DocumentMaterialLinkEntity link, MaterialEntity material) {
        return new DocumentMaterialLinkResponse(
                link.getId(), link.getDocumentId(), link.getMaterialId(), material == null ? "材料已删除" : material.getFilename(),
                link.getRole(), link.isIncluded(), link.getUpdatedAt()
        );
    }

    private void saveVersion(DocumentSectionEntity section, String changeSource, String changeSummary) {
        DocumentSectionVersionEntity version = new DocumentSectionVersionEntity();
        version.setId(UUID.randomUUID());
        version.setSectionId(section.getId());
        version.setVersionNo(section.getVersionNo());
        version.setTitle(section.getTitle());
        version.setContent(section.getContent());
        version.setSourceTraceMapJson(section.getSourceTraceMapJson() == null
                ? Map.of()
                : new LinkedHashMap<>(section.getSourceTraceMapJson()));
        version.setChangeSource(changeSource);
        version.setChangeSummary(changeSummary);
        version.setCreatedAt(OffsetDateTime.now());
        sectionVersionRepository.save(version);
    }

    private int nextVersionNo(DocumentSectionEntity section) {
        return currentSectionVersion(section) + 1;
    }

    private void touchDocument(AcademicDocumentEntity document) {
        document.setUpdatedAt(OffsetDateTime.now());
        if (document.getStatus() == AcademicDocumentStatus.PLANNING) document.setStatus(AcademicDocumentStatus.WRITING);
        documentRepository.save(document);
    }

    private WorkspaceEntity getWorkspace(UUID workspaceId) {
        return workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在"));
    }

    private String normalizeLengthUnit(String value) {
        String normalized = defaultString(value, "WORDS").toUpperCase();
        return "CHARACTERS".equals(normalized) ? normalized : "WORDS";
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

}
