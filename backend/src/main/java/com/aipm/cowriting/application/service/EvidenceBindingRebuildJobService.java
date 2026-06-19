package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class EvidenceBindingRebuildJobService {

    private static final String JOB_TYPE = "evidence_bindings_rebuild";

    private final DraftVersionRepository draftVersionRepository;
    private final EvidenceBindingApplicationService evidenceBindingApplicationService;
    private final JobApplicationService jobApplicationService;
    private final TaskExecutor taskExecutor;

    public EvidenceBindingRebuildJobService(
            DraftVersionRepository draftVersionRepository,
            EvidenceBindingApplicationService evidenceBindingApplicationService,
            JobApplicationService jobApplicationService,
            TaskExecutor taskExecutor
    ) {
        this.draftVersionRepository = draftVersionRepository;
        this.evidenceBindingApplicationService = evidenceBindingApplicationService;
        this.jobApplicationService = jobApplicationService;
        this.taskExecutor = taskExecutor;
    }

    public JobResponse enqueue(UUID draftId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        UUID jobId = jobApplicationService.createJob(
                JOB_TYPE,
                "running",
                draft.getWorkspaceId(),
                Map.of("draftVersionId", draftId.toString())
        );

        taskExecutor.execute(() -> rebuildInBackground(jobId, draftId));
        return new JobResponse(jobId.toString(), "running");
    }

    private void rebuildInBackground(UUID jobId, UUID draftId) {
        try {
            jobApplicationService.markRunning(jobId, 15);
            EvidenceBindingSummaryResponse summary = evidenceBindingApplicationService.rebuild(draftId);
            jobApplicationService.markSuccess(jobId, Map.of(
                    "draftVersionId", draftId.toString(),
                    "paragraphCount", summary.paragraphs().size(),
                    "missingParagraphCount", summary.missingParagraphIds().size(),
                    "usedMaterialCount", summary.usedMaterials().size(),
                    "unusedMaterialCount", summary.unusedMaterials().size()
            ));
        } catch (Exception error) {
            jobApplicationService.markFailed(jobId, safeErrorMessage(error), Map.of("draftVersionId", draftId.toString()));
        }
    }

    private String safeErrorMessage(Exception error) {
        if (error instanceof BusinessException businessException) {
            return businessException.getMessage();
        }
        return "可信链后台重建失败，请稍后重试";
    }
}
