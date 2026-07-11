package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "section_cowrite_previews",
        indexes = @Index(name = "idx_section_cowrite_previews_section_created", columnList = "section_id, created_at")
)
public class SectionCoWritePreviewEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sectionId;

    @Column(nullable = false)
    private Integer baseVersionNo;

    @Column(nullable = false, length = 64)
    private String action;

    @Column(columnDefinition = "TEXT")
    private String instruction;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> controlsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String candidateContent;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String baseContent = "";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> targetRangeJson = Map.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private java.util.List<Map<String, Object>> diffRowsJson = java.util.List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private java.util.List<Map<String, Object>> paragraphDiffRowsJson = java.util.List.of();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> candidateSourceTraceMapJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> diffSummaryJson;

    @Column(nullable = false, length = 32)
    private String status;

    private UUID aiActionLogId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime appliedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public Integer getBaseVersionNo() { return baseVersionNo; }
    public void setBaseVersionNo(Integer baseVersionNo) { this.baseVersionNo = baseVersionNo; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }
    public Map<String, Object> getControlsJson() { return controlsJson; }
    public void setControlsJson(Map<String, Object> controlsJson) { this.controlsJson = controlsJson; }
    public String getCandidateContent() { return candidateContent; }
    public void setCandidateContent(String candidateContent) { this.candidateContent = candidateContent; }
    public String getBaseContent() { return baseContent; }
    public void setBaseContent(String baseContent) { this.baseContent = baseContent; }
    public Map<String, Object> getTargetRangeJson() { return targetRangeJson; }
    public void setTargetRangeJson(Map<String, Object> targetRangeJson) { this.targetRangeJson = targetRangeJson; }
    public java.util.List<Map<String, Object>> getDiffRowsJson() { return diffRowsJson; }
    public void setDiffRowsJson(java.util.List<Map<String, Object>> diffRowsJson) { this.diffRowsJson = diffRowsJson; }
    public java.util.List<Map<String, Object>> getParagraphDiffRowsJson() { return paragraphDiffRowsJson; }
    public void setParagraphDiffRowsJson(java.util.List<Map<String, Object>> paragraphDiffRowsJson) { this.paragraphDiffRowsJson = paragraphDiffRowsJson; }
    public Map<String, Object> getCandidateSourceTraceMapJson() { return candidateSourceTraceMapJson; }
    public void setCandidateSourceTraceMapJson(Map<String, Object> candidateSourceTraceMapJson) { this.candidateSourceTraceMapJson = candidateSourceTraceMapJson; }
    public Map<String, Object> getDiffSummaryJson() { return diffSummaryJson; }
    public void setDiffSummaryJson(Map<String, Object> diffSummaryJson) { this.diffSummaryJson = diffSummaryJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public UUID getAiActionLogId() { return aiActionLogId; }
    public void setAiActionLogId(UUID aiActionLogId) { this.aiActionLogId = aiActionLogId; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getAppliedAt() { return appliedAt; }
    public void setAppliedAt(OffsetDateTime appliedAt) { this.appliedAt = appliedAt; }
}
