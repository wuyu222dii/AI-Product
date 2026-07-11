package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

public record AcademicProfileResponse(
        UUID workspaceId,
        AcademicStage academicStage,
        DisciplineGroup disciplineGroup,
        ResearchParadigm researchParadigm,
        String primaryLanguage,
        String defaultCitationStyle,
        String institution,
        AiUsagePolicy aiUsagePolicy,
        Map<String, Object> aiPolicy,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
