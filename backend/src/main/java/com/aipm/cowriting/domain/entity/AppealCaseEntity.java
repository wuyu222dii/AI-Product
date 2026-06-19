package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "appeal_cases")
public class AppealCaseEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID reviewItemId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String userReason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String evidenceJson;

    @Column(columnDefinition = "TEXT")
    private String reviewOutcome;

    private OffsetDateTime resolvedAt;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getReviewItemId() {
        return reviewItemId;
    }

    public void setReviewItemId(UUID reviewItemId) {
        this.reviewItemId = reviewItemId;
    }

    public String getUserReason() {
        return userReason;
    }

    public void setUserReason(String userReason) {
        this.userReason = userReason;
    }

    public String getEvidenceJson() {
        return evidenceJson;
    }

    public void setEvidenceJson(String evidenceJson) {
        this.evidenceJson = evidenceJson;
    }

    public String getReviewOutcome() {
        return reviewOutcome;
    }

    public void setReviewOutcome(String reviewOutcome) {
        this.reviewOutcome = reviewOutcome;
    }

    public OffsetDateTime getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(OffsetDateTime resolvedAt) {
        this.resolvedAt = resolvedAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
