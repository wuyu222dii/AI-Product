package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.AssembledDocumentResponse;
import com.aipm.cowriting.application.dto.export.ExportRequest;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.model.GenerationStatus;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class AcademicDocumentExportService {

    private final AcademicDocumentApplicationService documentService;
    private final ExportApplicationService exportApplicationService;
    private final ObjectMapper objectMapper;

    public AcademicDocumentExportService(
            AcademicDocumentApplicationService documentService,
            ExportApplicationService exportApplicationService,
            ObjectMapper objectMapper
    ) {
        this.documentService = documentService;
        this.exportApplicationService = exportApplicationService;
        this.objectMapper = objectMapper;
    }

    public JobResponse export(UUID documentId, ExportRequest request) {
        AcademicDocumentEntity document = documentService.getDocument(documentId);
        AssembledDocumentResponse assembled = documentService.assemble(documentId);
        DraftVersionEntity snapshot = new DraftVersionEntity();
        snapshot.setId(UUID.randomUUID());
        snapshot.setWorkspaceId(document.getWorkspaceId());
        snapshot.setDocumentId(documentId);
        snapshot.setVersionNo(1);
        snapshot.setTitleSuggestion(document.getTitle());
        snapshot.setOutlineJson(writeJson(Map.of("sections", assembled.sectionIds())));
        snapshot.setParagraphSkeletonsJson(writeJson(List.of()));
        snapshot.setDraftText(assembled.content());
        snapshot.setSourceTraceMapJson(writeJson(assembled.sourceTraceMap()));
        snapshot.setGenerationStatus(GenerationStatus.SUCCESS);
        snapshot.setCreatedBy("document-assembly");
        snapshot.setCreatedAt(OffsetDateTime.now());
        return exportApplicationService.exportSnapshot(snapshot, request);
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
        }
    }
}
