package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.DocumentReadinessResponse;
import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.GenerateSectionRequest;
import com.aipm.cowriting.application.dto.academic.ApplySectionCoWritePreviewRequest;
import com.aipm.cowriting.application.dto.academic.SectionCoWritePreviewResponse;
import com.aipm.cowriting.application.dto.academic.SectionCoWriteRequest;
import com.aipm.cowriting.application.dto.ai.CoWriteResult;
import com.aipm.cowriting.application.dto.ai.DraftGenerationResult;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import com.aipm.cowriting.domain.entity.AiActionLogEntity;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.entity.SectionCoWritePreviewEntity;
import com.aipm.cowriting.domain.entity.SectionCoWritePreviewReviewLinkEntity;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.SectionCoWritePreviewRepository;
import com.aipm.cowriting.domain.repository.SectionCoWritePreviewReviewLinkRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcademicSectionApplicationService {

    private final AcademicDocumentApplicationService documentService;
    private final AcademicProfileApplicationService profileService;
    private final AcademicReadinessApplicationService readinessService;
    private final SectionCoWritePreviewRepository previewRepository;
    private final AiSemanticParseResultRepository parseResultRepository;
    private final RequirementSnapshotRepository requirementSnapshotRepository;
    private final OpenAiDraftGenerationService draftGenerationService;
    private final OpenAiCoWriteService coWriteService;
    private final AiActionLogApplicationService actionLogService;
    private final AcademicReviewApplicationService reviewService;
    private final SectionCoWritePreviewReviewLinkRepository previewReviewLinkRepository;
    private final ObjectMapper objectMapper;

    public AcademicSectionApplicationService(
            AcademicDocumentApplicationService documentService,
            AcademicProfileApplicationService profileService,
            AcademicReadinessApplicationService readinessService,
            SectionCoWritePreviewRepository previewRepository,
            AiSemanticParseResultRepository parseResultRepository,
            RequirementSnapshotRepository requirementSnapshotRepository,
            OpenAiDraftGenerationService draftGenerationService,
            OpenAiCoWriteService coWriteService,
            AiActionLogApplicationService actionLogService,
            AcademicReviewApplicationService reviewService,
            SectionCoWritePreviewReviewLinkRepository previewReviewLinkRepository,
            ObjectMapper objectMapper
    ) {
        this.documentService = documentService;
        this.profileService = profileService;
        this.readinessService = readinessService;
        this.previewRepository = previewRepository;
        this.parseResultRepository = parseResultRepository;
        this.requirementSnapshotRepository = requirementSnapshotRepository;
        this.draftGenerationService = draftGenerationService;
        this.coWriteService = coWriteService;
        this.actionLogService = actionLogService;
        this.reviewService = reviewService;
        this.previewReviewLinkRepository = previewReviewLinkRepository;
        this.objectMapper = objectMapper;
    }

    public DocumentSectionResponse generate(UUID sectionId, GenerateSectionRequest request) {
        DocumentSectionEntity section = documentService.getSectionEntity(sectionId);
        int baseVersionNo = documentService.currentSectionVersion(section);
        AcademicDocumentEntity document = documentService.getDocument(section.getDocumentId());
        AcademicProjectProfileEntity profile = profileService.getEntity(document.getWorkspaceId());
        assertDraftingAllowed(profile);
        DocumentReadinessResponse readiness = readinessService.checkSection(sectionId);
        if (!readiness.generationEligible()) {
            throw new BusinessException(
                    ErrorCode.GENERATION_NOT_ELIGIBLE,
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    "当前章节缺少可靠材料，暂时无法调用 AI 生成",
                    Map.of("issues", readiness.issues())
            );
        }

        List<MaterialEntity> materials = parsedMaterials(document);
        List<Map<String, Object>> materialContext = materialContext(materials);
        Map<String, Object> requirementContext = academicContext(document, section, profile, request == null ? null : request.instruction());
        DraftGenerationResult result = draftGenerationService.generate(
                requirementContext,
                materialContext,
                request == null ? "stable" : request.mode()
        );

        DocumentSectionResponse response = documentService.applyAiSectionContent(
                sectionId,
                baseVersionNo,
                result.draftText(),
                result.sourceTraceMap(),
                "AI_GENERATE",
                "AI 基于已解析材料生成章节草稿"
        );
        actionLogService.create(
                document.getWorkspaceId(), document.getId(), section.getId(), "SECTION_GENERATE",
                materials.stream().map(MaterialEntity::getId).toList(),
                summarize(request == null ? null : request.instruction(), 300),
                summarize(result.draftText(), 500),
                disclosureRequired(profile)
        );
        return response;
    }

    @Transactional
    public SectionCoWritePreviewResponse previewCoWrite(UUID sectionId, SectionCoWriteRequest request) {
        DocumentSectionEntity section = documentService.getSectionEntity(sectionId);
        int baseVersionNo = documentService.currentSectionVersion(section);
        AcademicDocumentEntity document = documentService.getDocument(section.getDocumentId());
        AcademicProjectProfileEntity profile = profileService.getEntity(document.getWorkspaceId());
        assertDraftingAllowed(profile);
        List<MaterialEntity> materials = parsedMaterials(document);

        Map<String, Object> outline = new LinkedHashMap<>();
        outline.put("academicProfile", profileContext(profile));
        outline.put("document", documentContext(document));
        outline.put("section", sectionContext(section));
        outline.put("confirmedRequirements", confirmedRequirements(document));
        outline.put("selectedMaterials", materialContext(materials));
        Map<String, Object> targetRange = normalizeTargetRange(request.targetRange(), section.getContent());
        CoWriteResult result = coWriteService.coWrite(
                request.action(),
                request.instruction(),
                request.controls() == null ? Map.of() : request.controls(),
                targetRange,
                section.getTitle(),
                section.getContent(),
                outline,
                section.getSourceTraceMapJson() == null ? Map.of() : section.getSourceTraceMapJson()
        );

        AiActionLogEntity log = actionLogService.create(
                document.getWorkspaceId(), document.getId(), section.getId(), "SECTION_COWRITE_PREVIEW",
                materials.stream().map(MaterialEntity::getId).toList(),
                summarize(request.instruction(), 300),
                summarize(result.draftText(), 500),
                disclosureRequired(profile)
        );
        SectionCoWritePreviewEntity preview = new SectionCoWritePreviewEntity();
        preview.setId(UUID.randomUUID());
        preview.setSectionId(sectionId);
        preview.setBaseVersionNo(baseVersionNo);
        preview.setAction(request.action());
        preview.setInstruction(request.instruction());
        preview.setControlsJson(request.controls() == null ? Map.of() : request.controls());
        preview.setBaseContent(section.getContent());
        preview.setTargetRangeJson(targetRange);
        preview.setCandidateContent(result.draftText());
        preview.setCandidateSourceTraceMapJson(result.sourceTraceMap() == null ? Map.of() : result.sourceTraceMap());
        List<Map<String, Object>> diffRows = buildDiffRows(section.getContent(), result.draftText());
        List<Map<String, Object>> paragraphRows = buildParagraphRows(section.getContent(), result.draftText());
        preview.setDiffRowsJson(diffRows);
        preview.setParagraphDiffRowsJson(paragraphRows);
        preview.setDiffSummaryJson(diffSummary(section.getContent(), result.draftText(), targetRange));
        preview.setStatus("READY");
        preview.setAiActionLogId(log.getId());
        preview.setCreatedAt(OffsetDateTime.now());
        previewRepository.save(preview);
        linkRelatedReviews(preview, targetRange);
        return toPreviewResponse(preview);
    }

    @Transactional
    public DocumentSectionResponse applyPreview(UUID previewId) {
        return applyPreview(previewId, null);
    }

    @Transactional
    public DocumentSectionResponse applyPreview(UUID previewId, ApplySectionCoWritePreviewRequest request) {
        SectionCoWritePreviewEntity preview = getPreview(previewId);
        if (!"READY".equals(preview.getStatus())) {
            throw new BusinessException(ErrorCode.WORKSPACE_STATUS_CONFLICT, HttpStatus.CONFLICT.value(), "该章节共写预览已处理");
        }
        String mode = request == null ? "ALL" : request.effectiveMode();
        Set<String> selectedIds = request == null || request.selectedIds() == null
                ? Set.of()
                : new LinkedHashSet<>(request.selectedIds());
        String content = switch (mode) {
            case "PARAGRAPHS" -> applyRows(preview.getParagraphDiffRowsJson(), selectedIds, "\n\n");
            case "DIFF_ROWS" -> applyRows(preview.getDiffRowsJson(), selectedIds, "");
            default -> preview.getCandidateContent();
        };
        DocumentSectionResponse response = documentService.applyAiSectionContent(
                preview.getSectionId(),
                preview.getBaseVersionNo(),
                content,
                preview.getCandidateSourceTraceMapJson(),
                "AI_COWRITE",
                "应用章节共写预览"
        );
        preview.setStatus("APPLIED");
        preview.setAppliedAt(OffsetDateTime.now());
        previewRepository.save(preview);
        actionLogService.markAccepted(preview.getAiActionLogId(), true);
        List<UUID> relatedReviewIds = previewReviewLinkRepository
                .findBySectionCowritePreviewIdOrderByCreatedAtAsc(previewId).stream()
                .map(SectionCoWritePreviewReviewLinkEntity::getReviewItemId)
                .toList();
        reviewService.markPendingRecheck(relatedReviewIds);
        return response;
    }

    @Transactional
    public SectionCoWritePreviewResponse discardPreview(UUID previewId) {
        SectionCoWritePreviewEntity preview = getPreview(previewId);
        if ("READY".equals(preview.getStatus())) {
            preview.setStatus("DISCARDED");
            previewRepository.save(preview);
            actionLogService.markAccepted(preview.getAiActionLogId(), false);
        }
        return toPreviewResponse(preview);
    }

    private List<MaterialEntity> parsedMaterials(AcademicDocumentEntity document) {
        return documentService.resolveMaterials(document).stream()
                .filter(material -> material.getParseStage() == ParseStage.AI_PARSED)
                .toList();
    }

    private List<Map<String, Object>> materialContext(List<MaterialEntity> materials) {
        if (materials.isEmpty()) return List.of();
        Map<UUID, AiSemanticParseResultEntity> parseByMaterial = parseResultRepository
                .findByMaterialIdIn(materials.stream().map(MaterialEntity::getId).toList()).stream()
                .collect(java.util.stream.Collectors.toMap(AiSemanticParseResultEntity::getMaterialId, item -> item, (left, right) -> left));
        return materials.stream().map(material -> {
            AiSemanticParseResultEntity parse = parseByMaterial.get(material.getId());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("materialId", material.getId().toString());
            item.put("filename", material.getFilename());
            item.put("materialCategory", parse == null || parse.getMaterialCategory() == null ? "UNKNOWN" : parse.getMaterialCategory().name());
            item.put("materialRole", parse == null || parse.getMaterialRole() == null ? "UNKNOWN" : parse.getMaterialRole().name());
            item.put("researchArtifactType", parse == null || parse.getResearchArtifactType() == null ? "NONE" : parse.getResearchArtifactType().name());
            item.put("materialTags", parse == null ? List.of() : readList(parse.getMaterialTagsJson()));
            item.put("summary", parse == null ? "" : defaultString(parse.getSummary(), ""));
            item.put("topicRelation", parse == null ? "" : defaultString(parse.getTopicRelation(), ""));
            item.put("detectedClaims", parse == null ? List.of() : readList(parse.getDetectedClaimsJson()));
            item.put("detectedEvidence", parse == null ? List.of() : readList(parse.getDetectedEvidenceJson()));
            item.put("bibliographicMetadata", parse == null ? Map.of() : readMap(parse.getBibliographicMetadataJson()));
            return item;
        }).toList();
    }

    private Map<String, Object> academicContext(
            AcademicDocumentEntity document,
            DocumentSectionEntity section,
            AcademicProjectProfileEntity profile,
            String instruction
    ) {
        Map<String, Object> context = new LinkedHashMap<>();
        context.put("generationScope", "SECTION_ONLY");
        context.put("academicProfile", profileContext(profile));
        context.put("document", documentContext(document));
        context.put("section", sectionContext(section));
        context.put("confirmedRequirements", confirmedRequirements(document));
        context.put("userInstruction", defaultString(instruction, ""));
        context.put("requirementPriority", List.of(
                "confirmed institution/supervisor/course/journal requirements",
                "document settings",
                "research paradigm rules",
                "platform defaults"
        ));
        return context;
    }

    private Map<String, Object> profileContext(AcademicProjectProfileEntity profile) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("academicStage", profile.getAcademicStage().name());
        result.put("disciplineGroup", profile.getDisciplineGroup().name());
        result.put("researchParadigm", profile.getResearchParadigm().name());
        result.put("primaryLanguage", profile.getPrimaryLanguage());
        result.put("institution", profile.getInstitution());
        result.put("aiUsagePolicy", profile.getAiUsagePolicy().name());
        return result;
    }

    private Map<String, Object> documentContext(AcademicDocumentEntity document) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("documentId", document.getId().toString());
        result.put("documentType", document.getDocumentType().name());
        result.put("title", document.getTitle());
        result.put("targetInstitution", document.getTargetInstitution());
        result.put("targetVenue", document.getTargetVenue());
        result.put("targetLength", document.getTargetLength());
        result.put("lengthUnit", document.getLengthUnit());
        result.put("citationStyle", document.getCitationStyle());
        result.put("documentRequirements", document.getRequirementProfileJson() == null ? Map.of() : document.getRequirementProfileJson());
        return result;
    }

    private Map<String, Object> sectionContext(DocumentSectionEntity section) {
        return Map.of(
                "sectionId", section.getId().toString(),
                "sectionType", section.getSectionType(),
                "title", section.getTitle(),
                "targetLength", section.getTargetLength() == null ? 0 : section.getTargetLength(),
                "currentContent", defaultString(section.getContent(), "")
        );
    }

    private Map<String, Object> confirmedRequirements(AcademicDocumentEntity document) {
        Map<String, Object> result = new LinkedHashMap<>();
        requirementSnapshotRepository.findFirstByWorkspaceIdAndDocumentIdOrderByVersionDesc(document.getWorkspaceId(), document.getId())
                .ifPresent(snapshot -> result.put("documentSnapshot", requirementSnapshot(snapshot)));
        requirementSnapshotRepository.findFirstByWorkspaceIdAndDocumentIdIsNullOrderByVersionDesc(document.getWorkspaceId())
                .ifPresent(snapshot -> result.put("projectSnapshot", requirementSnapshot(snapshot)));
        result.put("documentProfile", document.getRequirementProfileJson() == null ? Map.of() : document.getRequirementProfileJson());
        return result;
    }

    private Map<String, Object> requirementSnapshot(RequirementSnapshotEntity snapshot) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("topic", snapshot.getTopic());
        result.put("wordCount", snapshot.getWordCount());
        result.put("deadline", snapshot.getDeadline() == null ? null : snapshot.getDeadline().toString());
        result.put("citationStyle", snapshot.getCitationStyle());
        result.put("specialRequirements", readMap(snapshot.getSpecialRequirementsJson()));
        return result;
    }

    private boolean disclosureRequired(AcademicProjectProfileEntity profile) {
        Object configured = profile.getAiPolicyJson() == null ? null : profile.getAiPolicyJson().get("disclosureRequired");
        return configured == null || Boolean.parseBoolean(String.valueOf(configured));
    }

    private void assertDraftingAllowed(AcademicProjectProfileEntity profile) {
        if (profile.getAiUsagePolicy() == AiUsagePolicy.GUIDANCE_ONLY) {
            throw new BusinessException(
                    ErrorCode.FORBIDDEN,
                    HttpStatus.FORBIDDEN.value(),
                    "当前项目的 AI 使用策略仅允许研究指导与审查，不允许直接生成正文"
            );
        }
    }

    private Map<String, Object> diffSummary(String current, String candidate, Map<String, Object> targetRange) {
        String before = defaultString(current, "");
        String after = defaultString(candidate, "");
        List<Map<String, Object>> warnings = new ArrayList<>();
        if (countPattern(before, Pattern.compile("\\d+(?:\\.\\d+)?")) != countPattern(after, Pattern.compile("\\d+(?:\\.\\d+)?"))) {
            warnings.add(Map.of("code", "DATA_CHANGED", "level", "HIGH", "title", "数据数量发生变化", "message", "请核对数字、比例、样本量或年份是否被误改。"));
        }
        if (countPattern(before, Pattern.compile("([（(][^）)]{1,80}[，,]\\s*(?:19|20)\\d{2}[）)])|(\\[\\d{1,3}])"))
                != countPattern(after, Pattern.compile("([（(][^）)]{1,80}[，,]\\s*(?:19|20)\\d{2}[）)])|(\\[\\d{1,3}])"))) {
            warnings.add(Map.of("code", "CITATION_CHANGED", "level", "HIGH", "title", "引用数量发生变化", "message", "请确认正文引用没有被删除、替换或无依据新增。"));
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("changed", !before.equals(after));
        result.put("currentLength", before.length());
        result.put("candidateLength", after.length());
        result.put("characterDelta", after.length() - before.length());
        result.put("targetRange", targetRange);
        result.put("conflictWarnings", warnings);
        result.put("recheckSuggestion", Map.of("shouldRecheck", !warnings.isEmpty(), "message", warnings.isEmpty()
                ? "当前未发现明显数据或引用冲突。"
                : "应用后建议复查相关审查项。"));
        return result;
    }

    private SectionCoWritePreviewEntity getPreview(UUID previewId) {
        return previewRepository.findById(previewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "章节共写预览不存在"));
    }

    private SectionCoWritePreviewResponse toPreviewResponse(SectionCoWritePreviewEntity entity) {
        List<UUID> reviewIds = previewReviewLinkRepository.findBySectionCowritePreviewIdOrderByCreatedAtAsc(entity.getId()).stream()
                .map(SectionCoWritePreviewReviewLinkEntity::getReviewItemId)
                .toList();
        return new SectionCoWritePreviewResponse(
                entity.getId(), entity.getSectionId(), entity.getBaseVersionNo(), entity.getAction(), entity.getInstruction(),
                entity.getControlsJson() == null ? Map.of() : entity.getControlsJson(),
                entity.getCandidateContent(),
                entity.getCandidateSourceTraceMapJson() == null ? Map.of() : entity.getCandidateSourceTraceMapJson(),
                entity.getDiffSummaryJson() == null ? Map.of() : entity.getDiffSummaryJson(),
                entity.getStatus(), entity.getCreatedAt(), entity.getAppliedAt(),
                entity.getBaseContent(),
                entity.getTargetRangeJson() == null ? Map.of() : entity.getTargetRangeJson(),
                entity.getDiffRowsJson() == null ? List.of() : entity.getDiffRowsJson(),
                entity.getParagraphDiffRowsJson() == null ? List.of() : entity.getParagraphDiffRowsJson(),
                reviewIds
        );
    }

    private void linkRelatedReviews(SectionCoWritePreviewEntity preview, Map<String, Object> targetRange) {
        List<ReviewItemEntity> reviews = reviewService.relatedOpenItems(preview.getSectionId(), targetRange);
        OffsetDateTime now = OffsetDateTime.now();
        List<SectionCoWritePreviewReviewLinkEntity> links = reviews.stream().map(review -> {
            SectionCoWritePreviewReviewLinkEntity link = new SectionCoWritePreviewReviewLinkEntity();
            link.setId(UUID.randomUUID());
            link.setSectionCowritePreviewId(preview.getId());
            link.setReviewItemId(review.getId());
            link.setRelationType("OVERLAPS");
            link.setRelationReason("共写范围与该审查项定位范围重叠");
            link.setRecheckPrompted(false);
            link.setCreatedAt(now);
            return link;
        }).toList();
        previewReviewLinkRepository.saveAll(links);
    }

    private Map<String, Object> normalizeTargetRange(Map<String, Object> requested, String content) {
        String text = content == null ? "" : content;
        if (requested == null || requested.isEmpty() || !"selection".equalsIgnoreCase(String.valueOf(requested.get("mode")))) {
            return Map.of("mode", "full_draft", "scope", "single_section", "start", 0, "end", text.length());
        }
        int start = Math.max(0, integer(requested.get("start"), 0));
        int end = Math.min(text.length(), Math.max(start, integer(requested.get("end"), text.length())));
        if (start == end) return Map.of("mode", "full_draft", "scope", "single_section", "start", 0, "end", text.length());
        return Map.of("mode", "selection", "scope", "single_section", "start", start, "end", end, "selectedText", text.substring(start, end));
    }

    private List<Map<String, Object>> buildParagraphRows(String current, String candidate) {
        return buildRows(splitParagraphs(current), splitParagraphs(candidate), "p", "段落修改");
    }

    private List<Map<String, Object>> buildDiffRows(String current, String candidate) {
        return buildRows(splitSentences(current), splitSentences(candidate), "s", "局部修改");
    }

    private List<Map<String, Object>> buildRows(List<String> current, List<String> candidate, String prefix, String intent) {
        List<Map<String, Object>> rows = new ArrayList<>();
        int total = Math.max(current.size(), candidate.size());
        for (int index = 0; index < total; index++) {
            String original = index < current.size() ? current.get(index) : "";
            String replacement = index < candidate.size() ? candidate.get(index) : "";
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", prefix + (index + 1));
            row.put("originalText", original);
            row.put("candidateText", replacement);
            row.put("changed", !original.equals(replacement));
            row.put("intentLabel", intent);
            rows.add(row);
        }
        return rows;
    }

    private String applyRows(List<Map<String, Object>> rows, Set<String> selectedIds, String separator) {
        if (selectedIds.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_BODY, HttpStatus.BAD_REQUEST.value(), "请至少选择一项需要应用的修改");
        }
        List<String> values = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String id = String.valueOf(row.get("id"));
            String value = String.valueOf(row.getOrDefault(selectedIds.contains(id) ? "candidateText" : "originalText", ""));
            if (!value.isBlank()) values.add(value);
        }
        return String.join(separator, values);
    }

    private List<String> splitParagraphs(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Pattern.compile("\\n\\s*\\n").splitAsStream(text).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private List<String> splitSentences(String text) {
        if (text == null || text.isBlank()) return List.of();
        return Pattern.compile("(?<=[。！？.!?])").splitAsStream(text).map(String::trim).filter(value -> !value.isBlank()).toList();
    }

    private int countPattern(String text, Pattern pattern) {
        int count = 0;
        var matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) count++;
        return count;
    }

    private int integer(Object value, int fallback) {
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); } catch (RuntimeException error) { return fallback; }
    }

    private String summarize(String value, int maxLength) {
        String normalized = defaultString(value, "").replaceAll("\\s+", " ");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength) + "...";
    }

    private String defaultString(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(json, Map.class);
        } catch (JsonProcessingException e) {
            return Map.of();
        }
    }

    private List<?> readList(String json) {
        if (json == null || json.isBlank()) return List.of();
        try {
            return objectMapper.readValue(json, List.class);
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }
}
