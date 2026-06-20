package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.evidence.EvidenceBindingItemResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceMaterialResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceParagraphResponse;
import com.aipm.cowriting.application.dto.evidence.CitationConsistencyReport;
import com.aipm.cowriting.application.dto.evidence.EvidenceCoverageReport;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.EvidenceBindingEntity;
import com.aipm.cowriting.domain.entity.KnowledgeChunkEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.EvidenceBindingRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EvidenceBindingApplicationService {

    private static final Set<String> ALLOWED_BINDING_STATUSES = Set.of("CONFIRMED", "WEAK", "MISSING", "USER_CONFIRMED");
    private static final Pattern PARAGRAPH_ID_PATTERN = Pattern.compile("p(\\d+)", Pattern.CASE_INSENSITIVE);
    private static final Pattern PAGE_HINT_PATTERN = Pattern.compile("(?:补充页码|页码|第)\\s*(\\d{1,4})\\s*(?:页)?");
    private static final Pattern APA_CITATION_PATTERN = Pattern.compile("[（(][^）)]{1,80}[，,]\\s*((19|20)\\d{2}|n\\.d\\.)[）)]");
    private static final Pattern GBT_CITATION_PATTERN = Pattern.compile("\\[(\\d{1,3})]");

    private final DraftVersionRepository draftVersionRepository;
    private final EvidenceBindingRepository evidenceBindingRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectMapper objectMapper;

    public EvidenceBindingApplicationService(
            DraftVersionRepository draftVersionRepository,
            EvidenceBindingRepository evidenceBindingRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            ObjectMapper objectMapper
    ) {
        this.draftVersionRepository = draftVersionRepository;
        this.evidenceBindingRepository = evidenceBindingRepository;
        this.materialRepository = materialRepository;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.objectMapper = objectMapper;
    }

    public EvidenceBindingSummaryResponse get(UUID draftId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return toSummary(draft, evidenceBindingRepository.findByDraftVersionIdOrderByCreatedAtAsc(draftId));
    }

    @Transactional
    public EvidenceBindingSummaryResponse rebuild(UUID draftId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        evidenceBindingRepository.deleteByDraftVersionId(draftId);

        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(draft.getWorkspaceId());
        Map<UUID, MaterialEntity> materialById = materials.stream()
                .collect(Collectors.toMap(MaterialEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        List<AiSemanticParseResultEntity> parseResults = aiSemanticParseResultRepository.findByMaterialIdIn(
                materials.stream().map(MaterialEntity::getId).toList()
        );
        Map<UUID, AiSemanticParseResultEntity> parseByMaterialId = parseResults.stream()
                .collect(Collectors.toMap(AiSemanticParseResultEntity::getMaterialId, item -> item, (left, right) -> left));
        Map<UUID, List<KnowledgeChunkEntity>> chunksByMaterialId = knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(draft.getWorkspaceId()).stream()
                .collect(Collectors.groupingBy(KnowledgeChunkEntity::getMaterialId));

        List<ParagraphSlice> paragraphs = splitParagraphs(draft.getDraftText());
        Map<String, LinkedHashSet<UUID>> paragraphSourceIds = collectParagraphSources(readMap(draft.getSourceTraceMapJson()));
        OffsetDateTime now = OffsetDateTime.now();

        List<EvidenceBindingEntity> entities = new ArrayList<>();
        for (ParagraphSlice paragraph : paragraphs) {
            LinkedHashSet<UUID> sourceIds = paragraphSourceIds.getOrDefault(paragraph.id(), new LinkedHashSet<>());
            if (sourceIds.isEmpty()) {
                entities.add(missingBinding(draft, paragraph, now));
                continue;
            }

            for (UUID materialId : sourceIds) {
                MaterialEntity material = materialById.get(materialId);
                if (material == null) {
                    entities.add(missingBinding(draft, paragraph, now));
                    continue;
                }
                AiSemanticParseResultEntity parseResult = parseByMaterialId.get(materialId);
                KnowledgeChunkEntity chunk = bestChunk(paragraph.text(), chunksByMaterialId.getOrDefault(materialId, List.of()));
                boolean hasStrongParsedEvidence = parseResult != null && hasUsefulEvidence(parseResult);
                String status = hasStrongParsedEvidence || chunk != null ? "CONFIRMED" : "WEAK";
                String supportType = chunk != null ? "KNOWLEDGE_CHUNK" : "SOURCE_TRACE";

                EvidenceBindingEntity entity = new EvidenceBindingEntity();
                entity.setId(UUID.randomUUID());
                entity.setDraftVersionId(draft.getId());
                entity.setParagraphId(paragraph.id());
                entity.setKnowledgeChunkId(chunk == null ? null : chunk.getId());
                entity.setClaimText(snippet(paragraph.text(), 360));
                entity.setMaterialId(materialId);
                entity.setSourceExcerpt(sourceExcerpt(parseResult, chunk, material));
                entity.setTargetRangeJson(writeJson(Map.of(
                        "start", paragraph.start(),
                        "end", paragraph.end(),
                        "selectedText", paragraph.text()
                )));
                entity.setConfidenceScore(status.equals("CONFIRMED") ? new BigDecimal("0.8200") : new BigDecimal("0.5200"));
                entity.setSupportType(supportType);
                entity.setBindingStatus(status);
                entity.setCitationText(citationText(material, parseResult));
                entity.setCreatedAt(now);
                entities.add(entity);
            }
        }

        List<EvidenceBindingEntity> saved = evidenceBindingRepository.saveAll(entities);
        return toSummary(draft, saved);
    }

    public EvidenceBindingItemResponse updateStatus(UUID bindingId, String status) {
        EvidenceBindingEntity entity = evidenceBindingRepository.findById(bindingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "evidence binding 不存在"));
        String normalized = normalizeBindingStatus(status);
        entity.setBindingStatus(normalized);
        if ("USER_CONFIRMED".equals(normalized)) {
            entity.setConfidenceScore(new BigDecimal("1.0000"));
        }
        evidenceBindingRepository.save(entity);
        return toItemResponse(entity, materialMap(entity.getDraftVersionId()), parseMap(entity.getDraftVersionId()));
    }

    private EvidenceBindingSummaryResponse toSummary(DraftVersionEntity draft, List<EvidenceBindingEntity> bindings) {
        List<ParagraphSlice> paragraphSlices = splitParagraphs(draft.getDraftText());
        Map<String, String> paragraphTextById = paragraphSlices.stream()
                .collect(Collectors.toMap(ParagraphSlice::id, ParagraphSlice::text, (left, right) -> left, LinkedHashMap::new));
        Map<UUID, MaterialEntity> materialById = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(draft.getWorkspaceId()).stream()
                .collect(Collectors.toMap(MaterialEntity::getId, item -> item, (left, right) -> left, LinkedHashMap::new));
        Map<UUID, AiSemanticParseResultEntity> parseByMaterialId = parseMapByWorkspace(draft.getWorkspaceId());

        Map<String, List<EvidenceBindingEntity>> byParagraph = bindings.stream()
                .collect(Collectors.groupingBy(EvidenceBindingEntity::getParagraphId, LinkedHashMap::new, Collectors.toList()));
        LinkedHashSet<String> paragraphIds = new LinkedHashSet<>(paragraphTextById.keySet());
        paragraphIds.addAll(byParagraph.keySet());

        List<EvidenceParagraphResponse> paragraphs = paragraphIds.stream()
                .map(paragraphId -> {
                    List<EvidenceBindingItemResponse> items = byParagraph.getOrDefault(paragraphId, List.of()).stream()
                            .map(item -> toItemResponse(item, materialById, parseByMaterialId))
                            .toList();
                    return new EvidenceParagraphResponse(
                            paragraphId,
                            paragraphTextById.getOrDefault(paragraphId, ""),
                            paragraphStatus(items),
                            items
                    );
                })
                .toList();

        Set<UUID> usedMaterialIds = bindings.stream()
                .map(EvidenceBindingEntity::getMaterialId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<String> missingParagraphIds = paragraphs.stream()
                .filter(item -> "MISSING".equals(item.bindingStatus()))
                .map(EvidenceParagraphResponse::paragraphId)
                .toList();

        List<EvidenceMaterialResponse> usedMaterials = usedMaterialIds.stream()
                .map(materialById::get)
                .filter(Objects::nonNull)
                .map(material -> toMaterialResponse(material, parseByMaterialId.get(material.getId())))
                .toList();
        List<EvidenceMaterialResponse> unusedMaterials = materialById.values().stream()
                .filter(material -> !usedMaterialIds.contains(material.getId()))
                .map(material -> toMaterialResponse(material, parseByMaterialId.get(material.getId())))
                .toList();

        return new EvidenceBindingSummaryResponse(
                draft.getId(),
                paragraphs,
                missingParagraphIds,
                usedMaterials,
                unusedMaterials,
                coverageReport(paragraphs),
                citationConsistencyReport(draft.getDraftText(), paragraphs, usedMaterials, parseByMaterialId)
        );
    }

    private EvidenceBindingEntity missingBinding(DraftVersionEntity draft, ParagraphSlice paragraph, OffsetDateTime now) {
        EvidenceBindingEntity entity = new EvidenceBindingEntity();
        entity.setId(UUID.randomUUID());
        entity.setDraftVersionId(draft.getId());
        entity.setParagraphId(paragraph.id());
        entity.setClaimText(snippet(paragraph.text(), 360));
        entity.setMaterialId(null);
        entity.setSourceExcerpt(null);
        entity.setTargetRangeJson(writeJson(Map.of(
                "start", paragraph.start(),
                "end", paragraph.end(),
                "selectedText", paragraph.text()
        )));
        entity.setConfidenceScore(BigDecimal.ZERO);
        entity.setSupportType("MISSING");
        entity.setBindingStatus("MISSING");
        entity.setCitationText(null);
        entity.setCreatedAt(now);
        return entity;
    }

    private Map<String, LinkedHashSet<UUID>> collectParagraphSources(Map<String, Object> sourceTraceMap) {
        Map<String, LinkedHashSet<UUID>> result = new LinkedHashMap<>();
        if (sourceTraceMap == null || sourceTraceMap.isEmpty()) {
            return result;
        }
        sourceTraceMap.forEach((key, value) -> {
            String paragraphId = normalizeParagraphId(key);
            LinkedHashSet<UUID> ids = new LinkedHashSet<>();
            collectSourceIds(value, ids);
            if (!ids.isEmpty()) {
                result.put(paragraphId, ids);
            }
        });
        return result;
    }

    private void collectSourceIds(Object value, LinkedHashSet<UUID> sourceIds) {
        if (value == null) {
            return;
        }
        if (value instanceof String stringValue) {
            parseUuid(stringValue).ifPresent(sourceIds::add);
            return;
        }
        if (value instanceof Iterable<?> iterable) {
            for (Object item : iterable) {
                collectSourceIds(item, sourceIds);
            }
            return;
        }
        if (value instanceof Map<?, ?> mapValue) {
            Object nested = mapValue.get("materialIds");
            if (nested == null) {
                nested = mapValue.get("materials");
            }
            if (nested == null) {
                nested = mapValue.get("sources");
            }
            if (nested == null) {
                nested = mapValue.get("materialId");
            }
            collectSourceIds(nested, sourceIds);
        }
    }

    private List<ParagraphSlice> splitParagraphs(String draftText) {
        String text = draftText == null ? "" : draftText;
        if (text.isBlank()) {
            return List.of(new ParagraphSlice("p1", 0, 0, ""));
        }
        List<ParagraphSlice> paragraphs = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\S[\\s\\S]*?(?=\\n\\s*\\n|$)").matcher(text);
        int index = 1;
        while (matcher.find()) {
            String paragraph = matcher.group().trim();
            if (paragraph.isBlank()) {
                continue;
            }
            paragraphs.add(new ParagraphSlice(
                    "p" + index,
                    matcher.start(),
                    matcher.end(),
                    paragraph
            ));
            index++;
        }
        if (paragraphs.isEmpty()) {
            paragraphs.add(new ParagraphSlice("p1", 0, text.length(), text.trim()));
        }
        return paragraphs;
    }

    private KnowledgeChunkEntity bestChunk(String paragraph, List<KnowledgeChunkEntity> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return null;
        }
        String normalizedParagraph = normalizeText(paragraph);
        return chunks.stream()
                .max(Comparator.comparingInt(chunk -> lexicalScore(normalizedParagraph, normalizeText(chunk.getChunkText()))))
                .filter(chunk -> lexicalScore(normalizedParagraph, normalizeText(chunk.getChunkText())) > 0)
                .orElse(null);
    }

    private int lexicalScore(String paragraph, String chunk) {
        if (paragraph.isBlank() || chunk.isBlank()) {
            return 0;
        }
        int score = 0;
        for (String token : lexicalTokens(paragraph)) {
            if (chunk.contains(token)) {
                score++;
            }
        }
        return score;
    }

    private Set<String> lexicalTokens(String text) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String token : text.split("\\s+")) {
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }
        String compact = text.replaceAll("\\s+", "");
        for (int i = 0; i < compact.length() - 1 && tokens.size() < 80; i++) {
            tokens.add(compact.substring(i, i + 2));
        }
        return tokens;
    }

    private String sourceExcerpt(AiSemanticParseResultEntity parseResult, KnowledgeChunkEntity chunk, MaterialEntity material) {
        if (chunk != null && hasText(chunk.getSourceExcerpt())) {
            return snippet(chunk.getSourceExcerpt(), 420);
        }
        if (chunk != null && hasText(chunk.getChunkText())) {
            return snippet(chunk.getChunkText(), 420);
        }
        if (parseResult != null && hasText(parseResult.getSummary())) {
            return snippet(parseResult.getSummary(), 420);
        }
        if (material != null && hasText(material.getPlainTextContent())) {
            return snippet(material.getPlainTextContent(), 420);
        }
        return null;
    }

    private boolean hasUsefulEvidence(AiSemanticParseResultEntity parseResult) {
        return hasJsonContent(parseResult.getDetectedEvidenceJson())
                || hasJsonContent(parseResult.getDetectedClaimsJson())
                || hasText(parseResult.getSummary());
    }

    private boolean hasJsonContent(String json) {
        if (!hasText(json) || "[]".equals(json.trim()) || "{}".equals(json.trim())) {
            return false;
        }
        try {
            Object value = objectMapper.readValue(json, Object.class);
            if (value instanceof List<?> list) {
                return !list.isEmpty();
            }
            if (value instanceof Map<?, ?> map) {
                return !map.isEmpty();
            }
            return value != null;
        } catch (JsonProcessingException e) {
            return false;
        }
    }

    private EvidenceBindingItemResponse toItemResponse(
            EvidenceBindingEntity entity,
            Map<UUID, MaterialEntity> materialById,
            Map<UUID, AiSemanticParseResultEntity> parseByMaterialId
    ) {
        MaterialEntity material = entity.getMaterialId() == null ? null : materialById.get(entity.getMaterialId());
        AiSemanticParseResultEntity parseResult = entity.getMaterialId() == null ? null : parseByMaterialId.get(entity.getMaterialId());
        return new EvidenceBindingItemResponse(
                entity.getId(),
                entity.getDraftVersionId(),
                entity.getParagraphId(),
                entity.getMaterialId(),
                entity.getKnowledgeChunkId(),
                material == null ? null : material.getFilename(),
                entity.getClaimText(),
                entity.getSourceExcerpt(),
                sourceLocation(entity, material),
                readMap(entity.getTargetRangeJson()),
                entity.getConfidenceScore(),
                entity.getSupportType(),
                entity.getBindingStatus(),
                entity.getCitationText(),
                parseResult == null ? Map.of() : readMap(defaultJson(parseResult.getBibliographicMetadataJson(), "{}")),
                entity.getCreatedAt()
        );
    }

    private Map<String, Object> sourceLocation(EvidenceBindingEntity entity, MaterialEntity material) {
        Map<String, Object> location = new LinkedHashMap<>();
        location.put("materialId", entity.getMaterialId());
        location.put("knowledgeChunkId", entity.getKnowledgeChunkId());
        location.put("supportType", entity.getSupportType());

        if (entity.getMaterialId() == null) {
            location.put("type", "missing");
            location.put("label", "暂无可定位材料");
            location.put("confidence", "none");
            return location;
        }

        Optional<Integer> pageHint = pageHint(entity.getSourceExcerpt())
                .or(() -> pageHint(material == null ? null : material.getSupplementText()))
                .or(() -> pageHint(material == null ? null : material.getPlainTextContent()));
        if (pageHint.isPresent()) {
            location.put("type", "page_hint");
            location.put("page", pageHint.get());
            location.put("label", "第 " + pageHint.get() + " 页附近");
            location.put("confidence", "explicit");
            location.put("previewUrl", "/api/v1/materials/" + entity.getMaterialId() + "/preview");
            return location;
        }

        if (entity.getKnowledgeChunkId() != null) {
            location.put("type", "knowledge_chunk");
            location.put("label", "知识库片段 " + shortId(entity.getKnowledgeChunkId()));
            location.put("confidence", "inferred");
            location.put("previewUrl", "/api/v1/materials/" + entity.getMaterialId() + "/preview");
            return location;
        }

        Map<String, Object> targetRange = readMap(entity.getTargetRangeJson());
        if (!targetRange.isEmpty()) {
            location.put("type", "draft_range");
            location.put("label", "正文字符 " + targetRange.getOrDefault("start", "?") + "-" + targetRange.getOrDefault("end", "?") + " 对应材料摘录");
            location.put("confidence", "inferred");
            location.put("previewUrl", "/api/v1/materials/" + entity.getMaterialId() + "/preview");
            return location;
        }

        location.put("type", "excerpt");
        location.put("label", "材料摘录位置");
        location.put("confidence", "inferred");
        location.put("previewUrl", "/api/v1/materials/" + entity.getMaterialId() + "/preview");
        return location;
    }

    private EvidenceCoverageReport coverageReport(List<EvidenceParagraphResponse> paragraphs) {
        int total = paragraphs.size();
        int confirmed = (int) paragraphs.stream()
                .filter(item -> "CONFIRMED".equals(item.bindingStatus()) || "USER_CONFIRMED".equals(item.bindingStatus()))
                .count();
        int weak = (int) paragraphs.stream().filter(item -> "WEAK".equals(item.bindingStatus())).count();
        int missing = (int) paragraphs.stream().filter(item -> "MISSING".equals(item.bindingStatus())).count();
        int coverageRatio = total == 0 ? 0 : Math.round(((confirmed + weak) * 100.0f) / total);
        int confirmedRatio = total == 0 ? 0 : Math.round((confirmed * 100.0f) / total);
        String healthLabel = confirmedRatio >= 80 && missing == 0
                ? "可信链健康"
                : coverageRatio >= 70
                ? "仍需补强"
                : "证据不足";
        List<String> recommendations = new ArrayList<>();
        if (missing > 0) {
            recommendations.add("有 " + missing + " 个段落缺少明确来源，建议补充材料或重新生成可信链。");
        }
        if (weak > 0) {
            recommendations.add("有 " + weak + " 个段落为弱绑定，建议打开原始材料核对后确认可信。");
        }
        if (confirmedRatio < 80 && total > 0) {
            recommendations.add("已确认来源比例低于 80%，导出前建议优先处理关键段落。");
        }
        if (recommendations.isEmpty()) {
            recommendations.add("当前正文段落基本具备可追溯来源，可进入导出前格式检查。");
        }
        return new EvidenceCoverageReport(total, confirmed, weak, missing, coverageRatio, confirmedRatio, healthLabel, recommendations);
    }

    private CitationConsistencyReport citationConsistencyReport(
            String draftText,
            List<EvidenceParagraphResponse> paragraphs,
            List<EvidenceMaterialResponse> usedMaterials,
            Map<UUID, AiSemanticParseResultEntity> parseByMaterialId
    ) {
        String text = draftText == null ? "" : draftText;
        int detectedCitationCount = countMatches(APA_CITATION_PATTERN, text) + countMatches(GBT_CITATION_PATTERN, text);
        int linkedMaterialCount = usedMaterials.size();
        int missingCitationParagraphCount = (int) paragraphs.stream()
                .filter(paragraph -> !"MISSING".equals(paragraph.bindingStatus()))
                .filter(paragraph -> paragraph.bindings().stream().noneMatch(binding -> hasText(binding.citationText())))
                .count();
        int orphanCitationCount = detectedCitationCount > 0 && linkedMaterialCount == 0 ? detectedCitationCount : 0;
        int incompleteReferenceCount = (int) usedMaterials.stream()
                .filter(material -> isBibliographicMetadataIncomplete(parseByMaterialId.get(material.materialId())))
                .count();

        List<String> issues = new ArrayList<>();
        if (detectedCitationCount == 0 && linkedMaterialCount > 0) {
            issues.add("正文已经绑定材料，但暂未检测到正文引用标记。");
        }
        if (orphanCitationCount > 0) {
            issues.add("正文存在引用标记，但没有可追溯材料来源。");
        }
        if (missingCitationParagraphCount > 0) {
            issues.add("有 " + missingCitationParagraphCount + " 个有来源段落缺少可插入引用文本。");
        }
        if (incompleteReferenceCount > 0) {
            issues.add("有 " + incompleteReferenceCount + " 份已使用材料缺少作者、年份或题名。");
        }
        String status = issues.isEmpty() ? "READY" : (orphanCitationCount > 0 || incompleteReferenceCount > 0 ? "NEEDS_FIX" : "NEEDS_REVIEW");
        if (issues.isEmpty()) {
            issues.add("正文引用、材料来源和文献信息当前未发现明显冲突。");
        }
        return new CitationConsistencyReport(
                status,
                detectedCitationCount,
                linkedMaterialCount,
                missingCitationParagraphCount,
                orphanCitationCount,
                incompleteReferenceCount,
                issues
        );
    }

    private int countMatches(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text == null ? "" : text);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }

    private boolean isBibliographicMetadataIncomplete(AiSemanticParseResultEntity parseResult) {
        if (parseResult == null || !hasText(parseResult.getBibliographicMetadataJson())) {
            return true;
        }
        Map<String, Object> metadata = readMap(parseResult.getBibliographicMetadataJson());
        Object authors = metadata.get("authors");
        boolean hasAuthor = authors instanceof List<?> list && list.stream().anyMatch(item -> item != null && !String.valueOf(item).isBlank());
        return !hasAuthor || !hasText(stringValue(metadata.get("year"))) || !hasText(stringValue(metadata.get("title")));
    }

    private Optional<Integer> pageHint(String value) {
        if (!hasText(value)) {
            return Optional.empty();
        }
        Matcher matcher = PAGE_HINT_PATTERN.matcher(value);
        if (!matcher.find()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Integer.parseInt(matcher.group(1)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String shortId(UUID id) {
        if (id == null) {
            return "";
        }
        String value = id.toString();
        return "#" + value.substring(0, Math.min(8, value.length()));
    }

    private EvidenceMaterialResponse toMaterialResponse(MaterialEntity material, AiSemanticParseResultEntity parseResult) {
        return new EvidenceMaterialResponse(
                material.getId(),
                material.getFilename(),
                material.getFileType(),
                material.getSourceType(),
                material.isKeyMaterial(),
                parseResult == null ? Map.of() : readMap(defaultJson(parseResult.getBibliographicMetadataJson(), "{}"))
        );
    }

    private String citationText(MaterialEntity material, AiSemanticParseResultEntity parseResult) {
        Map<String, Object> metadata = parseResult == null ? Map.of() : readMap(defaultJson(parseResult.getBibliographicMetadataJson(), "{}"));
        String year = stringValue(metadata.get("year"));
        Object authors = metadata.get("authors");
        if (authors instanceof List<?> authorList && !authorList.isEmpty()) {
            String firstAuthor = stringValue(authorList.get(0));
            if (hasText(firstAuthor) && hasText(year)) {
                return "（" + firstAuthor + "，" + year + "）";
            }
        }
        String title = stringValue(metadata.get("title"));
        if (hasText(title) && hasText(year)) {
            return "（" + title + "，" + year + "）";
        }
        if (material != null && hasText(material.getFilename())) {
            return "（" + material.getFilename() + "）";
        }
        return null;
    }

    private Map<UUID, MaterialEntity> materialMap(UUID draftVersionId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(draft.getWorkspaceId()).stream()
                .collect(Collectors.toMap(MaterialEntity::getId, item -> item, (left, right) -> left));
    }

    private Map<UUID, AiSemanticParseResultEntity> parseMap(UUID draftVersionId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftVersionId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return parseMapByWorkspace(draft.getWorkspaceId());
    }

    private Map<UUID, AiSemanticParseResultEntity> parseMapByWorkspace(UUID workspaceId) {
        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        return aiSemanticParseResultRepository.findByMaterialIdIn(materials.stream().map(MaterialEntity::getId).toList()).stream()
                .collect(Collectors.toMap(AiSemanticParseResultEntity::getMaterialId, item -> item, (left, right) -> left));
    }

    private String paragraphStatus(List<EvidenceBindingItemResponse> items) {
        if (items.isEmpty()) {
            return "MISSING";
        }
        if (items.stream().anyMatch(item -> "MISSING".equals(item.bindingStatus()))) {
            return "MISSING";
        }
        if (items.stream().anyMatch(item -> "WEAK".equals(item.bindingStatus()))) {
            return "WEAK";
        }
        if (items.stream().anyMatch(item -> "USER_CONFIRMED".equals(item.bindingStatus()))) {
            return "USER_CONFIRMED";
        }
        return "CONFIRMED";
    }

    private String normalizeBindingStatus(String status) {
        String normalized = status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
        if (ALLOWED_BINDING_STATUSES.contains(normalized)) {
            return normalized;
        }
        throw new BusinessException(
                ErrorCode.INVALID_REQUEST_BODY,
                HttpStatus.BAD_REQUEST.value(),
                "binding status 仅支持 CONFIRMED / WEAK / MISSING / USER_CONFIRMED"
        );
    }

    private String normalizeParagraphId(String value) {
        if (value == null || value.isBlank()) {
            return "p1";
        }
        Matcher matcher = PARAGRAPH_ID_PATTERN.matcher(value.trim());
        if (matcher.find()) {
            return "p" + matcher.group(1);
        }
        Matcher digitMatcher = Pattern.compile("(\\d+)").matcher(value.trim());
        if (digitMatcher.find()) {
            return "p" + digitMatcher.group(1);
        }
        return value.trim();
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    private String normalizeText(String value) {
        return value == null ? "" : value.replaceAll("[\\p{Punct}\\s]+", " ").toLowerCase(Locale.ROOT);
    }

    private String snippet(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, Math.max(0, maxLength - 1)) + "…";
    }

    private String defaultJson(String json, String fallback) {
        return json == null || json.isBlank() ? fallback : json;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readMap(String json) {
        try {
            return objectMapper.readValue(defaultJson(json, "{}"), Map.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 反序列化失败");
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    private record ParagraphSlice(String id, int start, int end, String text) {
    }
}
