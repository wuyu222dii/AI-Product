package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "ai_action_logs",
        indexes = {
                @Index(name = "idx_ai_action_logs_document_created", columnList = "document_id, created_at"),
                @Index(name = "idx_ai_action_logs_workspace_created", columnList = "workspace_id, created_at")
        }
)
public class AiActionLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    private UUID documentId;
    private UUID sectionId;

    @Column(nullable = false, length = 64)
    private String actionType;

    @Column(length = 120)
    private String modelName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<UUID> evidenceMaterialIdsJson;

    @Column(columnDefinition = "TEXT")
    private String requestSummary;

    @Column(columnDefinition = "TEXT")
    private String outputSummary;

    private Boolean accepted;

    @Column(nullable = false)
    private boolean disclosureRequired;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }
    public String getModelName() { return modelName; }
    public void setModelName(String modelName) { this.modelName = modelName; }
    public List<UUID> getEvidenceMaterialIdsJson() { return evidenceMaterialIdsJson; }
    public void setEvidenceMaterialIdsJson(List<UUID> evidenceMaterialIdsJson) { this.evidenceMaterialIdsJson = evidenceMaterialIdsJson; }
    public String getRequestSummary() { return requestSummary; }
    public void setRequestSummary(String requestSummary) { this.requestSummary = requestSummary; }
    public String getOutputSummary() { return outputSummary; }
    public void setOutputSummary(String outputSummary) { this.outputSummary = outputSummary; }
    public Boolean getAccepted() { return accepted; }
    public void setAccepted(Boolean accepted) { this.accepted = accepted; }
    public boolean isDisclosureRequired() { return disclosureRequired; }
    public void setDisclosureRequired(boolean disclosureRequired) { this.disclosureRequired = disclosureRequired; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
