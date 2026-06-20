package com.aipm.cowriting.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.evidence.EvidenceBindingItemResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceMaterialResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceParagraphResponse;
import com.aipm.cowriting.application.dto.evidence.CitationConsistencyReport;
import com.aipm.cowriting.application.dto.evidence.EvidenceCoverageReport;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.service.EvidenceBindingApplicationService;
import com.aipm.cowriting.application.service.EvidenceBindingRebuildJobService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = EvidenceBindingController.class)
@Import(GlobalExceptionHandler.class)
class EvidenceBindingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EvidenceBindingApplicationService evidenceBindingApplicationService;

    @MockBean
    private EvidenceBindingRebuildJobService evidenceBindingRebuildJobService;

    @Test
    void getShouldReturnParagraphEvidenceMap() throws Exception {
        UUID draftId = UUID.randomUUID();
        UUID bindingId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        EvidenceBindingItemResponse item = new EvidenceBindingItemResponse(
                bindingId,
                draftId,
                "p1",
                materialId,
                null,
                "reference.pdf",
                "正文段落",
                "证据片段",
                Map.of("type", "page_hint", "page", 3, "label", "第 3 页附近"),
                Map.of("start", 0, "end", 4),
                new BigDecimal("0.8200"),
                "SOURCE_TRACE",
                "CONFIRMED",
                "（作者，2024）",
                Map.of("year", "2024"),
                OffsetDateTime.now()
        );
        when(evidenceBindingApplicationService.get(draftId))
                .thenReturn(new EvidenceBindingSummaryResponse(
                        draftId,
                        List.of(new EvidenceParagraphResponse("p1", "正文段落", "CONFIRMED", List.of(item))),
                        List.of(),
                        List.of(new EvidenceMaterialResponse(materialId, "reference.pdf", "pdf", "upload", true, Map.of())),
                        List.of(),
                        new EvidenceCoverageReport(1, 1, 0, 0, 100, 100, "可信链健康", List.of("当前正文段落基本具备可追溯来源，可进入导出前格式检查。")),
                        new CitationConsistencyReport("READY", 1, 1, 0, 0, 0, List.of("正文引用、材料来源和文献信息当前未发现明显冲突。"))
                ));

        mockMvc.perform(get("/api/v1/drafts/{id}/evidence-bindings", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.paragraphs[0].bindingStatus").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paragraphs[0].bindings[0].citationText").value("（作者，2024）"))
                .andExpect(jsonPath("$.data.coverage.coverageRatio").value(100))
                .andExpect(jsonPath("$.data.citationConsistency.status").value("READY"));
    }

    @Test
    void rebuildShouldReturnAcceptedJob() throws Exception {
        UUID draftId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(evidenceBindingRebuildJobService.enqueue(draftId))
                .thenReturn(new JobResponse(jobId.toString(), "running"));

        mockMvc.perform(post("/api/v1/drafts/{id}/evidence-bindings/rebuild", draftId))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.status").value("running"));
    }

    @Test
    void updateStatusShouldReturnUserConfirmedBinding() throws Exception {
        UUID bindingId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        when(evidenceBindingApplicationService.updateStatus(bindingId, "USER_CONFIRMED"))
                .thenReturn(new EvidenceBindingItemResponse(
                        bindingId,
                        draftId,
                        "p1",
                        UUID.randomUUID(),
                        null,
                        "reference.pdf",
                        "正文段落",
                        "证据片段",
                        Map.of("type", "excerpt", "label", "材料摘录位置"),
                        Map.of(),
                        BigDecimal.ONE,
                        "SOURCE_TRACE",
                        "USER_CONFIRMED",
                        "（作者，2024）",
                        Map.of(),
                        OffsetDateTime.now()
                ));

        mockMvc.perform(patch("/api/v1/evidence-bindings/{id}/status", bindingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "USER_CONFIRMED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bindingStatus").value("USER_CONFIRMED"));
    }
}
