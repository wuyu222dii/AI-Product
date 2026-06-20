package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "cowrite_preview_review_links")
public class CoWritePreviewReviewLinkEntity {

    @Id
    private UUID id;

    @Column(name = "cowrite_preview_id", nullable = false)
    private UUID coWritePreviewId;

    @Column(nullable = false)
    private UUID reviewItemId;

    @Column(nullable = false, length = 40)
    private String relationType;

    @Column(columnDefinition = "TEXT")
    private String relationReason;

    @Column(nullable = false)
    private boolean recheckPrompted;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCoWritePreviewId() {
        return coWritePreviewId;
    }

    public void setCoWritePreviewId(UUID coWritePreviewId) {
        this.coWritePreviewId = coWritePreviewId;
    }

    public UUID getReviewItemId() {
        return reviewItemId;
    }

    public void setReviewItemId(UUID reviewItemId) {
        this.reviewItemId = reviewItemId;
    }

    public String getRelationType() {
        return relationType;
    }

    public void setRelationType(String relationType) {
        this.relationType = relationType;
    }

    public String getRelationReason() {
        return relationReason;
    }

    public void setRelationReason(String relationReason) {
        this.relationReason = relationReason;
    }

    public boolean isRecheckPrompted() {
        return recheckPrompted;
    }

    public void setRecheckPrompted(boolean recheckPrompted) {
        this.recheckPrompted = recheckPrompted;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
