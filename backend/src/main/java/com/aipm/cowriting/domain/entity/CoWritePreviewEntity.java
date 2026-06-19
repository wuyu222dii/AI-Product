package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "cowrite_previews",
        indexes = {
                @Index(name = "idx_cowrite_previews_workspace_created", columnList = "workspace_id, created_at"),
                @Index(name = "idx_cowrite_previews_draft", columnList = "draft_version_id")
        }
)
public class CoWritePreviewEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private UUID draftVersionId;

    @Column(nullable = false)
    private String action;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetRangeJson;

    @Column(columnDefinition = "TEXT")
    private String instruction;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String controlsJson;

    @Column(columnDefinition = "TEXT")
    private String candidateTitleSuggestion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String candidateDraftText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String candidateSourceTraceMapJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String diffSummaryJson;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column
    private OffsetDateTime appliedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public UUID getDraftVersionId() {
        return draftVersionId;
    }

    public void setDraftVersionId(UUID draftVersionId) {
        this.draftVersionId = draftVersionId;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getTargetRangeJson() {
        return targetRangeJson;
    }

    public void setTargetRangeJson(String targetRangeJson) {
        this.targetRangeJson = targetRangeJson;
    }

    public String getInstruction() {
        return instruction;
    }

    public void setInstruction(String instruction) {
        this.instruction = instruction;
    }

    public String getControlsJson() {
        return controlsJson;
    }

    public void setControlsJson(String controlsJson) {
        this.controlsJson = controlsJson;
    }

    public String getCandidateTitleSuggestion() {
        return candidateTitleSuggestion;
    }

    public void setCandidateTitleSuggestion(String candidateTitleSuggestion) {
        this.candidateTitleSuggestion = candidateTitleSuggestion;
    }

    public String getCandidateDraftText() {
        return candidateDraftText;
    }

    public void setCandidateDraftText(String candidateDraftText) {
        this.candidateDraftText = candidateDraftText;
    }

    public String getCandidateSourceTraceMapJson() {
        return candidateSourceTraceMapJson;
    }

    public void setCandidateSourceTraceMapJson(String candidateSourceTraceMapJson) {
        this.candidateSourceTraceMapJson = candidateSourceTraceMapJson;
    }

    public String getDiffSummaryJson() {
        return diffSummaryJson;
    }

    public void setDiffSummaryJson(String diffSummaryJson) {
        this.diffSummaryJson = diffSummaryJson;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getAppliedAt() {
        return appliedAt;
    }

    public void setAppliedAt(OffsetDateTime appliedAt) {
        this.appliedAt = appliedAt;
    }
}
