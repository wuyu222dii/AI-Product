package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AcademicDocumentResponse(
        UUID id,
        UUID workspaceId,
        AcademicDocumentType documentType,
        String title,
        AcademicDocumentStatus status,
        String targetInstitution,
        String targetVenue,
        Integer targetLength,
        String lengthUnit,
        String citationStyle,
        Map<String, Object> requirementProfile,
        boolean primaryDocument,
        int sectionCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
