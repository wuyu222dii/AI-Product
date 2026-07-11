package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.constraints.Pattern;
import java.util.List;

public record ApplySectionCoWritePreviewRequest(
        @Pattern(regexp = "ALL|PARAGRAPHS|DIFF_ROWS") String mode,
        List<String> selectedIds
) {
    public String effectiveMode() {
        return mode == null || mode.isBlank() ? "ALL" : mode;
    }
}
