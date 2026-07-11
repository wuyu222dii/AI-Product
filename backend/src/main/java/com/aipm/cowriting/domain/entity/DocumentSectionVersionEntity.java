package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(
        name = "document_section_versions",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_section_version", columnNames = {"section_id", "version_no"}),
        indexes = @Index(name = "idx_document_section_versions_latest", columnList = "section_id, version_no")
)
public class DocumentSectionVersionEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sectionId;

    @Column(nullable = false)
    private Integer versionNo;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> sourceTraceMapJson;

    @Column(nullable = false, length = 48)
    private String changeSource;

    @Column(columnDefinition = "TEXT")
    private String changeSummary;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer versionNo) { this.versionNo = versionNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Map<String, Object> getSourceTraceMapJson() { return sourceTraceMapJson; }
    public void setSourceTraceMapJson(Map<String, Object> sourceTraceMapJson) { this.sourceTraceMapJson = sourceTraceMapJson; }
    public String getChangeSource() { return changeSource; }
    public void setChangeSource(String changeSource) { this.changeSource = changeSource; }
    public String getChangeSummary() { return changeSummary; }
    public void setChangeSummary(String changeSummary) { this.changeSummary = changeSummary; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
