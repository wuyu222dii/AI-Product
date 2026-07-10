package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "literature_candidates")
public class LiteratureCandidateEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String provider;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String authorsJson;

    private String year;

    private String sourceTitle;

    private String publisher;

    private String doi;

    @Column(columnDefinition = "TEXT")
    private String url;

    @Column(columnDefinition = "TEXT")
    private String abstractSnippet;

    @Column(columnDefinition = "TEXT")
    private String citationPreview;

    private Integer qualityScore;

    private String qualityLabel;

    @Column(columnDefinition = "TEXT")
    private String matchedReasonsJson;

    @Column(columnDefinition = "TEXT")
    private String missingMetadataJson;

    private String duplicateGroupKey;

    @Column(columnDefinition = "TEXT")
    private String recommendedUse;

    @Column(nullable = false)
    private String status;

    private UUID materialId;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorsJson() {
        return authorsJson;
    }

    public void setAuthorsJson(String authorsJson) {
        this.authorsJson = authorsJson;
    }

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getDoi() {
        return doi;
    }

    public void setDoi(String doi) {
        this.doi = doi;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getAbstractSnippet() {
        return abstractSnippet;
    }

    public void setAbstractSnippet(String abstractSnippet) {
        this.abstractSnippet = abstractSnippet;
    }

    public String getCitationPreview() {
        return citationPreview;
    }

    public void setCitationPreview(String citationPreview) {
        this.citationPreview = citationPreview;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public String getQualityLabel() {
        return qualityLabel;
    }

    public void setQualityLabel(String qualityLabel) {
        this.qualityLabel = qualityLabel;
    }

    public String getMatchedReasonsJson() {
        return matchedReasonsJson;
    }

    public void setMatchedReasonsJson(String matchedReasonsJson) {
        this.matchedReasonsJson = matchedReasonsJson;
    }

    public String getMissingMetadataJson() {
        return missingMetadataJson;
    }

    public void setMissingMetadataJson(String missingMetadataJson) {
        this.missingMetadataJson = missingMetadataJson;
    }

    public String getDuplicateGroupKey() {
        return duplicateGroupKey;
    }

    public void setDuplicateGroupKey(String duplicateGroupKey) {
        this.duplicateGroupKey = duplicateGroupKey;
    }

    public String getRecommendedUse() {
        return recommendedUse;
    }

    public void setRecommendedUse(String recommendedUse) {
        this.recommendedUse = recommendedUse;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getMaterialId() {
        return materialId;
    }

    public void setMaterialId(UUID materialId) {
        this.materialId = materialId;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
