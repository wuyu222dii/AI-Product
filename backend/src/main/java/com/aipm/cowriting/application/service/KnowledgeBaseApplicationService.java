package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.knowledge.KnowledgeBuildResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeChunkResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchRequest;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchResponse;
import com.aipm.cowriting.common.api.PagedResponse;
import com.aipm.cowriting.common.api.Pagination;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.KnowledgeChunkEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private static final Pattern KEYWORD_PATTERN = Pattern.compile("[\\p{IsHan}]{2,12}|[A-Za-z][A-Za-z0-9_-]{2,}|\\d+(?:\\.\\d+)?%?");
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "with", "this", "that", "from", "paper", "study", "research",
            "材料", "摘要", "主题", "关系", "观点", "证据", "要求", "原文", "内容", "可以", "进行"
    );

    private final WorkspaceRepository workspaceRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository aiSemanticParseResultRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final ObjectMapper objectMapper;

    public KnowledgeBaseApplicationService(
            WorkspaceRepository workspaceRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository aiSemanticParseResultRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            ObjectMapper objectMapper
    ) {
        this.workspaceRepository = workspaceRepository;
        this.materialRepository = materialRepository;
        this.aiSemanticParseResultRepository = aiSemanticParseResultRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
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
        List<KnowledgeChunkEntity> chunks = knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);

        List<SearchCandidate> candidates = chunks.stream()
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
            String normalized = normalizeWhitespace(sourceText);
            if (normalized.isBlank() || !seen.add(normalized)) {
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
        appendList(lines, "老师要求", readStringList(parseResult.getDetectedRequirementsJson()));
        if (parseResult.getBibliographicMetadataJson() != null && !parseResult.getBibliographicMetadataJson().isBlank()) {
            lines.add("文献信息：" + parseResult.getBibliographicMetadataJson());
        }
        return shorten(String.join("\n", lines), CHUNK_SIZE);
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
        String normalized = normalizeWhitespace(text);
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
        if (text == null || text.isBlank()) {
            return List.of();
        }
        Set<String> keywords = new LinkedHashSet<>();
        Matcher matcher = KEYWORD_PATTERN.matcher(text);
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
                if (item != null && !String.valueOf(item).isBlank()) {
                    items.add(String.valueOf(item));
                }
            }
            return items;
        } catch (JsonProcessingException e) {
            return List.of();
        }
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
        if (value != null && !value.isBlank()) {
            lines.add(label + "：" + value.trim());
        }
    }

    private void appendList(List<String> lines, String label, List<String> values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                lines.add(label + "：" + value.trim());
            }
        }
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
