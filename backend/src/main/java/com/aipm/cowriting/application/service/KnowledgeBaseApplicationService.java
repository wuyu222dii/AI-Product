package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.knowledge.KnowledgeBuildResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeChunkResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchRequest;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchResponse;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.common.api.PagedResponse;
import com.aipm.cowriting.common.api.Pagination;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.KnowledgeChunkEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.text.Normalizer;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class KnowledgeBaseApplicationService {

    private static final int CHUNK_SIZE = 900;
    private static final int CHUNK_OVERLAP = 120;
    private static final int MAX_CHUNKS_PER_WORKSPACE = 80;
    private static final int MIN_READABLE_CHUNK_LENGTH = 24;
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[\\p{IsHan}]{2,12}|[A-Za-z][A-Za-z0-9_-]{2,}|\\d+(?:\\.\\d+)?%?");
    private static final Pattern MARKDOWN_TABLE_SEPARATOR = Pattern.compile("(?m)^\\s*\\|?\\s*[-:]{3,}(?:\\s*\\|\\s*[-:]{3,})+\\s*\\|?\\s*$");
    private static final Pattern SUSPICIOUS_SYMBOL_RUN = Pattern.compile("[#*_~=|]{8,}");
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "with", "this", "that", "from", "paper", "study", "research",
            "材料", "摘要", "主题", "关系", "观点", "证据", "要求", "原文", "内容", "可以", "进行"
    );

    private final WorkspaceRepository workspaceRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final AcademicDocumentApplicationService academicDocumentService;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseApplicationService(
            WorkspaceRepository workspaceRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            AcademicDocumentApplicationService academicDocumentService,
            ObjectMapper objectMapper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.materialRepository = materialRepository;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.academicDocumentService = academicDocumentService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public KnowledgeBuildResponse build(UUID workspaceId) {
        assertWorkspaceExists(workspaceId);
        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        Map<UUID, AiSemanticParseResultEntity> parseResultMap = buildParseResultMap(materials);

        knowledgeChunkRepository.deleteByWorkspaceId(workspaceId);

        List<KnowledgeChunkEntity> chunks = new ArrayList<>();
        int eligibleMaterialCount = 0;
        for (MaterialEntity material : materials) {
            AiSemanticParseResultEntity parseResult = parseResultMap.get(material.getId());
            if (!isKnowledgeEligible(material, parseResult)) {
                continue;
            }
            eligibleMaterialCount += 1;
            chunks.addAll(buildChunksForMaterial(material, parseResult, MAX_CHUNKS_PER_WORKSPACE - chunks.size()));
            if (chunks.size() >= MAX_CHUNKS_PER_WORKSPACE) {
                break;
            }
        }

        knowledgeChunkRepository.saveAll(chunks);
        return new KnowledgeBuildResponse(
                workspaceId,
                eligibleMaterialCount,
                chunks.size(),
                chunks.isEmpty() ? "NEEDS_PARSED_MATERIAL" : "LEXICAL_READY",
                buildMessage(chunks.size())
        );
    }

    public PagedResponse<KnowledgeChunkResponse> list(UUID workspaceId) {
        assertWorkspaceExists(workspaceId);
        List<KnowledgeChunkEntity> chunks = knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        Map<UUID, MaterialEntity> materialMap = buildMaterialMap(chunks);
        List<KnowledgeChunkResponse> items = chunks.stream()
                .map(chunk -> toResponse(chunk, materialMap.get(chunk.getMaterialId()), 0))
                .toList();
        return new PagedResponse<>(items, new Pagination(1, items.size(), items.size()));
    }

    public KnowledgeSearchResponse search(UUID workspaceId, KnowledgeSearchRequest request) {
        assertWorkspaceExists(workspaceId);
        String query = request.query() == null ? "" : request.query().trim();
        if (query.isBlank()) {
            throw new BusinessException(
                    ErrorCode.INVALID_QUERY_PARAM,
                    HttpStatus.BAD_REQUEST.value(),
                    "请输入要检索的问题或关键词"
            );
        }

        int limit = normalizeLimit(request.limit());
        Set<UUID> allowedMaterialIds = resolveSearchMaterialIds(workspaceId, request);
        List<KnowledgeChunkEntity> chunks = knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        List<SearchCandidate> candidates = chunks.stream()
                .filter(chunk -> allowedMaterialIds == null || allowedMaterialIds.contains(chunk.getMaterialId()))
                .filter(chunk -> matchesTags(chunk, request.tags()))
                .map(chunk -> new SearchCandidate(chunk, roundScore(lexicalScore(query, chunk))))
                .filter(candidate -> candidate.score() > 0)
                .sorted(Comparator.comparingDouble(SearchCandidate::score).reversed())
                .limit(limit)
                .toList();

        Map<UUID, MaterialEntity> materialMap = buildMaterialMap(candidates.stream().map(SearchCandidate::chunk).toList());
        List<KnowledgeChunkResponse> items = candidates.stream()
                .map(candidate -> toResponse(candidate.chunk(), materialMap.get(candidate.chunk().getMaterialId()), candidate.score()))
                .toList();

        return new KnowledgeSearchResponse(workspaceId, query, items.size(), items);
    }

    private List<KnowledgeChunkEntity> buildChunksForMaterial(
            MaterialEntity material,
            AiSemanticParseResultEntity parseResult,
            int remainingCapacity
    ) {
        if (remainingCapacity <= 0) {
            return List.of();
        }

        List<String> sourceTexts = new ArrayList<>();
        String signalText = buildSignalText(material, parseResult);
        if (!signalText.isBlank()) {
            sourceTexts.add(signalText);
        }

        String rawText = joinNonBlank(material.getPlainTextContent(), material.getSupplementText());
        sourceTexts.addAll(splitIntoChunks(rawText));

        List<KnowledgeChunkEntity> chunks = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int chunkIndex = 1;
        for (String sourceText : sourceTexts) {
            String normalized = cleanChunkText(sourceText);
            if (!isReadableKnowledgeChunk(normalized) || !seen.add(normalized)) {
                continue;
            }
            KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
            chunk.setId(UUID.randomUUID());
            chunk.setWorkspaceId(material.getWorkspaceId());
            chunk.setMaterialId(material.getId());
            chunk.setChunkIndex(chunkIndex++);
            chunk.setChunkText(normalized);
            chunk.setSourceExcerpt(shorten(normalized, 260));
            chunk.setKeywordsJson(writeJson(extractKeywords(normalized)));
            chunk.setCreatedAt(OffsetDateTime.now());
            chunks.add(chunk);
            if (chunks.size() >= remainingCapacity) {
                break;
            }
        }
        return chunks;
    }

    private String buildSignalText(MaterialEntity material, AiSemanticParseResultEntity parseResult) {
        List<String> lines = new ArrayList<>();
        lines.add("材料文件：" + nullSafe(material.getFilename()));
        if (parseResult.getMaterialCategory() != null) {
            lines.add("材料角色：" + parseResult.getMaterialCategory());
        }
        appendIfPresent(lines, "解析摘要", parseResult.getSummary());
        appendIfPresent(lines, "主题关系", parseResult.getTopicRelation());
        appendList(lines, "观点", readStringList(parseResult.getDetectedClaimsJson()));
        appendList(lines, "证据", readStringList(parseResult.getDetectedEvidenceJson()));
        appendList(lines, "写作与提交要求", readStringList(parseResult.getDetectedRequirementsJson()));
        appendIfPresent(lines, "文献信息", formatBibliographicMetadata(readBibliographicMetadata(parseResult)));
        return shorten(cleanChunkText(String.join("\n", lines)), CHUNK_SIZE);
    }

    private Map<UUID, AiSemanticParseResultEntity> buildParseResultMap(List<MaterialEntity> materials) {
        if (materials.isEmpty()) {
            return Map.of();
        }
        List<UUID> materialIds = materials.stream().map(MaterialEntity::getId).toList();
        List<AiSemanticParseResultEntity> parseResults = aiSemanticParseResultRepository.findByMaterialIdIn(materialIds);
        Map<UUID, AiSemanticParseResultEntity> parseResultMap = new LinkedHashMap<>();
        for (AiSemanticParseResultEntity parseResult : parseResults) {
            parseResultMap.put(parseResult.getMaterialId(), parseResult);
        }
        return parseResultMap;
    }

    private Set<UUID> resolveSearchMaterialIds(UUID workspaceId, KnowledgeSearchRequest request) {
        Set<UUID> allowedIds = null;
        if (request.documentId() != null) {
            AcademicDocumentEntity document = academicDocumentService.getDocument(request.documentId());
            if (!document.getWorkspaceId().equals(workspaceId)) {
                throw new BusinessException(
                        ErrorCode.RESOURCE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        "学术文档不存在"
                );
            }
            allowedIds = academicDocumentService.resolveMaterials(document).stream()
                    .map(MaterialEntity::getId)
                    .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        }
        if (request.materialIds() != null && !request.materialIds().isEmpty()) {
            Set<UUID> requestedIds = new LinkedHashSet<>(request.materialIds());
            if (allowedIds == null) {
                allowedIds = requestedIds;
            } else {
                allowedIds.retainAll(requestedIds);
            }
        }
        return allowedIds;
    }

    private boolean matchesTags(KnowledgeChunkEntity chunk, List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return true;
        }
        String searchable = normalizeForSearch(chunk.getChunkText() + " " + String.join(" ", readStringList(chunk.getKeywordsJson())));
        return tags.stream()
                .filter(this::hasText)
                .map(this::normalizeForSearch)
                .anyMatch(searchable::contains);
    }

    private Map<UUID, MaterialEntity> buildMaterialMap(List<KnowledgeChunkEntity> chunks) {
        if (chunks.isEmpty()) {
            return Map.of();
        }
        List<UUID> materialIds = chunks.stream()
                .map(KnowledgeChunkEntity::getMaterialId)
                .distinct()
                .toList();
        Map<UUID, MaterialEntity> materialMap = new LinkedHashMap<>();
        for (MaterialEntity material : materialRepository.findAllById(materialIds)) {
            materialMap.put(material.getId(), material);
        }
        return materialMap;
    }

    private boolean isKnowledgeEligible(MaterialEntity material, AiSemanticParseResultEntity parseResult) {
        return material.getParseStage() == ParseStage.AI_PARSED
                && parseResult != null
                && hasUsefulText(material, parseResult);
    }

    private boolean hasUsefulText(MaterialEntity material, AiSemanticParseResultEntity parseResult) {
        return hasText(material.getPlainTextContent())
                || hasText(material.getSupplementText())
                || hasText(parseResult.getSummary())
                || hasText(parseResult.getTopicRelation())
                || !readStringList(parseResult.getDetectedClaimsJson()).isEmpty()
                || !readStringList(parseResult.getDetectedEvidenceJson()).isEmpty()
                || !readStringList(parseResult.getDetectedRequirementsJson()).isEmpty();
    }

    private double lexicalScore(String query, KnowledgeChunkEntity chunk) {
        String text = normalizeForSearch(chunk.getChunkText());
        String normalizedQuery = normalizeForSearch(query);
        if (text.isBlank() || normalizedQuery.isBlank()) {
            return 0;
        }

        double score = text.contains(normalizedQuery) ? 1.0d : 0;
        List<String> queryTokens = extractKeywords(query);
        List<String> chunkKeywords = readStringList(chunk.getKeywordsJson());
        for (String token : queryTokens) {
            String normalizedToken = normalizeForSearch(token);
            if (normalizedToken.length() < 2) {
                continue;
            }
            if (text.contains(normalizedToken)) {
                score += 0.22d;
            }
            if (chunkKeywords.stream().map(this::normalizeForSearch).anyMatch(normalizedToken::equals)) {
                score += 0.18d;
            }
        }
        return Math.min(score, 1.0d);
    }

    private KnowledgeChunkResponse toResponse(KnowledgeChunkEntity chunk, MaterialEntity material, double score) {
        return new KnowledgeChunkResponse(
                chunk.getId(),
                chunk.getWorkspaceId(),
                chunk.getMaterialId(),
                material == null ? "未知材料" : material.getFilename(),
                chunk.getChunkIndex(),
                chunk.getChunkText(),
                chunk.getSourceExcerpt(),
                readStringList(chunk.getKeywordsJson()),
                roundScore(score),
                chunk.getCreatedAt()
        );
    }

    private List<String> splitIntoChunks(String text) {
        String normalized = cleanChunkText(text);
        if (normalized.isBlank()) {
            return List.of();
        }
        if (normalized.length() <= CHUNK_SIZE) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(start + CHUNK_SIZE, normalized.length());
            if (end < normalized.length()) {
                int paragraphBreak = normalized.lastIndexOf("\n", end);
                if (paragraphBreak > start + 300) {
                    end = paragraphBreak;
                }
            }
            chunks.add(normalized.substring(start, end).trim());
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(0, end - CHUNK_OVERLAP);
        }
        return chunks;
    }

    private List<String> extractKeywords(String text) {
        String cleaned = cleanChunkText(text);
        if (cleaned.isBlank()) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = KEYWORD_PATTERN.matcher(cleaned);
        while (matcher.find()) {
            String token = matcher.group().trim().toLowerCase(Locale.ROOT);
            if (token.length() < 2 || STOPWORDS.contains(token)) {
                continue;
            }
            keywords.add(token);
            if (keywords.size() >= 12) {
                break;
            }
        }
        return new ArrayList<>(keywords);
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<?> rawItems = objectMapper.readValue(json, List.class);
            List<String> items = new ArrayList<>();
            for (Object item : rawItems) {
                String itemText = item instanceof Map<?, ?> map ? flattenMap(map) : String.valueOf(item);
                itemText = cleanChunkText(itemText);
                if (!itemText.isBlank()) {
                    items.add(itemText);
                }
            }
            return items;
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    private BibliographicMetadata readBibliographicMetadata(AiSemanticParseResultEntity parseResult) {
        if (parseResult == null
                || parseResult.getBibliographicMetadataJson() == null
                || parseResult.getBibliographicMetadataJson().isBlank()) {
            return BibliographicMetadata.empty();
        }
        try {
            return objectMapper.readValue(parseResult.getBibliographicMetadataJson(), BibliographicMetadata.class);
        } catch (JsonProcessingException e) {
            return BibliographicMetadata.empty();
        }
    }

    private String formatBibliographicMetadata(BibliographicMetadata metadata) {
        if (metadata == null) {
            return "";
        }
        List<String> parts = new ArrayList<>();
        if (metadata.authors() != null && !metadata.authors().isEmpty()) {
            parts.add(String.join("、", metadata.authors()));
        }
        appendPlain(parts, metadata.year());
        appendPlain(parts, metadata.title());
        appendPlain(parts, metadata.sourceTitle());
        appendPlain(parts, metadata.publisher());
        appendPlain(parts, metadata.doi() == null ? null : "DOI：" + metadata.doi());
        appendPlain(parts, metadata.url());
        return cleanChunkText(String.join("，", parts));
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "知识库 JSON 序列化失败");
        }
    }

    private void assertWorkspaceExists(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new BusinessException(
                    ErrorCode.WORKSPACE_NOT_FOUND,
                    HttpStatus.NOT_FOUND.value(),
                    "workspace 不存在"
            );
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return 8;
        }
        return Math.max(1, Math.min(limit, 20));
    }

    private String buildMessage(int chunkCount) {
        if (chunkCount == 0) {
            return "当前没有已完成 AI 解析且可入库的材料，请先到解析状态页完成材料解析。";
        }
        return "已构建项目知识库，当前使用关键词检索。";
    }

    private String joinNonBlank(String... values) {
        List<String> parts = new ArrayList<>();
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                parts.add(value.trim());
            }
        }
        return String.join("\n\n", parts);
    }

    private String normalizeWhitespace(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }

    private String cleanChunkText(String text) {
        if (text == null) {
            return "";
        }
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFC)
                .replace('\u00A0', ' ')
                .replace('\uFFFD', ' ');
        normalized = normalized.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F\\u007F]", " ");
        normalized = normalized.replaceAll("!\\[[^]]*]\\([^)]*\\)", " ");
        normalized = normalized.replaceAll("\\[([^]]+)]\\([^)]*\\)", "$1");
        normalized = normalized.replaceAll("(?m)^\\s{0,8}#{1,6}\\s*", "");
        normalized = normalized.replaceAll("(?m)^\\s{0,8}>\\s?", "");
        normalized = normalized.replaceAll("(?m)^\\s{0,8}[-*+]\\s+", "");
        normalized = normalized.replaceAll("`{1,3}", "");
        normalized = normalized.replaceAll("\\*{1,3}([^*\\n]+)\\*{1,3}", "$1");
        normalized = MARKDOWN_TABLE_SEPARATOR.matcher(normalized).replaceAll(" ");
        normalized = normalized.replace('|', ' ');
        normalized = normalized.replaceAll("[_*=~]{3,}", " ");
        normalized = normalized.replaceAll("-{4,}", " ");
        normalized = normalized.replaceAll("(?<=[\\p{IsHan}])[ \\t]+(?=[\\p{IsHan}])", "");
        normalized = normalized.replaceAll("\\s+([，。；：、！？,.!?;:])", "$1");
        normalized = normalized.replaceAll("([（(])\\s+", "$1");
        normalized = normalized.replaceAll("\\s+([）)])", "$1");
        return normalizeWhitespace(normalized);
    }

    private boolean isReadableKnowledgeChunk(String text) {
        if (text == null || text.isBlank() || text.length() < MIN_READABLE_CHUNK_LENGTH) {
            return false;
        }
        if (SUSPICIOUS_SYMBOL_RUN.matcher(text).find()) {
            return false;
        }
        int meaningfulChars = 0;
        int visibleChars = 0;
        for (int i = 0; i < text.length(); i++) {
            char value = text.charAt(i);
            if (Character.isWhitespace(value)) {
                continue;
            }
            visibleChars += 1;
            if (Character.isLetterOrDigit(value) || Character.UnicodeScript.of(value) == Character.UnicodeScript.HAN) {
                meaningfulChars += 1;
            }
        }
        if (visibleChars == 0) {
            return false;
        }
        double meaningfulRatio = (double) meaningfulChars / visibleChars;
        return meaningfulRatio >= 0.45d;
    }

    private String normalizeForSearch(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
    }

    private String shorten(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = normalizeWhitespace(value);
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength - 1) + "...";
    }

    private void appendIfPresent(List<String> lines, String label, String value) {
        String cleaned = cleanChunkText(value);
        if (!cleaned.isBlank()) {
            lines.add(label + "：" + cleaned);
        }
    }

    private void appendList(List<String> lines, String label, List<String> values) {
        for (String value : values) {
            String cleaned = cleanChunkText(value);
            if (!cleaned.isBlank()) {
                lines.add(label + "：" + cleaned);
            }
        }
    }

    private void appendPlain(List<String> parts, String value) {
        String cleaned = cleanChunkText(value);
        if (!cleaned.isBlank()) {
            parts.add(cleaned);
        }
    }

    private String flattenMap(Map<?, ?> map) {
        List<String> values = new ArrayList<>();
        for (Object value : map.values()) {
            if (value == null) {
                continue;
            }
            if (value instanceof List<?> list) {
                list.stream()
                        .filter(item -> item != null && !String.valueOf(item).isBlank())
                        .map(String::valueOf)
                        .forEach(values::add);
            } else if (!String.valueOf(value).isBlank()) {
                values.add(String.valueOf(value));
            }
        }
        return String.join("，", values);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private double roundScore(double score) {
        return Math.round(score * 1000.0d) / 1000.0d;
    }

    private record SearchCandidate(KnowledgeChunkEntity chunk, double score) {
    }
}
