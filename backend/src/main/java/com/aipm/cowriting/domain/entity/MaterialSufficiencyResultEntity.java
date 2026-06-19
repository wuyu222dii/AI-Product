package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "material_sufficiency_results")
public class MaterialSufficiencyResultEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private boolean isGenerationEligible;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String missingItemsJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String recommendedSupplementsJson;

    private String reason;

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

    public boolean isGenerationEligible() {
        return isGenerationEligible;
    }

    public void setGenerationEligible(boolean generationEligible) {
        isGenerationEligible = generationEligible;
    }

    public String getMissingItemsJson() {
        return missingItemsJson;
    }

    public void setMissingItemsJson(String missingItemsJson) {
        this.missingItemsJson = missingItemsJson;
    }

    public String getRecommendedSupplementsJson() {
        return recommendedSupplementsJson;
    }

    public void setRecommendedSupplementsJson(String recommendedSupplementsJson) {
        this.recommendedSupplementsJson = recommendedSupplementsJson;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
