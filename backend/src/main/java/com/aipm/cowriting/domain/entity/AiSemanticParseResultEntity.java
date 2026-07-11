package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.MaterialRole;
import com.aipm.cowriting.domain.model.ResearchArtifactType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_semantic_parse_results")
public class AiSemanticParseResultEntity {

    @Id
    private UUID id;

    @Column(nullable = false, unique = true)
    private UUID materialId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private MaterialCategory materialCategory;

    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private MaterialCategory manualMaterialCategory;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private MaterialRole materialRole;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private ResearchArtifactType researchArtifactType;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String materialTagsJson;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String topicRelation;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detectedClaimsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detectedEvidenceJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String detectedRequirementsJson;

    @Column(columnDefinition = "TEXT")
    private String bibliographicMetadataJson;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidenceScore;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public MaterialCategory getMaterialCategory() {
        return materialCategory;
    }

    public void setMaterialCategory(MaterialCategory materialCategory) {
        this.materialCategory = materialCategory;
    }

    public MaterialCategory getManualMaterialCategory() {
        return manualMaterialCategory;
    }

    public void setManualMaterialCategory(MaterialCategory manualMaterialCategory) {
        this.manualMaterialCategory = manualMaterialCategory;
    }

    public MaterialRole getMaterialRole() {
        return materialRole;
    }

    public void setMaterialRole(MaterialRole materialRole) {
        this.materialRole = materialRole;
    }

    public ResearchArtifactType getResearchArtifactType() {
        return researchArtifactType;
    }

    public void setResearchArtifactType(ResearchArtifactType researchArtifactType) {
        this.researchArtifactType = researchArtifactType;
    }

    public String getMaterialTagsJson() {
        return materialTagsJson;
    }

    public void setMaterialTagsJson(String materialTagsJson) {
        this.materialTagsJson = materialTagsJson;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getTopicRelation() {
        return topicRelation;
    }

    public void setTopicRelation(String topicRelation) {
        this.topicRelation = topicRelation;
    }

    public String getDetectedClaimsJson() {
        return detectedClaimsJson;
    }

    public void setDetectedClaimsJson(String detectedClaimsJson) {
        this.detectedClaimsJson = detectedClaimsJson;
    }

    public String getDetectedEvidenceJson() {
        return detectedEvidenceJson;
    }

    public void setDetectedEvidenceJson(String detectedEvidenceJson) {
        this.detectedEvidenceJson = detectedEvidenceJson;
    }

    public String getDetectedRequirementsJson() {
        return detectedRequirementsJson;
    }

    public void setDetectedRequirementsJson(String detectedRequirementsJson) {
        this.detectedRequirementsJson = detectedRequirementsJson;
    }

    public String getBibliographicMetadataJson() {
        return bibliographicMetadataJson;
    }

    public void setBibliographicMetadataJson(String bibliographicMetadataJson) {
        this.bibliographicMetadataJson = bibliographicMetadataJson;
    }

    public BigDecimal getConfidenceScore() {
        return confidenceScore;
    }

    public void setConfidenceScore(BigDecimal confidenceScore) {
        this.confidenceScore = confidenceScore;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
