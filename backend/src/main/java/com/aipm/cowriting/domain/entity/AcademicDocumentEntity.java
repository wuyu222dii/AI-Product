package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
        name = "academic_documents",
        indexes = {
                @Index(name = "idx_academic_documents_workspace_status_updated", columnList = "workspace_id, status, updated_at"),
                @Index(name = "idx_academic_documents_workspace_type", columnList = "workspace_id, document_type")
        }
)
public class AcademicDocumentEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private AcademicDocumentType documentType;

    @Column(nullable = false, length = 300)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AcademicDocumentStatus status;

    @Column(length = 300)
    private String targetInstitution;

    @Column(length = 300)
    private String targetVenue;

    private Integer targetLength;

    @Column(nullable = false, length = 32)
    private String lengthUnit;

    @Column(nullable = false, length = 64)
    private String citationStyle;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> requirementProfileJson;

    @Column(nullable = false)
    private boolean primaryDocument;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public AcademicDocumentType getDocumentType() { return documentType; }
    public void setDocumentType(AcademicDocumentType documentType) { this.documentType = documentType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public AcademicDocumentStatus getStatus() { return status; }
    public void setStatus(AcademicDocumentStatus status) { this.status = status; }
    public String getTargetInstitution() { return targetInstitution; }
    public void setTargetInstitution(String targetInstitution) { this.targetInstitution = targetInstitution; }
    public String getTargetVenue() { return targetVenue; }
    public void setTargetVenue(String targetVenue) { this.targetVenue = targetVenue; }
    public Integer getTargetLength() { return targetLength; }
    public void setTargetLength(Integer targetLength) { this.targetLength = targetLength; }
    public String getLengthUnit() { return lengthUnit; }
    public void setLengthUnit(String lengthUnit) { this.lengthUnit = lengthUnit; }
    public String getCitationStyle() { return citationStyle; }
    public void setCitationStyle(String citationStyle) { this.citationStyle = citationStyle; }
    public Map<String, Object> getRequirementProfileJson() { return requirementProfileJson; }
    public void setRequirementProfileJson(Map<String, Object> requirementProfileJson) { this.requirementProfileJson = requirementProfileJson; }
    public boolean isPrimaryDocument() { return primaryDocument; }
    public void setPrimaryDocument(boolean primaryDocument) { this.primaryDocument = primaryDocument; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
