package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "evidence_bindings",
        indexes = {
                @Index(name = "idx_evidence_bindings_draft_paragraph", columnList = "draft_version_id, paragraph_id"),
                @Index(name = "idx_evidence_bindings_material", columnList = "material_id"),
                @Index(name = "idx_evidence_bindings_section_version_paragraph", columnList = "section_id, section_version_no, paragraph_id")
        }
)
public class EvidenceBindingEntity {

    @Id
    private UUID id;

    @Column
    private UUID draftVersionId;

    @Column(nullable = false, length = 32)
    private String scopeType = "LEGACY_DRAFT";

    @Column
    private UUID documentId;

    @Column
    private UUID sectionId;

    @Column
    private Integer sectionVersionNo;

    @Column(length = 64)
    private String paragraphFingerprint;

    @Column(nullable = false)
    private String paragraphId;

    @Column
    private UUID knowledgeChunkId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String claimText;

    @Column
    private UUID materialId;

    @Column(columnDefinition = "TEXT")
    private String sourceExcerpt;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetRangeJson;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(nullable = false, length = 48)
    private String supportType;

    @Column(nullable = false, length = 32)
    private String bindingStatus;

    @Column(columnDefinition = "TEXT")
    private String citationText;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getDraftVersionId() {
        return draftVersionId;
    }

    public void setDraftVersionId(UUID draftVersionId) {
        this.draftVersionId = draftVersionId;
    }

    public String getScopeType() { return scopeType; }
    public void setScopeType(String scopeType) { this.scopeType = scopeType; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getSectionId() { return sectionId; }
    public void setSectionId(UUID sectionId) { this.sectionId = sectionId; }
    public Integer getSectionVersionNo() { return sectionVersionNo; }
    public void setSectionVersionNo(Integer sectionVersionNo) { this.sectionVersionNo = sectionVersionNo; }
    public String getParagraphFingerprint() { return paragraphFingerprint; }
    public void setParagraphFingerprint(String paragraphFingerprint) { this.paragraphFingerprint = paragraphFingerprint; }

    public String getParagraphId() {
        return paragraphId;
    }

    public void setParagraphId(String paragraphId) {
        this.paragraphId = paragraphId;
    }

    public UUID getKnowledgeChunkId() {
        return knowledgeChunkId;
    }

    public void setKnowledgeChunkId(UUID knowledgeChunkId) {
        this.knowledgeChunkId = knowledgeChunkId;
    }

    public String getClaimText() {
        return claimText;
    }

    public void setClaimText(String claimText) {
        this.claimText = claimText;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public String getSourceExcerpt() {
        return sourceExcerpt;
    }

    public void setSourceExcerpt(String sourceExcerpt) {
        this.sourceExcerpt = sourceExcerpt;
    }

    public String getTargetRangeJson() {
        return targetRangeJson;
    }

    public void setTargetRangeJson(String targetRangeJson) {
        this.targetRangeJson = targetRangeJson;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public String getSupportType() {
        return supportType;
    }

    public void setSupportType(String supportType) {
        this.supportType = supportType;
    }

    public String getBindingStatus() {
        return bindingStatus;
    }

    public void setBindingStatus(String bindingStatus) {
        this.bindingStatus = bindingStatus;
    }

    public String getCitationText() {
        return citationText;
    }

    public void setCitationText(String citationText) {
        this.citationText = citationText;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
