package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.evidence.CitationConsistencyReport;
import com.aipm.cowriting.application.dto.evidence.DocumentEvidenceSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingItemResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceCoverageReport;
import com.aipm.cowriting.application.dto.evidence.EvidenceMaterialResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceParagraphResponse;
import com.aipm.cowriting.application.model.ContentScope;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.EvidenceBindingEntity;
import com.aipm.cowriting.domain.entity.KnowledgeChunkEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.EvidenceBindingRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScopedEvidenceBindingApplicationService {

    private static final Pattern APA_CITATION_PATTERN = Pattern.compile("[（(][^）)]{1,80}[，,]\\s*((19|20)\\d{2}|n\\.d\\.)[）)]");
    private static final Pattern GBT_CITATION_PATTERN = Pattern.compile("\\[(\\d{1,3})]");

    private final ContentScopeResolverService scopeResolver;
    private final EvidenceBindingRepository bindingRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository parseResultRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectMapper objectMapper;

    public ScopedEvidenceBindingApplicationService(
            ContentScopeResolverService scopeResolver,
            EvidenceBindingRepository bindingRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository parseResultRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            ObjectMapper objectMapper
    ) {
        this.scopeResolver = scopeResolver;
        this.bindingRepository = bindingRepository;
        this.materialRepository = materialRepository;
        this.parseResultRepository = parseResultRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.objectMapper = objectMapper;
    }

    public EvidenceBindingSummaryResponse getSection(UUID sectionId) {
        ContentScope scope = scopeResolver.section(sectionId);
        List<EvidenceBindingEntity> bindings = bindingRepository
                .findBySectionIdAndSectionVersionNoOrderByCreatedAtAsc(sectionId, scope.revision());
        return summary(scope, bindings, bindings.isEmpty() ? "STALE" : "CURRENT");
    }

    @Transactional
    public EvidenceBindingSummaryResponse rebuildSection(UUID sectionId) {
        ContentScope scope = scopeResolver.section(sectionId);
        List<EvidenceBindingEntity> previous = bindingRepository.findBySectionIdOrderBySectionVersionNoDescCreatedAtAsc(sectionId);
        Map<String, String> inheritedStatuses = inheritedStatuses(previous);
        bindingRepository.deleteBySectionIdAndSectionVersionNo(sectionId, scope.revision());

        List<MaterialEntity> materials = scope.materialIds().isEmpty()
                ? List.of()
                : materialRepository.findAllById(scope.materialIds());
        Map<UUID, MaterialEntity> materialById = materials.stream()
                .collect(Collectors.toMap(MaterialEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<UUID, AiSemanticParseResultEntity> parseByMaterial = parseResults(materials);
        Map<UUID, List<KnowledgeChunkEntity>> chunksByMaterial = knowledgeChunkRepository
                .findByWorkspaceIdOrderByCreatedAtDesc(scope.workspaceId()).stream()
                .filter(chunk -> materialById.containsKey(chunk.getMaterialId()))
                .collect(Collectors.groupingBy(KnowledgeChunkEntity::getMaterialId));

        Map<String, LinkedHashSet<UUID>> paragraphSources = collectParagraphSources(scope.sourceTraceMap());
        List<EvidenceBindingEntity> entities = new ArrayList<>();
        OffsetDateTime now = OffsetDateTime.now();
        for (ParagraphSlice paragraph : splitParagraphs(scope.content())) {
            LinkedHashSet<UUID> sourceIds = paragraphSources.getOrDefault(paragraph.id(), new LinkedHashSet<>());
            if (sourceIds.isEmpty()) {
                entities.add(binding(scope, paragraph, null, null, null, "MISSING", "MISSING", now, inheritedStatuses));
                continue;
            }
            for (UUID materialId : sourceIds) {
                MaterialEntity material = materialById.get(materialId);
                if (material == null) {
                    entities.add(binding(scope, paragraph, null, null, null, "MISSING", "MISSING", now, inheritedStatuses));
                    continue;
                }
                AiSemanticParseResultEntity parse = parseByMaterial.get(materialId);
                KnowledgeChunkEntity chunk = bestChunk(paragraph.text(), chunksByMaterial.getOrDefault(materialId, List.of()));
                boolean strong = chunk != null || hasUsefulEvidence(parse);
                entities.add(binding(
                        scope,
                        paragraph,
                        material,
                        parse,
                        chunk,
                        strong ? "CONFIRMED" : "WEAK",
                        chunk == null ? "SOURCE_TRACE" : "KNOWLEDGE_CHUNK",
                        now,
                        inheritedStatuses
                ));
            }
        }
        List<EvidenceBindingEntity> saved = bindingRepository.saveAll(entities);
        return summary(scope, saved, "CURRENT");
    }

    public DocumentEvidenceSummaryResponse documentSummary(UUID documentId) {
        return documentSummary(documentId, scopeResolver.documentSections(documentId));
    }

    public DocumentEvidenceSummaryResponse documentSummary(UUID documentId, List<ContentScope> scopes) {
        LinkedHashSet<UUID> materialIds = scopes.stream()
                .flatMap(scope -> scope.materialIds().stream())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<UUID, MaterialEntity> materials = materialIds.isEmpty()
                ? Map.of()
                : materialRepository.findAllById(materialIds).stream()
                        .collect(Collectors.toMap(MaterialEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<UUID, AiSemanticParseResultEntity> parseByMaterial = parseResults(new ArrayList<>(materials.values()));
        Map<UUID, List<EvidenceBindingEntity>> currentBindings = bindingRepository
                .findByDocumentIdOrderBySectionIdAscCreatedAtAsc(documentId).stream()
                .collect(Collectors.groupingBy(EvidenceBindingEntity::getSectionId, LinkedHashMap::new, Collectors.toList()));
        List<EvidenceBindingSummaryResponse> sections = scopes.stream()
                .map(scope -> {
                    List<EvidenceBindingEntity> bindings = currentBindings.getOrDefault(scope.sectionId(), List.of()).stream()
                            .filter(binding -> Objects.equals(binding.getSectionVersionNo(), scope.revision()))
                            .toList();
                    return summary(scope, bindings, bindings.isEmpty() ? "STALE" : "CURRENT", materials, parseByMaterial);
                })
                .toList();
        int total = sections.stream().map(EvidenceBindingSummaryResponse::coverage).mapToInt(EvidenceCoverageReport::totalParagraphs).sum();
        int confirmed = sections.stream().map(EvidenceBindingSummaryResponse::coverage).mapToInt(EvidenceCoverageReport::confirmedParagraphs).sum();
        int weak = sections.stream().map(EvidenceBindingSummaryResponse::coverage).mapToInt(EvidenceCoverageReport::weakParagraphs).sum();
        int missing = sections.stream().map(EvidenceBindingSummaryResponse::coverage).mapToInt(EvidenceCoverageReport::missingParagraphs).sum();
        EvidenceCoverageReport coverage = coverage(total, confirmed, weak, missing);
        int detected = sections.stream().map(EvidenceBindingSummaryResponse::citationConsistency)
                .mapToInt(CitationConsistencyReport::detectedCitationCount).sum();
        int linked = sections.stream().flatMap(item -> item.usedMaterials().stream()).map(EvidenceMaterialResponse::materialId)
                .collect(Collectors.toSet()).size();
        int orphan = sections.stream().map(EvidenceBindingSummaryResponse::citationConsistency)
                .mapToInt(CitationConsistencyReport::orphanCitationCount).sum();
        int incomplete = sections.stream().map(EvidenceBindingSummaryResponse::citationConsistency)
                .mapToInt(CitationConsistencyReport::incompleteReferenceCount).sum();
        List<String> issues = sections.stream().flatMap(item -> item.citationConsistency().issues().stream()).distinct().limit(8).toList();
        CitationConsistencyReport citation = new CitationConsistencyReport(
                issues.isEmpty() ? "READY" : "NEEDS_REVIEW",
                detected,
                linked,
                missing,
                orphan,
                incomplete,
                issues
        );
        String state = sections.stream().anyMatch(item -> "STALE".equals(item.analysisState())) ? "STALE" : "CURRENT";
        return new DocumentEvidenceSummaryResponse(documentId, sections, coverage, citation, state);
    }

    public EvidenceBindingItemResponse updateStatus(UUID bindingId, String status) {
        EvidenceBindingEntity binding = bindingRepository.findById(bindingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "evidence binding 不存在"));
        if (!Set.of("CONFIRMED", "WEAK", "MISSING", "USER_CONFIRMED").contains(status)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_BODY, HttpStatus.BAD_REQUEST.value(), "不支持的可信链状态");
        }
        binding.setBindingStatus(status);
        if ("USER_CONFIRMED".equals(status)) binding.setConfidenceScore(BigDecimal.ONE);
        bindingRepository.save(binding);
        ContentScope scope = scopeResolver.section(binding.getSectionId());
        Map<UUID, MaterialEntity> materialMap = materials(scope);
        return item(binding, materialMap, parseResults(new ArrayList<>(materialMap.values())), scope.revision());
    }

    private EvidenceBindingEntity binding(
            ContentScope scope,
            ParagraphSlice paragraph,
            MaterialEntity material,
            AiSemanticParseResultEntity parse,
            KnowledgeChunkEntity chunk,
            String status,
            String supportType,
            OffsetDateTime now,
            Map<String, String> inheritedStatuses
    ) {
        String fingerprint = fingerprint(paragraph.text());
        UUID materialId = material == null ? null : material.getId();
        String inherited = inheritedStatuses.get(inheritanceKey(fingerprint, materialId));
        if ("USER_CONFIRMED".equals(inherited)) status = inherited;
        EvidenceBindingEntity entity = new EvidenceBindingEntity();
        entity.setId(UUID.randomUUID());
        entity.setDraftVersionId(null);
        entity.setScopeType("SECTION");
        entity.setDocumentId(scope.documentId());
        entity.setSectionId(scope.sectionId());
        entity.setSectionVersionNo(scope.revision());
        entity.setParagraphFingerprint(fingerprint);
        entity.setParagraphId(paragraph.id());
        entity.setKnowledgeChunkId(chunk == null ? null : chunk.getId());
        entity.setClaimText(snippet(paragraph.text(), 360));
        entity.setMaterialId(materialId);
        entity.setSourceExcerpt(sourceExcerpt(parse, chunk, material));
        entity.setTargetRangeJson(writeJson(Map.of(
                "sectionId", scope.sectionId().toString(),
                "start", paragraph.start(),
                "end", paragraph.end(),
                "selectedText", paragraph.text()
        )));
        entity.setConfidenceScore("USER_CONFIRMED".equals(status)
                ? BigDecimal.ONE
                : "CONFIRMED".equals(status) ? new BigDecimal("0.8200") : "WEAK".equals(status) ? new BigDecimal("0.5200") : BigDecimal.ZERO);
        entity.setSupportType(supportType);
        entity.setBindingStatus(status);
        entity.setCitationText(citationText(material, parse));
        entity.setCreatedAt(now);
        return entity;
    }

    private EvidenceBindingSummaryResponse summary(ContentScope scope, List<EvidenceBindingEntity> bindings, String state) {
        Map<UUID, MaterialEntity> materials = materials(scope);
        Map<UUID, AiSemanticParseResultEntity> parseByMaterial = parseResults(new ArrayList<>(materials.values()));
        return summary(scope, bindings, state, materials, parseByMaterial);
    }

    private EvidenceBindingSummaryResponse summary(
            ContentScope scope,
            List<EvidenceBindingEntity> bindings,
            String state,
            Map<UUID, MaterialEntity> materials,
            Map<UUID, AiSemanticParseResultEntity> parseByMaterial
    ) {
        Map<String, String> textByParagraph = splitParagraphs(scope.content()).stream()
                .collect(Collectors.toMap(ParagraphSlice::id, ParagraphSlice::text, (left, right) -> left, LinkedHashMap::new));
        Map<String, List<EvidenceBindingEntity>> grouped = bindings.stream()
                .collect(Collectors.groupingBy(EvidenceBindingEntity::getParagraphId, LinkedHashMap::new, Collectors.toList()));
        LinkedHashSet<String> paragraphIds = new LinkedHashSet<>(textByParagraph.keySet());
        paragraphIds.addAll(grouped.keySet());
        List<EvidenceParagraphResponse> paragraphs = paragraphIds.stream().map(paragraphId -> {
            List<EvidenceBindingItemResponse> items = grouped.getOrDefault(paragraphId, List.of()).stream()
                    .map(binding -> item(binding, materials, parseByMaterial, scope.revision()))
                    .toList();
            return new EvidenceParagraphResponse(paragraphId, textByParagraph.getOrDefault(paragraphId, ""), paragraphStatus(items), items);
        }).toList();
        Set<UUID> usedIds = bindings.stream().map(EvidenceBindingEntity::getMaterialId).filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<EvidenceMaterialResponse> used = usedIds.stream().map(materials::get).filter(Objects::nonNull)
                .map(material -> material(material, parseByMaterial.get(material.getId()))).toList();
        List<EvidenceMaterialResponse> unused = materials.values().stream().filter(material -> !usedIds.contains(material.getId()))
                .map(material -> material(material, parseByMaterial.get(material.getId()))).toList();
        List<String> missing = paragraphs.stream().filter(item -> "MISSING".equals(item.bindingStatus()))
                .map(EvidenceParagraphResponse::paragraphId).toList();
        int confirmed = (int) paragraphs.stream().filter(item -> Set.of("CONFIRMED", "USER_CONFIRMED").contains(item.bindingStatus())).count();
        int weak = (int) paragraphs.stream().filter(item -> "WEAK".equals(item.bindingStatus())).count();
        EvidenceCoverageReport coverage = coverage(paragraphs.size(), confirmed, weak, missing.size());
        CitationConsistencyReport citations = citationConsistency(scope.content(), paragraphs, used, parseByMaterial);
        return new EvidenceBindingSummaryResponse(
                null, paragraphs, missing, used, unused, coverage, citations,
                "SECTION", scope.documentId(), scope.sectionId(), scope.revision(), state
        );
    }

    private EvidenceBindingItemResponse item(
            EvidenceBindingEntity binding,
            Map<UUID, MaterialEntity> materials,
            Map<UUID, AiSemanticParseResultEntity> parseByMaterial,
            int currentVersion
    ) {
        UUID materialId = binding.getMaterialId();
        MaterialEntity material = materialId == null ? null : materials.get(materialId);
        AiSemanticParseResultEntity parse = materialId == null ? null : parseByMaterial.get(materialId);
        return new EvidenceBindingItemResponse(
                binding.getId(), binding.getDraftVersionId(), binding.getParagraphId(), binding.getMaterialId(), binding.getKnowledgeChunkId(),
                material == null ? null : material.getFilename(), binding.getClaimText(), binding.getSourceExcerpt(),
                Map.of("label", binding.getKnowledgeChunkId() == null ? "材料解析片段" : "知识库片段", "confidence", "approximate"),
                readMap(binding.getTargetRangeJson()), binding.getConfidenceScore(), binding.getSupportType(), binding.getBindingStatus(),
                binding.getCitationText(), metadata(parse), binding.getCreatedAt(), binding.getScopeType(), binding.getDocumentId(),
                binding.getSectionId(), binding.getSectionVersionNo(), binding.getParagraphFingerprint(),
                Objects.equals(binding.getSectionVersionNo(), currentVersion) ? "CURRENT" : "STALE"
        );
    }

    private Map<UUID, MaterialEntity> materials(ContentScope scope) {
        if (scope.materialIds().isEmpty()) return Map.of();
        return materialRepository.findAllById(scope.materialIds()).stream()
                .collect(Collectors.toMap(MaterialEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
    }

    private Map<UUID, AiSemanticParseResultEntity> parseResults(List<MaterialEntity> materials) {
        if (materials.isEmpty()) return Map.of();
        return parseResultRepository.findByMaterialIdIn(materials.stream().map(MaterialEntity::getId).toList()).stream()
                .collect(Collectors.toMap(AiSemanticParseResultEntity::getMaterialId, item -> item, (left, right) -> left));
    }

    private Map<String, String> inheritedStatuses(List<EvidenceBindingEntity> previous) {
        Map<String, String> result = new LinkedHashMap<>();
        previous.stream().filter(item -> "USER_CONFIRMED".equals(item.getBindingStatus()))
                .forEach(item -> result.putIfAbsent(inheritanceKey(item.getParagraphFingerprint(), item.getMaterialId()), item.getBindingStatus()));
        return result;
    }

    private String inheritanceKey(String fingerprint, UUID materialId) {
        return String.valueOf(fingerprint) + ":" + String.valueOf(materialId);
    }

    private Map<String, LinkedHashSet<UUID>> collectParagraphSources(Map<String, Object> trace) {
        Map<String, LinkedHashSet<UUID>> result = new LinkedHashMap<>();
        trace.forEach((key, value) -> {
            String paragraphId = key.matches("(?i)p\\d+") ? key.toLowerCase() : "p1";
            LinkedHashSet<UUID> ids = new LinkedHashSet<>();
            collectIds(value, ids);
            if (!ids.isEmpty()) result.put(paragraphId, ids);
        });
        return result;
    }

    private void collectIds(Object value, Set<UUID> ids) {
        if (value == null) return;
        if (value instanceof String string) {
            try { ids.add(UUID.fromString(string)); } catch (IllegalArgumentException ignored) { }
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            iterable.forEach(item -> collectIds(item, ids));
            return;
        }
        if (value instanceof Map<?, ?> map) {
            for (String key : List.of("materialIds", "materials", "sources", "materialId")) {
                if (map.containsKey(key)) collectIds(map.get(key), ids);
            }
        }
    }

    private List<ParagraphSlice> splitParagraphs(String content) {
        String text = content == null ? "" : content;
        if (text.isBlank()) return List.of(new ParagraphSlice("p1", 0, 0, ""));
        List<ParagraphSlice> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\S[\\s\\S]*?(?=\\n\\s*\\n|$)").matcher(text);
        int index = 1;
        while (matcher.find()) {
            String value = matcher.group().trim();
            if (!value.isBlank()) result.add(new ParagraphSlice("p" + index++, matcher.start(), matcher.end(), value));
        }
        return result.isEmpty() ? List.of(new ParagraphSlice("p1", 0, text.length(), text.trim())) : result;
    }

    private KnowledgeChunkEntity bestChunk(String paragraph, List<KnowledgeChunkEntity> chunks) {
        Set<String> tokens = tokens(paragraph);
        return chunks.stream().max(Comparator.comparingInt(chunk -> score(tokens, chunk.getChunkText())))
                .filter(chunk -> score(tokens, chunk.getChunkText()) > 0).orElse(null);
    }

    private int score(Set<String> tokens, String text) {
        String normalized = normalize(text);
        return (int) tokens.stream().filter(normalized::contains).count();
    }

    private Set<String> tokens(String text) {
        LinkedHashSet<String> result = new LinkedHashSet<>();
        String compact = normalize(text).replace(" ", "");
        for (int index = 0; index < compact.length() - 1 && result.size() < 80; index++) {
            result.add(compact.substring(index, index + 2));
        }
        return result;
    }

    private boolean hasUsefulEvidence(AiSemanticParseResultEntity parse) {
        return parse != null && ((parse.getDetectedEvidenceJson() != null && !parse.getDetectedEvidenceJson().matches("\\s*\\[\\s*]\\s*"))
                || (parse.getDetectedClaimsJson() != null && !parse.getDetectedClaimsJson().matches("\\s*\\[\\s*]\\s*")));
    }

    private String sourceExcerpt(AiSemanticParseResultEntity parse, KnowledgeChunkEntity chunk, MaterialEntity material) {
        if (chunk != null && hasText(chunk.getSourceExcerpt())) return snippet(chunk.getSourceExcerpt(), 420);
        if (chunk != null && hasText(chunk.getChunkText())) return snippet(chunk.getChunkText(), 420);
        if (parse != null && hasText(parse.getSummary())) return snippet(parse.getSummary(), 420);
        if (material != null && hasText(material.getPlainTextContent())) return snippet(material.getPlainTextContent(), 420);
        return null;
    }

    private String citationText(MaterialEntity material, AiSemanticParseResultEntity parse) {
        if (material == null) return null;
        Map<String, Object> metadata = metadata(parse);
        String year = String.valueOf(metadata.getOrDefault("year", "n.d."));
        Object authors = metadata.get("authors");
        String author = authors instanceof List<?> list && !list.isEmpty() ? String.valueOf(list.get(0)) : material.getFilename();
        return "（" + author + "，" + year + "）";
    }

    private EvidenceMaterialResponse material(MaterialEntity material, AiSemanticParseResultEntity parse) {
        return new EvidenceMaterialResponse(material.getId(), material.getFilename(), material.getFileType(), material.getSourceType(), material.isKeyMaterial(), metadata(parse));
    }

    private Map<String, Object> metadata(AiSemanticParseResultEntity parse) {
        return parse == null ? Map.of() : readMap(parse.getBibliographicMetadataJson());
    }

    private String paragraphStatus(List<EvidenceBindingItemResponse> items) {
        if (items.stream().anyMatch(item -> "USER_CONFIRMED".equals(item.bindingStatus()))) return "USER_CONFIRMED";
        if (items.stream().anyMatch(item -> "CONFIRMED".equals(item.bindingStatus()))) return "CONFIRMED";
        if (items.stream().anyMatch(item -> "WEAK".equals(item.bindingStatus()))) return "WEAK";
        return "MISSING";
    }

    private EvidenceCoverageReport coverage(int total, int confirmed, int weak, int missing) {
        int coverageRatio = total == 0 ? 0 : Math.round(((confirmed + weak) * 100f) / total);
        int confirmedRatio = total == 0 ? 0 : Math.round((confirmed * 100f) / total);
        String label = coverageRatio >= 90 ? "来源覆盖良好" : coverageRatio >= 70 ? "部分段落需确认" : "证据覆盖不足";
        List<String> recommendations = missing == 0 ? List.of("当前章节没有明确缺来源段落。")
                : List.of("为缺来源段落补充真实材料或研究结果后重建可信链。", "弱绑定需要人工核对原始材料。 ");
        return new EvidenceCoverageReport(total, confirmed, weak, missing, coverageRatio, confirmedRatio, label, recommendations);
    }

    private CitationConsistencyReport citationConsistency(
            String content,
            List<EvidenceParagraphResponse> paragraphs,
            List<EvidenceMaterialResponse> used,
            Map<UUID, AiSemanticParseResultEntity> parseByMaterial
    ) {
        int citations = count(APA_CITATION_PATTERN, content) + count(GBT_CITATION_PATTERN, content);
        int missing = (int) paragraphs.stream().filter(item -> "MISSING".equals(item.bindingStatus())).count();
        int incomplete = (int) used.stream().filter(item -> {
            Map<String, Object> metadata = item.bibliographicMetadata();
            return metadata.isEmpty() || !metadata.containsKey("authors") || !metadata.containsKey("year") || !metadata.containsKey("title");
        }).count();
        int orphan = citations > 0 && used.isEmpty() ? citations : 0;
        List<String> issues = new ArrayList<>();
        if (!used.isEmpty() && citations == 0) issues.add("章节已经使用材料，但正文中尚未识别到引用标记。");
        if (orphan > 0) issues.add("正文存在引用标记，但没有可追溯的已使用材料。");
        if (incomplete > 0) issues.add("有 " + incomplete + " 份引用材料的作者、年份或题名不完整。");
        return new CitationConsistencyReport(issues.isEmpty() ? "READY" : "NEEDS_REVIEW", citations, used.size(), missing, orphan, incomplete, issues);
    }

    private int count(Pattern pattern, String text) {
        int count = 0;
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        while (matcher.find()) count++;
        return count;
    }

    private String fingerprint(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(normalize(text).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException error) {
            throw new IllegalStateException(error);
        }
    }

    private String normalize(String text) {
        return text == null ? "" : text.replaceAll("\\s+", " ").trim().toLowerCase();
    }

    private String snippet(String text, int max) {
        String value = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        return value.length() <= max ? value : value.substring(0, max - 1) + "…";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String writeJson(Object value) {
        try { return objectMapper.writeValueAsString(value); } catch (JsonProcessingException error) { return "{}"; }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        if (json == null || json.isBlank()) return Map.of();
        try { return objectMapper.readValue(json, Map.class); } catch (JsonProcessingException error) { return Map.of(); }
    }

    private record ParagraphSlice(String id, int start, int end, String text) { }
}
