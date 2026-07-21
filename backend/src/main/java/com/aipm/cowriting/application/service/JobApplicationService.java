package com.aipm.cowriting.application.service;

import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class JobApplicationService {

    private final Map<UUID, Map<String, Object>> jobStore = new ConcurrentHashMap<>();
    private final CurrentUserService currentUserService;

    public JobApplicationService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public UUID createJob(String jobType, String status, UUID workspaceId) {
        return createJob(jobType, status, workspaceId, Map.of());
    }

    public UUID createJob(String jobType, String status, UUID workspaceId, Map<String, Object> inputRef) {
        UUID id = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", id);
        data.put("workspaceId", workspaceId);
        data.put("ownerUserId", currentUserService.userId());
        data.put("jobType", jobType);
        data.put("status", status);
        data.put("progress", initialProgress(status));
        data.put("inputRef", inputRef == null ? Map.of() : inputRef);
        data.put("outputRef", Map.of());
        data.put("errorMessage", null);
        data.put("createdAt", now.toString());
        data.put("updatedAt", now.toString());
        jobStore.put(id, data);
        return id;
    }

    public Map<String, Object> getJob(UUID jobId) {
        Map<String, Object> job = jobStore.get(jobId);
        if (job == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "job 不存在");
        }
        return job;
    }

    public Map<String, Object> getJobForCurrentUser(UUID jobId) {
        Map<String, Object> job = getJob(jobId);
        UUID ownerId = (UUID) job.get("ownerUserId");
        if (!currentUserService.userId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "job 不存在");
        }
        return job;
    }

    public void requireCurrentUser(UUID jobId) {
        getJobForCurrentUser(jobId);
    }

    public void attachOutput(UUID jobId, Map<String, Object> outputRef) {
        Map<String, Object> job = getJob(jobId);
        job.put("outputRef", outputRef);
        job.put("updatedAt", OffsetDateTime.now().toString());
    }

    public void updateProgress(UUID jobId, int progress) {
        Map<String, Object> job = getJob(jobId);
        job.put("progress", clampProgress(progress));
        job.put("updatedAt", OffsetDateTime.now().toString());
    }

    public void markRunning(UUID jobId, int progress) {
        Map<String, Object> job = getJob(jobId);
        job.put("status", "running");
        job.put("progress", clampProgress(progress));
        job.put("errorMessage", null);
        job.put("updatedAt", OffsetDateTime.now().toString());
    }

    public void markSuccess(UUID jobId, Map<String, Object> outputRef) {
        Map<String, Object> job = getJob(jobId);
        job.put("status", "success");
        job.put("progress", 100);
        job.put("outputRef", outputRef == null ? Map.of() : outputRef);
        job.put("errorMessage", null);
        job.put("updatedAt", OffsetDateTime.now().toString());
    }

    public void markFailed(UUID jobId, String errorMessage) {
        markFailed(jobId, errorMessage, Map.of());
    }

    public void markFailed(UUID jobId, String errorMessage, Map<String, Object> outputRef) {
        Map<String, Object> job = getJob(jobId);
        job.put("status", "failed");
        job.put("progress", 100);
        job.put("outputRef", outputRef == null ? Map.of() : outputRef);
        job.put("errorMessage", errorMessage == null || errorMessage.isBlank() ? "任务执行失败" : errorMessage);
        job.put("updatedAt", OffsetDateTime.now().toString());
    }

    private int initialProgress(String status) {
        if ("success".equals(status) || "failed".equals(status)) {
            return 100;
        }
        return "running".equals(status) ? 5 : 0;
    }

    private int clampProgress(int progress) {
        return Math.max(0, Math.min(100, progress));
    }
}
