package com.aipm.cowriting.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "section_cowrite_preview_review_links",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_section_cowrite_preview_review_link",
                columnNames = {"section_cowrite_preview_id", "review_item_id"}
        ),
        indexes = {
                @Index(name = "idx_section_cowrite_preview_review_links_preview", columnList = "section_cowrite_preview_id"),
                @Index(name = "idx_section_cowrite_preview_review_links_review", columnList = "review_item_id")
        }
)
public class SectionCoWritePreviewReviewLinkEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID sectionCowritePreviewId;

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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getSectionCowritePreviewId() { return sectionCowritePreviewId; }
    public void setSectionCowritePreviewId(UUID sectionCowritePreviewId) { this.sectionCowritePreviewId = sectionCowritePreviewId; }
    public UUID getReviewItemId() { return reviewItemId; }
    public void setReviewItemId(UUID reviewItemId) { this.reviewItemId = reviewItemId; }
    public String getRelationType() { return relationType; }
    public void setRelationType(String relationType) { this.relationType = relationType; }
    public String getRelationReason() { return relationReason; }
    public void setRelationReason(String relationReason) { this.relationReason = relationReason; }
    public boolean isRecheckPrompted() { return recheckPrompted; }
    public void setRecheckPrompted(boolean recheckPrompted) { this.recheckPrompted = recheckPrompted; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
}
