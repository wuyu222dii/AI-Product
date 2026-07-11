package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.ReviewImpactLevel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_items")
public class ReviewItemEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

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
    private String issueFingerprint;

    @Column(nullable = false)
    private String reviewType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ReviewImpactLevel reviewImpactLevel;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String targetRangeJson;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String suggestedFix;

    @Column(nullable = false)
    private boolean canBypass;

    @Column(nullable = false, length = 32)
    private String reviewStatus = "OPEN";

    @Column(columnDefinition = "TEXT")
    private String resolutionNote;

    @Column
    private OffsetDateTime resolvedAt;

    @Column
    private OffsetDateTime lastRecheckedAt;

    @Column(columnDefinition = "TEXT")
    private String recheckNote;

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
    public String getIssueFingerprint() { return issueFingerprint; }
    public void setIssueFingerprint(String issueFingerprint) { this.issueFingerprint = issueFingerprint; }

    public String getReviewType() {
        return reviewType;
    }

    public void setReviewType(String reviewType) {
        this.reviewType = reviewType;
    }

    public ReviewImpactLevel getReviewImpactLevel() {
        return reviewImpactLevel;
    }

    public void setReviewImpactLevel(ReviewImpactLevel reviewImpactLevel) {
        this.reviewImpactLevel = reviewImpactLevel;
    }

    public String getTargetRangeJson() {
        return targetRangeJson;
    }

    public void setTargetRangeJson(String targetRangeJson) {
        this.targetRangeJson = targetRangeJson;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSuggestedFix() {
        return suggestedFix;
    }

    public void setSuggestedFix(String suggestedFix) {
        this.suggestedFix = suggestedFix;
    }

    public boolean isCanBypass() {
        return canBypass;
    }

    public void setCanBypass(boolean canBypass) {
        this.canBypass = canBypass;
    }

    public String getReviewStatus() {
        return reviewStatus;
    }

    public void setReviewStatus(String reviewStatus) {
        this.reviewStatus = reviewStatus;
    }

    public String getResolutionNote() {
        return resolutionNote;
    }

    public void setResolutionNote(String resolutionNote) {
        this.resolutionNote = resolutionNote;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public OffsetDateTime getLastRecheckedAt() {
        return lastRecheckedAt;
    }

    public void setLastRecheckedAt(OffsetDateTime lastRecheckedAt) {
        this.lastRecheckedAt = lastRecheckedAt;
    }

    public String getRecheckNote() {
        return recheckNote;
    }

    public void setRecheckNote(String recheckNote) {
        this.recheckNote = recheckNote;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
