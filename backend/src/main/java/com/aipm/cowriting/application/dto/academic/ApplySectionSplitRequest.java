package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record ApplySectionSplitRequest(
        @NotNull Integer baseVersionNo,
        @NotEmpty List<@Valid SectionSplitItem> sections
) {
}
