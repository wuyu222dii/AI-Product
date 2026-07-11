package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record UpdateAcademicDocumentRequest(
        @Size(max = 300) String title,
        AcademicDocumentStatus status,
        @Size(max = 300) String targetInstitution,
        @Size(max = 300) String targetVenue,
        @Min(100) @Max(300000) Integer targetLength,
        @Size(max = 32) String lengthUnit,
        @Size(max = 64) String citationStyle,
        Map<String, Object> requirementProfile
) {
}
