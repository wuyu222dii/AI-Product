package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.literature.LiteratureCandidateRequest;
import com.aipm.cowriting.application.dto.literature.LiteratureCandidateResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.LiteratureCandidateEntity;
import com.aipm.cowriting.domain.repository.LiteratureCandidateRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LiteratureCandidateApplicationService {

    private static final TypeReference<List<String>> STRING_LIST_TYPE = new TypeReference<>() {
    };

    private final LiteratureCandidateRepository literatureCandidateRepository;
    private final WorkspaceRepository workspaceRepository;
    private final ObjectMapper objectMapper;

    public LiteratureCandidateApplicationService(
            LiteratureCandidateRepository literatureCandidateRepository,
            WorkspaceRepository workspaceRepository,
            ObjectMapper objectMapper
    ) {
        this.literatureCandidateRepository = literatureCandidateRepository;
        this.workspaceRepository = workspaceRepository;
        this.objectMapper = objectMapper;
    }

    public List<LiteratureCandidateResponse> list(UUID workspaceId) {
        assertWorkspaceExists(workspaceId);
        return literatureCandidateRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public LiteratureCandidateResponse save(UUID workspaceId, LiteratureCandidateRequest request) {
        assertWorkspaceExists(workspaceId);
        String title = clean(request == null ? null : request.title());
        if (title == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST_BODY, HttpStatus.BAD_REQUEST.value(), "候选文献标题不能为空");
        }
        String duplicateGroupKey = firstNonBlank(
                clean(request.duplicateGroupKey()),
                LiteratureQualityService.duplicateGroupKey(request.doi(), title)
        );
        LiteratureCandidateEntity entity = literatureCandidateRepository
                .findFirstByWorkspaceIdAndDuplicateGroupKey(workspaceId, duplicateGroupKey)
                .orElseGet(() -> {
                    LiteratureCandidateEntity created = new LiteratureCandidateEntity();
                    created.setId(UUID.randomUUID());
                    created.setWorkspaceId(workspaceId);
                    created.setCreatedAt(OffsetDateTime.now());
                    created.setStatus("TO_DOWNLOAD");
                    return created;
                });

        entity.setProvider(firstNonBlank(clean(request.provider()), "Unknown"));
        entity.setTitle(title);
        entity.setAuthorsJson(writeJson(request.authors() == null ? List.of() : request.authors()));
        entity.setYear(clean(request.year()));
        entity.setSourceTitle(clean(request.sourceTitle()));
        entity.setPublisher(clean(request.publisher()));
        entity.setDoi(clean(request.doi()));
        entity.setUrl(clean(request.url()));
        entity.setAbstractSnippet(clean(request.abstractSnippet()));
        entity.setCitationPreview(clean(request.citationPreview()));
        entity.setQualityScore(request.qualityScore() == null ? 0 : request.qualityScore());
        entity.setQualityLabel(firstNonBlank(clean(request.qualityLabel()), "需人工确认"));
        entity.setMatchedReasonsJson(writeJson(request.matchedReasons() == null ? List.of() : request.matchedReasons()));
        entity.setMissingMetadataJson(writeJson(request.missingMetadata() == null ? List.of() : request.missingMetadata()));
        entity.setDuplicateGroupKey(duplicateGroupKey);
        entity.setRecommendedUse(clean(request.recommendedUse()));
        entity.setUpdatedAt(OffsetDateTime.now());
        return toResponse(literatureCandidateRepository.save(entity));
    }

    public void linkCandidateToMaterial(UUID workspaceId, UUID candidateId, UUID materialId) {
        if (candidateId == null) {
            return;
        }
        LiteratureCandidateEntity entity = literatureCandidateRepository.findById(candidateId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "候选文献不存在"));
        if (!workspaceId.equals(entity.getWorkspaceId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN.value(), "候选文献不属于当前项目");
        }
        entity.setStatus("LINKED");
        entity.setMaterialId(materialId);
        entity.setUpdatedAt(OffsetDateTime.now());
        literatureCandidateRepository.save(entity);
    }

    private LiteratureCandidateResponse toResponse(LiteratureCandidateEntity entity) {
        return new LiteratureCandidateResponse(
                entity.getId(),
                entity.getWorkspaceId(),
                entity.getProvider(),
                entity.getTitle(),
                readStringList(entity.getAuthorsJson()),
                entity.getYear(),
                entity.getSourceTitle(),
                entity.getPublisher(),
                entity.getDoi(),
                entity.getUrl(),
                entity.getAbstractSnippet(),
                entity.getCitationPreview(),
                entity.getQualityScore(),
                entity.getQualityLabel(),
                readStringList(entity.getMatchedReasonsJson()),
                readStringList(entity.getMissingMetadataJson()),
                entity.getDuplicateGroupKey(),
                entity.getRecommendedUse(),
                entity.getStatus(),
                entity.getMaterialId(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private void assertWorkspaceExists(UUID workspaceId) {
        if (!workspaceRepository.existsById(workspaceId)) {
            throw new BusinessException(ErrorCode.WORKSPACE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "workspace 不存在");
        }
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, STRING_LIST_TYPE);
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }

    private String clean(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
