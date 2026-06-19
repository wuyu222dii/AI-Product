package com.aipm.cowriting.application.dto.requirement;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.Map;

public record CreateRequirementSnapshotRequest(
        @Size(max = 300, message = "topic 长度不能超过 300")
        String topic,
        @Min(value = 100, message = "wordCount 最小为 100")
        @Max(value = 50000, message = "wordCount 最大为 50000")
        Integer wordCount,
        OffsetDateTime deadline,
        @Size(max = 50, message = "citationStyle 长度不能超过 50")
        String citationStyle,
        Map<String, Object> specialRequirements
) {
}
