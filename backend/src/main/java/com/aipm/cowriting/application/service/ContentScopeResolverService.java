package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.model.ContentScope;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.AcademicProjectProfileRepository;
import com.aipm.cowriting.domain.repository.DocumentMaterialLinkRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class ContentScopeResolverService {

    private final DraftVersionRepository draftVersionRepository;
    private final AcademicDocumentRepository documentRepository;
    private final DocumentSectionRepository sectionRepository;
    private final DocumentMaterialLinkRepository materialLinkRepository;
    private final MaterialRepository materialRepository;
    private final RequirementSnapshotRepository requirementSnapshotRepository;
    private final AcademicProjectProfileRepository profileRepository;
    private final ObjectMapper objectMapper;

    public ContentScopeResolverService(
            DraftVersionRepository draftVersionRepository,
            AcademicDocumentRepository documentRepository,
            DocumentSectionRepository sectionRepository,
            DocumentMaterialLinkRepository materialLinkRepository,
            MaterialRepository materialRepository,
            RequirementSnapshotRepository requirementSnapshotRepository,
            AcademicProjectProfileRepository profileRepository,
            ObjectMapper objectMapper
    ) {
        this.draftVersionRepository = draftVersionRepository;
        this.documentRepository = documentRepository;
        this.sectionRepository = sectionRepository;
        this.materialLinkRepository = materialLinkRepository;
        this.materialRepository = materialRepository;
        this.requirementSnapshotRepository = requirementSnapshotRepository;
        this.profileRepository = profileRepository;
        this.objectMapper = objectMapper;
    }

    public ContentScope draft(UUID draftId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return draft(draft);
    }

    public ContentScope draft(DraftVersionEntity draft) {
        return new ContentScope(
                "LEGACY_DRAFT",
                draft.getWorkspaceId(),
                draft.getDocumentId(),
                null,
                draft.getId(),
                draft.getVersionNo(),
                draft.getTitleSuggestion(),
                defaultString(draft.getDraftText()),
                readMap(draft.getSourceTraceMapJson()),
                resolveCitationStyle(draft.getWorkspaceId(), draft.getDocumentId()),
                requirements(draft.getWorkspaceId(), draft.getDocumentId()),
                materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(draft.getWorkspaceId()).stream()
                        .map(item -> item.getId())
                        .toList()
        );
    }

    public ContentScope section(UUID sectionId) {
        DocumentSectionEntity section = sectionRepository.findById(sectionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "文档章节不存在"));
        AcademicDocumentEntity document = documentEntity(section.getDocumentId());
        return section(section, document, requirements(document.getWorkspaceId(), document.getId()), resolveMaterialIds(document));
    }

    public List<ContentScope> documentSections(UUID documentId) {
        AcademicDocumentEntity document = documentEntity(documentId);
        Map<String, Object> requirementContext = requirements(document.getWorkspaceId(), document.getId());
        List<UUID> materialIds = resolveMaterialIds(document);
        return sectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId).stream()
                .map(section -> section(section, document, requirementContext, materialIds))
                .toList();
    }

    public ContentScope document(UUID documentId) {
        AcademicDocumentEntity document = documentEntity(documentId);
        List<DocumentSectionEntity> sections = sectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId);
        StringBuilder content = new StringBuilder();
        Map<String, Object> trace = new LinkedHashMap<>();
        for (DocumentSectionEntity section : sections) {
            if (!content.isEmpty()) content.append("\n\n");
            content.append("# ").append(section.getTitle()).append("\n\n").append(defaultString(section.getContent()));
            trace.put(section.getId().toString(), section.getSourceTraceMapJson() == null ? Map.of() : section.getSourceTraceMapJson());
        }
        int revision = sections.stream().map(DocumentSectionEntity::getVersionNo).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
        return new ContentScope(
                "DOCUMENT",
                document.getWorkspaceId(),
                document.getId(),
                null,
                null,
                revision,
                document.getTitle(),
                content.toString(),
                trace,
                document.getCitationStyle(),
                requirements(document.getWorkspaceId(), document.getId()),
                resolveMaterialIds(document)
        );
    }

    private ContentScope section(
            DocumentSectionEntity section,
            AcademicDocumentEntity document,
            Map<String, Object> requirementContext,
            List<UUID> materialIds
    ) {
        return new ContentScope(
                "SECTION",
                document.getWorkspaceId(),
                document.getId(),
                section.getId(),
                null,
                section.getVersionNo(),
                section.getTitle(),
                defaultString(section.getContent()),
                section.getSourceTraceMapJson() == null ? Map.of() : section.getSourceTraceMapJson(),
                document.getCitationStyle(),
                requirementContext,
                materialIds
        );
    }

    private AcademicDocumentEntity documentEntity(UUID documentId) {
        return documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "学术文档不存在"));
    }

    private List<UUID> resolveMaterialIds(AcademicDocumentEntity document) {
        if (!materialLinkRepository.existsByDocumentId(document.getId())) {
            return materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(document.getWorkspaceId()).stream()
                    .map(item -> item.getId())
                    .toList();
        }
        return materialLinkRepository.findByDocumentIdAndIncludedTrue(document.getId()).stream()
                .map(item -> item.getMaterialId())
                .toList();
    }

    private String resolveCitationStyle(UUID workspaceId, UUID documentId) {
        if (documentId != null) {
            return documentRepository.findById(documentId).map(AcademicDocumentEntity::getCitationStyle).orElse("APA");
        }
        return requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId)
                .map(RequirementSnapshotEntity::getCitationStyle)
                .filter(value -> value != null && !value.isBlank())
                .orElse("APA");
    }

    private Map<String, Object> requirements(UUID workspaceId, UUID documentId) {
        Map<String, Object> result = new LinkedHashMap<>();
        profileRepository.findById(workspaceId).ifPresent(profile -> result.put("academicProfile", profile(profile)));
        if (documentId != null) {
            documentRepository.findById(documentId).ifPresent(document -> result.put("documentProfile", Map.of(
                    "documentType", document.getDocumentType().name(),
                    "targetLength", document.getTargetLength() == null ? 0 : document.getTargetLength(),
                    "citationStyle", document.getCitationStyle(),
                    "requirements", document.getRequirementProfileJson() == null ? Map.of() : document.getRequirementProfileJson()
            )));
            requirementSnapshotRepository.findFirstByWorkspaceIdAndDocumentIdOrderByVersionDesc(workspaceId, documentId)
                    .ifPresent(snapshot -> result.put("documentSnapshot", snapshot(snapshot)));
        }
        requirementSnapshotRepository.findFirstByWorkspaceIdAndDocumentIdIsNullOrderByVersionDesc(workspaceId)
                .or(() -> requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId))
                .ifPresent(snapshot -> result.put("projectSnapshot", snapshot(snapshot)));
        result.put("priority", List.of(
                "用户确认的学校、导师、课程或期刊要求",
                "当前文档设置",
                "研究范式规则",
                "平台默认规则"
        ));
        return result;
    }

    private Map<String, Object> profile(AcademicProjectProfileEntity profile) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("academicStage", profile.getAcademicStage().name());
        value.put("disciplineGroup", profile.getDisciplineGroup().name());
        value.put("researchParadigm", profile.getResearchParadigm().name());
        value.put("primaryLanguage", profile.getPrimaryLanguage());
        value.put("institution", profile.getInstitution());
        return value;
    }

    private Map<String, Object> snapshot(RequirementSnapshotEntity snapshot) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("topic", snapshot.getTopic());
        value.put("wordCount", snapshot.getWordCount());
        value.put("deadline", snapshot.getDeadline() == null ? null : snapshot.getDeadline().toString());
        value.put("citationStyle", snapshot.getCitationStyle());
        value.put("specialRequirements", readMap(snapshot.getSpecialRequirementsJson()));
        return value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException ignored) {
            return Map.of();
        }
    }

    private String defaultString(String value) {
        return value == null ? "" : value;
    }
}
