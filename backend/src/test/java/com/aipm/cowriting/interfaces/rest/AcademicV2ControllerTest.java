package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.academic.AcademicDocumentResponse;
import com.aipm.cowriting.application.dto.academic.AcademicProfileResponse;
import com.aipm.cowriting.application.dto.academic.DocumentReadinessResponse;
import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.SectionCoWritePreviewResponse;
import com.aipm.cowriting.application.service.AcademicDocumentApplicationService;
import com.aipm.cowriting.application.service.AcademicDocumentExportService;
import com.aipm.cowriting.application.service.AcademicProfileApplicationService;
import com.aipm.cowriting.application.service.AcademicReadinessApplicationService;
import com.aipm.cowriting.application.service.AcademicSectionApplicationService;
import com.aipm.cowriting.application.service.AiActionLogApplicationService;
import com.aipm.cowriting.application.service.LegacySectionSplitApplicationService;
import com.aipm.cowriting.application.service.ScopedEvidenceBindingJobService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.DocumentSectionStatus;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = {AcademicDocumentController.class, AcademicProfileController.class, AcademicSectionController.class})
@Import(GlobalExceptionHandler.class)
@AuthenticatedApiTest
class AcademicV2ControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private AcademicDocumentApplicationService documentService;
    @MockBean private AcademicReadinessApplicationService readinessService;
    @MockBean private AiActionLogApplicationService actionLogService;
    @MockBean private AcademicDocumentExportService exportService;
    @MockBean private AcademicProfileApplicationService profileService;
    @MockBean private AcademicSectionApplicationService sectionService;
    @MockBean private ScopedEvidenceBindingJobService evidenceJobService;
    @MockBean private LegacySectionSplitApplicationService splitService;

    @Test
    void profileAndDocumentListShouldExposeAcademicProjectContext() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        when(profileService.getOrCreateDefault(workspaceId)).thenReturn(profile(workspaceId));
        when(documentService.list(workspaceId)).thenReturn(List.of(document(workspaceId, documentId)));

        mockMvc.perform(get("/api/v1/workspaces/{id}/academic-profile", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.academicStage").value("MASTER"))
                .andExpect(jsonPath("$.data.researchParadigm").value("QUANTITATIVE"));

        mockMvc.perform(get("/api/v1/workspaces/{id}/documents", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].documentType").value("MASTER_THESIS"))
                .andExpect(jsonPath("$.data[0].sectionCount").value(8));
    }

    @Test
    void readinessAndSectionAiEndpointsShouldReturnDocumentScopedResults() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        UUID previewId = UUID.randomUUID();
        when(readinessService.check(documentId)).thenReturn(new DocumentReadinessResponse(
                documentId, "READY", 97, true, List.of(),
                Map.of("parsedKeyMaterial", true, "literature", true), "可以按章节继续写作。"
        ));
        when(readinessService.checkSection(sectionId)).thenReturn(new DocumentReadinessResponse(
                documentId, "READY", 100, true, List.of(),
                Map.of("parsedKeyMaterial", true, "literature", true), "当前章节可以生成或共写。"
        ));
        when(sectionService.generate(eq(sectionId), any())).thenReturn(section(documentId, sectionId, "生成后的章节正文"));
        when(sectionService.previewCoWrite(eq(sectionId), any())).thenReturn(new SectionCoWritePreviewResponse(
                previewId, sectionId, 2, "improve_expression", "保持作者声音", Map.of("keepData", true),
                "共写候选正文", Map.of(), Map.of("characterDelta", 4), "READY", OffsetDateTime.now(), null
        ));

        mockMvc.perform(post("/api/v1/documents/{id}/readiness-check", documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationEligible").value(true))
                .andExpect(jsonPath("$.data.score").value(97));

        mockMvc.perform(post("/api/v1/sections/{id}/generate", sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"聚焦研究问题\",\"mode\":\"stable\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("生成后的章节正文"));

        mockMvc.perform(post("/api/v1/sections/{id}/readiness-check", sectionId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.generationEligible").value(true))
                .andExpect(jsonPath("$.data.nextAction").value("当前章节可以生成或共写。"));

        mockMvc.perform(post("/api/v1/sections/{id}/co-write/preview", sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"action\":\"improve_expression\",\"instruction\":\"保持作者声音\",\"controls\":{\"keepData\":true}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.candidateContent").value("共写候选正文"));
    }

    @Test
    void sectionVersionConstraintShouldReturnConflictInsteadOfInternalError() throws Exception {
        UUID sectionId = UUID.randomUUID();
        when(sectionService.generate(eq(sectionId), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate section version"));

        mockMvc.perform(post("/api/v1/sections/{id}/generate", sectionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"instruction\":\"继续生成\",\"mode\":\"stable\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("DATA_INTEGRITY_CONFLICT"))
                .andExpect(jsonPath("$.error.message").value("数据版本或唯一性发生冲突，请刷新后重试"));
    }

    @Test
    void documentSectionsShouldBeReorderable() throws Exception {
        UUID documentId = UUID.randomUUID();
        UUID firstSectionId = UUID.randomUUID();
        UUID secondSectionId = UUID.randomUUID();
        when(documentService.reorderSections(eq(documentId), any())).thenReturn(List.of(
                section(documentId, secondSectionId, "研究方法"),
                section(documentId, firstSectionId, "绪论")
        ));

        mockMvc.perform(patch("/api/v1/documents/{id}/sections/order", documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sectionIds\":[\"" + secondSectionId + "\",\"" + firstSectionId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(secondSectionId.toString()))
                .andExpect(jsonPath("$.data[0].content").value("研究方法"));
    }

    private AcademicProfileResponse profile(UUID workspaceId) {
        return new AcademicProfileResponse(
                workspaceId, AcademicStage.MASTER, DisciplineGroup.SOCIAL_SCIENCE,
                ResearchParadigm.QUANTITATIVE, "zh-CN", "APA", "示例大学",
                AiUsagePolicy.EVIDENCE_GROUNDED_DRAFTING, Map.of("disclosureRequired", true),
                OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    private AcademicDocumentResponse document(UUID workspaceId, UUID documentId) {
        return new AcademicDocumentResponse(
                documentId, workspaceId, AcademicDocumentType.MASTER_THESIS, "硕士论文",
                AcademicDocumentStatus.WRITING, "示例大学", null, 40000, "WORDS", "APA",
                Map.of(), true, 8, OffsetDateTime.now(), OffsetDateTime.now()
        );
    }

    private DocumentSectionResponse section(UUID documentId, UUID sectionId, String content) {
        return new DocumentSectionResponse(
                sectionId, documentId, null, 1, "INTRODUCTION", "绪论", content, 3000,
                DocumentSectionStatus.DRAFTING, Map.of(), 2, OffsetDateTime.now(), OffsetDateTime.now()
        );
    }
}
