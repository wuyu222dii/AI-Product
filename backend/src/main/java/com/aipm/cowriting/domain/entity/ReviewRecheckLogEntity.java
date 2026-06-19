package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "review_recheck_logs")
public class ReviewRecheckLogEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID reviewItemId;

    @Column(nullable = false)
    private UUID draftVersionId;

    @Column(nullable = false, length = 40)
    private String outcome;

    @Column(length = 32)
    private String previousStatus;

    @Column(length = 32)
    private String nextStatus;

    @Column(length = 32)
    private String previousImpactLevel;

    @Column(length = 32)
    private String nextImpactLevel;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(columnDefinition = "TEXT")
    private String basisJson;

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

    public UUID getDraftVersionId() {
        return draftVersionId;
    }

    public void setDraftVersionId(UUID draftVersionId) {
        this.draftVersionId = draftVersionId;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(String previousStatus) {
        this.previousStatus = previousStatus;
    }

    public String getNextStatus() {
        return nextStatus;
    }

    public void setNextStatus(String nextStatus) {
        this.nextStatus = nextStatus;
    }

    public String getPreviousImpactLevel() {
        return previousImpactLevel;
    }

    public void setPreviousImpactLevel(String previousImpactLevel) {
        this.previousImpactLevel = previousImpactLevel;
    }

    public String getNextImpactLevel() {
        return nextImpactLevel;
    }

    public void setNextImpactLevel(String nextImpactLevel) {
        this.nextImpactLevel = nextImpactLevel;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public String getBasisJson() {
        return basisJson;
    }

    public void setBasisJson(String basisJson) {
        this.basisJson = basisJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
