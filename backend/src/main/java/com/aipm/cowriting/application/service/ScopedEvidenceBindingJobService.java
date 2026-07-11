package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.model.ContentScope;
import com.aipm.cowriting.common.error.BusinessException;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class ScopedEvidenceBindingJobService {

    private static final Logger log = LoggerFactory.getLogger(ScopedEvidenceBindingJobService.class);

    private final ContentScopeResolverService scopeResolver;
    private final ScopedEvidenceBindingApplicationService evidenceService;
    private final JobApplicationService jobService;
    private final TaskExecutor taskExecutor;

    public ScopedEvidenceBindingJobService(
            ContentScopeResolverService scopeResolver,
            ScopedEvidenceBindingApplicationService evidenceService,
            JobApplicationService jobService,
            TaskExecutor taskExecutor
    ) {
        this.scopeResolver = scopeResolver;
        this.evidenceService = evidenceService;
        this.jobService = jobService;
        this.taskExecutor = taskExecutor;
    }

    public JobResponse enqueueSection(UUID sectionId) {
        ContentScope scope = scopeResolver.section(sectionId);
        UUID jobId = jobService.createJob(
                "section_evidence_bindings_rebuild",
                "running",
                scope.workspaceId(),
                Map.of("documentId", scope.documentId().toString(), "sectionId", sectionId.toString(), "sectionVersionNo", scope.revision())
        );
        taskExecutor.execute(() -> run(jobId, sectionId));
        return new JobResponse(jobId.toString(), "running");
    }

    private void run(UUID jobId, UUID sectionId) {
        try {
            jobService.markRunning(jobId, 15);
            EvidenceBindingSummaryResponse summary = evidenceService.rebuildSection(sectionId);
            jobService.markSuccess(jobId, Map.of(
                    "documentId", summary.documentId().toString(),
                    "sectionId", sectionId.toString(),
                    "sectionVersionNo", summary.sectionVersionNo(),
                    "paragraphCount", summary.paragraphs().size(),
                    "missingParagraphCount", summary.missingParagraphIds().size()
            ));
        } catch (Exception error) {
            log.error("Section evidence rebuild job failed: jobId={}, sectionId={}", jobId, sectionId, error);
            String message = error instanceof BusinessException ? error.getMessage() : "章节可信链后台重建失败，请稍后重试";
            jobService.markFailed(jobId, message, Map.of("sectionId", sectionId.toString()));
        }
    }
}
