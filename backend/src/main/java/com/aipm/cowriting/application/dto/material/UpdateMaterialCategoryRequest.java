package com.aipm.cowriting.application.dto.material;

import com.aipm.cowriting.domain.model.MaterialCategory;
import jakarta.validation.constraints.NotNull;

public record UpdateMaterialCategoryRequest(
        @NotNull(message = "materialCategory 不能为空")
        MaterialCategory materialCategory
) {
}
