package com.aipm.cowriting.application.dto.guide;

import com.aipm.cowriting.domain.model.GuideMode;
import com.aipm.cowriting.domain.model.GuideProgress;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public record ProjectGuideRequest(
        @NotNull GuideProgress currentProgress,
        @NotNull @Size(max = 16) List<@Size(max = 48) String> availableMaterials,
        LocalDate targetDeadline,
        @NotNull GuideMode preferredMode
) {
}
