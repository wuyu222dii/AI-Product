package com.aipm.cowriting.application.dto.academic;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SectionCoWriteRequest(
        @NotBlank @Size(max = 64) String action,
        @Size(max = 4000) String instruction,
        Map<String, Object> controls,
        Map<String, Object> targetRange
) {
    public SectionCoWriteRequest(String action, String instruction, Map<String, Object> controls) {
        this(action, instruction, controls, Map.of());
    }
}
