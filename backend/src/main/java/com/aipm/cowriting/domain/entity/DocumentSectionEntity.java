package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.DocumentSectionStatus;
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
        name = "document_sections",
        indexes = {
                @Index(name = "idx_document_sections_tree", columnList = "document_id, parent_section_id, sort_order"),
                @Index(name = "idx_document_sections_status", columnList = "document_id, status")
        }
)
public class DocumentSectionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID documentId;

    private UUID parentSectionId;

    @Column(nullable = false)
    private Integer sortOrder;

    @Column(nullable = false, length = 64)
    private String sectionType;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    private Integer targetLength;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DocumentSectionStatus status;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> sourceTraceMapJson;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getParentSectionId() { return parentSectionId; }
    public void setParentSectionId(UUID parentSectionId) { this.parentSectionId = parentSectionId; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getSectionType() { return sectionType; }
    public void setSectionType(String sectionType) { this.sectionType = sectionType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getTargetLength() { return targetLength; }
    public void setTargetLength(Integer targetLength) { this.targetLength = targetLength; }
    public DocumentSectionStatus getStatus() { return status; }
    public void setStatus(DocumentSectionStatus status) { this.status = status; }
    public Map<String, Object> getSourceTraceMapJson() { return sourceTraceMapJson; }
    public void setSourceTraceMapJson(Map<String, Object> sourceTraceMapJson) { this.sourceTraceMapJson = sourceTraceMapJson; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
