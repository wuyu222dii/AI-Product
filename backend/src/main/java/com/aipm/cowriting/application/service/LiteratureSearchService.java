package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.literature.ExternalSearchLink;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchRequest;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LiteratureSearchService {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 20;

    private final WorkspaceRepository workspaceRepository;
    private final RequirementSnapshotRepository requirementSnapshotRepository;
    private final MaterialRepository materialRepository;
    private final AiSemanticParseResultRepository parseResultRepository;
    private final CrossrefLiteratureClient crossrefLiteratureClient;
    private final OpenAlexLiteratureClient openAlexLiteratureClient;
    private final SemanticScholarLiteratureClient semanticScholarLiteratureClient;

    public LiteratureSearchService(
            WorkspaceRepository workspaceRepository,
            RequirementSnapshotRepository requirementSnapshotRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository parseResultRepository,
            CrossrefLiteratureClient crossrefLiteratureClient,
            OpenAlexLiteratureClient openAlexLiteratureClient,
            SemanticScholarLiteratureClient semanticScholarLiteratureClient
    ) {
        this.workspaceRepository = workspaceRepository;
        this.requirementSnapshotRepository = requirementSnapshotRepository;
        this.materialRepository = materialRepository;
        this.parseResultRepository = parseResultRepository;
        this.crossrefLiteratureClient = crossrefLiteratureClient;
        this.openAlexLiteratureClient = openAlexLiteratureClient;
        this.semanticScholarLiteratureClient = semanticScholarLiteratureClient;
    }

    public LiteratureSearchResponse search(UUID workspaceId, LiteratureSearchRequest request) {
        WorkspaceEntity workspace = workspaceRepository.findById(workspaceId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.WORKSPACE_NOT_FOUND,
                        HttpStatus.NOT_FOUND.value(),
                        "workspace 不存在"
                ));
        int limit = normalizeLimit(request == null ? null : request.limit());
        String missingItemType = request == null ? null : request.missingItemType();
        String query = firstNonBlank(
                request == null ? null : request.query(),
                defaultQuery(workspace, missingItemType, request == null ? null : request.searchIntent())
        );
        LiteratureSearchRequest effectiveRequest = request == null
                ? new LiteratureSearchRequest(query, "crossref", limit, missingItemType, List.of("crossref"), null, null, List.of(), null, null)
                : request;
        Map<String, String> providerStatus = new LinkedHashMap<>();
        List<LiteratureSearchItem> rawItems = new ArrayList<>();

        for (String provider : providers(effectiveRequest)) {
            rawItems.addAll(searchProvider(provider, query, limit, effectiveRequest, providerStatus));
        }
        List<LiteratureSearchItem> items = LiteratureQualityService.enrichAndDedupe(rawItems, effectiveRequest).stream()
                .limit(limit)
                .toList();

        return new LiteratureSearchResponse(
                query,
                items,
                externalLinks(query),
                providerStatus
        );
    }

    private List<String> providers(LiteratureSearchRequest request) {
        if (request.providers() != null && !request.providers().isEmpty()) {
            return request.providers().stream()
                    .map(this::normalizeProvider)
                    .filter(provider -> !provider.isBlank())
                    .distinct()
                    .toList();
        }
        String legacySource = normalizeProvider(request.source());
        if (!legacySource.isBlank()) {
            return List.of(legacySource);
        }
        return List.of("crossref", "openalex");
    }

    private List<LiteratureSearchItem> searchProvider(
            String provider,
            String query,
            int limit,
            LiteratureSearchRequest request,
            Map<String, String> providerStatus
    ) {
        try {
            List<LiteratureSearchItem> items = switch (provider) {
                case "crossref" -> crossrefLiteratureClient.search(query, limit, request);
                case "openalex" -> openAlexLiteratureClient.search(query, limit, request);
                case "semantic_scholar" -> semanticScholarLiteratureClient.search(query, limit, request);
                default -> List.of();
            };
            providerStatus.put(provider, items.isEmpty() ? "EMPTY" : "SUCCESS");
            return items;
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            providerStatus.put(provider, "FAILED");
            return List.of();
        } catch (Exception ex) {
            providerStatus.put(provider, "FAILED");
            return List.of();
        }
    }

    private String normalizeProvider(String provider) {
        if (provider == null || provider.isBlank()) {
            return "";
        }
        String normalized = provider.trim().toLowerCase().replace("-", "_").replace(" ", "_");
        if ("semantic".equals(normalized) || "semanticscholar".equals(normalized)) {
            return "semantic_scholar";
        }
        return normalized;
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String defaultQuery(WorkspaceEntity workspace, String missingItemType, String searchIntent) {
        List<String> parts = new ArrayList<>();
        add(parts, workspace.getTitle());
        requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspace.getId())
                .map(RequirementSnapshotEntity::getTopic)
                .ifPresent(topic -> add(parts, topic));
        addMaterialSummaries(parts, workspace.getId());
        if ("reference_material".equalsIgnoreCase(missingItemType)) {
            add(parts, "research literature");
        }
        add(parts, intentKeyword(searchIntent));
        if (parts.isEmpty()) {
            return "academic writing research";
        }
        return String.join(" ", parts);
    }

    private void addMaterialSummaries(List<String> parts, UUID workspaceId) {
        List<MaterialEntity> materials = materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId);
        if (materials.isEmpty()) {
            return;
        }
        List<UUID> materialIds = materials.stream().map(MaterialEntity::getId).toList();
        List<AiSemanticParseResultEntity> parseResults = parseResultRepository.findByMaterialIdIn(materialIds);
        parseResults.stream()
                .map(AiSemanticParseResultEntity::getSummary)
                .filter(summary -> summary != null && !summary.isBlank())
                .limit(2)
                .forEach(summary -> add(parts, summary));
    }

    private void add(List<String> parts, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() > 80) {
            normalized = normalized.substring(0, 80);
        }
        if (!parts.contains(normalized)) {
            parts.add(normalized);
        }
    }

    private String intentKeyword(String searchIntent) {
        return switch (String.valueOf(searchIntent).toLowerCase()) {
            case "theory" -> "theoretical framework literature review";
            case "method" -> "methodology empirical study";
            case "case" -> "case study";
            case "data" -> "dataset survey empirical evidence";
            default -> null;
        };
    }

    private List<ExternalSearchLink> externalLinks(String query) {
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);
        return List.of(
                new ExternalSearchLink("Google Scholar", "https://scholar.google.com/scholar?q=" + encoded),
                new ExternalSearchLink("CNKI", "https://oversea.cnki.net/kns/defaultresult/index?kw=" + encoded),
                new ExternalSearchLink("Crossref", "https://search.crossref.org/?q=" + encoded)
        );
    }

    private String firstNonBlank(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred.replaceAll("\\s+", " ").trim();
        }
        return fallback;
    }
}
