package com.aipm.cowriting.interfaces.rest;

import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import com.aipm.cowriting.application.service.WritingRiskApplicationService;
import com.aipm.cowriting.common.api.ApiResponse;
import com.aipm.cowriting.common.web.RequestMetaUtil;
import com.aipm.cowriting.common.web.RestConstants;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WritingRiskController {

    private final WritingRiskApplicationService writingRiskApplicationService;

    public WritingRiskController(WritingRiskApplicationService writingRiskApplicationService) {
        this.writingRiskApplicationService = writingRiskApplicationService;
    }

    @GetMapping(RestConstants.API_V1 + "/drafts/{id}/writing-risks")
    public ResponseEntity<ApiResponse<WritingRiskSummaryResponse>> get(
            @PathVariable("id") UUID draftId,
            HttpServletRequest httpServletRequest
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                writingRiskApplicationService.evaluate(draftId),
                RequestMetaUtil.meta(httpServletRequest)
        ));
    }
}
