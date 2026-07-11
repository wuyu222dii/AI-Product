package com.aipm.cowriting.application.dto.academic;

import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record AcademicProfileRequest(
        @NotNull AcademicStage academicStage,
        @NotNull DisciplineGroup disciplineGroup,
        @NotNull ResearchParadigm researchParadigm,
        @Size(max = 32) String primaryLanguage,
        @Size(max = 64) String defaultCitationStyle,
        @Size(max = 300) String institution,
        AiUsagePolicy aiUsagePolicy,
        Map<String, Object> aiPolicy
) {
}
