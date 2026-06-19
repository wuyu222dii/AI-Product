package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.GenerationStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "draft_versions")
public class DraftVersionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private Integer versionNo;

    private String titleSuggestion;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String outlineJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String paragraphSkeletonsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String draftText;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String sourceTraceMapJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private GenerationStatus generationStatus;

    @Column(nullable = false)
    private String createdBy;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer versionNo) {
        this.versionNo = versionNo;
    }

    public String getTitleSuggestion() {
        return titleSuggestion;
    }

    public void setTitleSuggestion(String titleSuggestion) {
        this.titleSuggestion = titleSuggestion;
    }

    public String getOutlineJson() {
        return outlineJson;
    }

    public void setOutlineJson(String outlineJson) {
        this.outlineJson = outlineJson;
    }

    public String getParagraphSkeletonsJson() {
        return paragraphSkeletonsJson;
    }

    public void setParagraphSkeletonsJson(String paragraphSkeletonsJson) {
        this.paragraphSkeletonsJson = paragraphSkeletonsJson;
    }

    public String getDraftText() {
        return draftText;
    }

    public void setDraftText(String draftText) {
        this.draftText = draftText;
    }

    public String getSourceTraceMapJson() {
        return sourceTraceMapJson;
    }

    public void setSourceTraceMapJson(String sourceTraceMapJson) {
        this.sourceTraceMapJson = sourceTraceMapJson;
    }

    public GenerationStatus getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(GenerationStatus generationStatus) {
        this.generationStatus = generationStatus;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
