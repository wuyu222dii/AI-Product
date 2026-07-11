package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.model.ContentScope;
import com.aipm.cowriting.common.error.BusinessException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;

@Service
public class AcademicReviewJobService {

    private static final Logger log = LoggerFactory.getLogger(AcademicReviewJobService.class);

    private final ContentScopeResolverService scopeResolver;
    private final AcademicReviewApplicationService reviewService;
    private final JobApplicationService jobService;
    private final TaskExecutor taskExecutor;

    public AcademicReviewJobService(
            ContentScopeResolverService scopeResolver,
            AcademicReviewApplicationService reviewService,
            JobApplicationService jobService,
            TaskExecutor taskExecutor
    ) {
        this.scopeResolver = scopeResolver;
        this.reviewService = reviewService;
        this.jobService = jobService;
        this.taskExecutor = taskExecutor;
    }

    public JobResponse enqueueSection(UUID sectionId) {
        ContentScope scope = scopeResolver.section(sectionId);
        UUID jobId = jobService.createJob("section_review_pass", "running", scope.workspaceId(), Map.of(
                "documentId", scope.documentId().toString(), "sectionId", sectionId.toString(), "sectionVersionNo", scope.revision()
        ));
        taskExecutor.execute(() -> runSection(jobId, sectionId));
        return new JobResponse(jobId.toString(), "running");
    }

    public JobResponse enqueueDocument(UUID documentId) {
        ContentScope scope = scopeResolver.document(documentId);
        UUID jobId = jobService.createJob("document_review_pass", "running", scope.workspaceId(), Map.of("documentId", documentId.toString()));
        taskExecutor.execute(() -> runDocument(jobId, documentId));
        return new JobResponse(jobId.toString(), "running");
    }

    private void runSection(UUID jobId, UUID sectionId) {
        try {
            jobService.markRunning(jobId, 15);
            List<ReviewItemResponse> items = reviewService.refreshSection(sectionId);
            jobService.markSuccess(jobId, Map.of("sectionId", sectionId.toString(), "reviewItemCount", items.size()));
        } catch (Exception error) {
            fail(jobId, error, Map.of("sectionId", sectionId.toString()));
        }
    }

    private void runDocument(UUID jobId, UUID documentId) {
        try {
            jobService.markRunning(jobId, 10);
            List<ReviewItemResponse> items = reviewService.refreshDocument(documentId);
            jobService.markSuccess(jobId, Map.of("documentId", documentId.toString(), "reviewItemCount", items.size()));
        } catch (Exception error) {
            fail(jobId, error, Map.of("documentId", documentId.toString()));
        }
    }

    private void fail(UUID jobId, Exception error, Map<String, Object> output) {
        log.error("Academic review job failed: jobId={}, output={}", jobId, output, error);
        String message = error instanceof BusinessException ? error.getMessage() : "AI 审查请求失败，请稍后重试";
        jobService.markFailed(jobId, message, output);
    }
}
