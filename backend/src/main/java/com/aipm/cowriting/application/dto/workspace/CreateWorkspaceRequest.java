package com.aipm.cowriting.application.dto.workspace;

import com.aipm.cowriting.application.dto.academic.AcademicProfileRequest;
import com.aipm.cowriting.application.dto.academic.CreateAcademicDocumentRequest;
import com.aipm.cowriting.application.dto.guide.ProjectGuideRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateWorkspaceRequest(
        @NotBlank(message = "title 不能为空")
        @Size(max = 120, message = "title 长度不能超过 120")
        String title,
        @Valid AcademicProfileRequest academicProfile,
        @Valid CreateAcademicDocumentRequest initialDocument,
        @Valid ProjectGuideRequest guideProfile
) {
    public CreateWorkspaceRequest(String title) {
        this(title, null, null, null);
    }

    public CreateWorkspaceRequest(
            String title,
            AcademicProfileRequest academicProfile,
            CreateAcademicDocumentRequest initialDocument
    ) {
        this(title, academicProfile, initialDocument, null);
    }
}
