package com.aipm.cowriting.interfaces.rest;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.writingrisk.WritingRiskItemResponse;
import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import com.aipm.cowriting.application.service.WritingRiskApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = WritingRiskController.class)
@Import(GlobalExceptionHandler.class)
@AuthenticatedApiTest
class WritingRiskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WritingRiskApplicationService writingRiskApplicationService;

    @Test
    void getWritingRisksShouldReturnParagraphRisks() throws Exception {
        UUID draftId = UUID.randomUUID();
        when(writingRiskApplicationService.evaluate(draftId))
                .thenReturn(new WritingRiskSummaryResponse(
                        draftId,
                        "NEEDS_REVIEW",
                        76,
                        List.of(new WritingRiskItemResponse(
                                "p1",
                                Map.of("start", 0, "end", 12, "selectedText", "空泛段落"),
                                "generic_unsupported_claim",
                                "LOCAL_FIX",
                                "空泛段落",
                                List.of("抽象评价较多，但缺少具体案例、数据或情境"),
                                "补充具体案例或数据。",
                                "请补充一个能支撑该判断的具体例子、数据或材料来源：",
                                "请将选中段落从抽象判断改为具体论证。"
                        )),
                        List.of("把抽象判断改成具体论证。")
                ));

        mockMvc.perform(get("/api/v1/drafts/{id}/writing-risks", draftId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallStatus").value("NEEDS_REVIEW"))
                .andExpect(jsonPath("$.data.items[0].riskType").value("generic_unsupported_claim"))
                .andExpect(jsonPath("$.data.items[0].coWriteInstruction").exists());
    }
}
