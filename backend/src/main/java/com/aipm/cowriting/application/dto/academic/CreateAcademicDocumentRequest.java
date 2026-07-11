package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.AcademicDocumentType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record CreateAcademicDocumentRequest(
        @NotBlank @Size(max = 300) String title,
        @NotNull AcademicDocumentType documentType,
        @Size(max = 300) String targetInstitution,
        @Size(max = 300) String targetVenue,
        @Min(100) @Max(300000) Integer targetLength,
        @Size(max = 32) String lengthUnit,
        @Size(max = 64) String citationStyle,
        Map<String, Object> requirementProfile,
        Boolean primaryDocument
) {
}
