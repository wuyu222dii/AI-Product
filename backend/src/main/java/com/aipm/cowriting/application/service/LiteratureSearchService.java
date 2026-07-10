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

    public LiteratureSearchService(
            WorkspaceRepository workspaceRepository,
            RequirementSnapshotRepository requirementSnapshotRepository,
            MaterialRepository materialRepository,
            AiSemanticParseResultRepository parseResultRepository,
            CrossrefLiteratureClient crossrefLiteratureClient
    ) {
        this.workspaceRepository = workspaceRepository;
        this.requirementSnapshotRepository = requirementSnapshotRepository;
        this.materialRepository = materialRepository;
        this.parseResultRepository = parseResultRepository;
        this.crossrefLiteratureClient = crossrefLiteratureClient;
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
        String query = firstNonBlank(request == null ? null : request.query(), defaultQuery(workspace, missingItemType));
        Map<String, String> providerStatus = new LinkedHashMap<>();
        List<LiteratureSearchItem> items = List.of();

        if (shouldSearchCrossref(request == null ? null : request.source())) {
            try {
                items = crossrefLiteratureClient.search(query, limit);
                providerStatus.put("crossref", items.isEmpty() ? "EMPTY" : "SUCCESS");
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                providerStatus.put("crossref", "FAILED");
                items = List.of();
            } catch (Exception ex) {
                providerStatus.put("crossref", "FAILED");
                items = List.of();
            }
        } else {
            providerStatus.put("crossref", "SKIPPED");
        }

        return new LiteratureSearchResponse(
                query,
                items,
                externalLinks(query),
                providerStatus
        );
    }

    private boolean shouldSearchCrossref(String source) {
        return source == null || source.isBlank() || "crossref".equalsIgnoreCase(source);
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    private String defaultQuery(WorkspaceEntity workspace, String missingItemType) {
        List<String> parts = new ArrayList<>();
        add(parts, workspace.getTitle());
        requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspace.getId())
                .map(RequirementSnapshotEntity::getTopic)
                .ifPresent(topic -> add(parts, topic));
        addMaterialSummaries(parts, workspace.getId());
        if ("reference_material".equalsIgnoreCase(missingItemType)) {
            add(parts, "research literature");
        }
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
