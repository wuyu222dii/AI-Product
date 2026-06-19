package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.service.ReviewApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.domain.model.ReviewImpactLevel;
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

@WebMvcTest(controllers = ReviewController.class)
@Import(GlobalExceptionHandler.class)
class ReviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ReviewApplicationService reviewApplicationService;

    @Test
    void updateStatusShouldReturnUpdatedReviewItem() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewApplicationService.updateStatus(eq(reviewId), any()))
                .thenReturn(new ReviewItemResponse(
                        reviewId,
                        "missing_evidence",
                        ReviewImpactLevel.LOCAL_FIX,
                        Map.of(),
                        "缺少证据",
                        "补充来源说明",
                        true,
                        "RESOLVED",
                        "已经补充证据",
                        OffsetDateTime.now(),
                        null,
                        null,
                        List.of(),
                        OffsetDateTime.now()
                ));

        mockMvc.perform(patch("/api/v1/review-items/{id}/status", reviewId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "RESOLVED",
                                  "resolutionNote": "已经补充证据"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("RESOLVED"))
                .andExpect(jsonPath("$.data.resolutionNote").value("已经补充证据"));
    }

    @Test
    void recheckShouldReturnUpdatedReviewItem() throws Exception {
        UUID reviewId = UUID.randomUUID();
        when(reviewApplicationService.recheck(reviewId))
                .thenReturn(new ReviewItemResponse(
                        reviewId,
                        "missing_evidence",
                        ReviewImpactLevel.LOCAL_FIX,
                        Map.of(),
                        "缺少证据",
                        "补充来源说明",
                        true,
                        "OPEN",
                        null,
                        null,
                        OffsetDateTime.now(),
                        "STILL_OPEN：仍缺少证据",
                        List.of(),
                        OffsetDateTime.now()
                ));

        mockMvc.perform(post("/api/v1/review-items/{id}/recheck", reviewId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reviewStatus").value("OPEN"))
                .andExpect(jsonPath("$.data.recheckNote").value("STILL_OPEN：仍缺少证据"));
    }
}
