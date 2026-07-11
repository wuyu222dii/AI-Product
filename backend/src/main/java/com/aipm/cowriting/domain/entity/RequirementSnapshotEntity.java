package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "requirement_snapshots")
public class RequirementSnapshotEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    private UUID documentId;

    @Column(nullable = false, length = 32)
    private String sourceType;

    private String topic;

    private Integer wordCount;

    private OffsetDateTime deadline;

    private String citationStyle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String specialRequirementsJson;

    @Column(nullable = false)
    private Integer version;

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

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public Integer getWordCount() {
        return wordCount;
    }

    public void setWordCount(Integer wordCount) {
        this.wordCount = wordCount;
    }

    public OffsetDateTime getDeadline() {
        return deadline;
    }

    public void setDeadline(OffsetDateTime deadline) {
        this.deadline = deadline;
    }

    public String getCitationStyle() {
        return citationStyle;
    }

    public void setCitationStyle(String citationStyle) {
        this.citationStyle = citationStyle;
    }

    public String getSpecialRequirementsJson() {
        return specialRequirementsJson;
    }

    public void setSpecialRequirementsJson(String specialRequirementsJson) {
        this.specialRequirementsJson = specialRequirementsJson;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
