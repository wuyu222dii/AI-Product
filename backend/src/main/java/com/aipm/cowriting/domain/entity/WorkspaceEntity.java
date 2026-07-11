package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.WorkspaceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "workspaces")
public class WorkspaceEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private WorkspaceStatus status;

    private UUID currentDraftVersionId;

    private UUID activeDocumentId;

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public WorkspaceStatus getStatus() {
        return status;
    }

    public void setStatus(WorkspaceStatus status) {
        this.status = status;
    }

    public UUID getCurrentDraftVersionId() {
        return currentDraftVersionId;
    }

    public void setCurrentDraftVersionId(UUID currentDraftVersionId) {
        this.currentDraftVersionId = currentDraftVersionId;
    }

    public UUID getActiveDocumentId() {
        return activeDocumentId;
    }

    public void setActiveDocumentId(UUID activeDocumentId) {
        this.activeDocumentId = activeDocumentId;
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
