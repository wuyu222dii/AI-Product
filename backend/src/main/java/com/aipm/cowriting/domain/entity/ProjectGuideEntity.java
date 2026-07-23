package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.GuideMode;
import com.aipm.cowriting.domain.model.GuideProgress;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "project_guides")
public class ProjectGuideEntity {

    @Id
    private UUID workspaceId;

    @Column(nullable = false, length = 32)
    private String guideVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private GuideProgress currentProgress;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> availableMaterials;

    private LocalDate targetDeadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private GuideMode preferredMode;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public String getGuideVersion() { return guideVersion; }
    public void setGuideVersion(String guideVersion) { this.guideVersion = guideVersion; }
    public GuideProgress getCurrentProgress() { return currentProgress; }
    public void setCurrentProgress(GuideProgress currentProgress) { this.currentProgress = currentProgress; }
    public List<String> getAvailableMaterials() { return availableMaterials; }
    public void setAvailableMaterials(List<String> availableMaterials) { this.availableMaterials = availableMaterials; }
    public LocalDate getTargetDeadline() { return targetDeadline; }
    public void setTargetDeadline(LocalDate targetDeadline) { this.targetDeadline = targetDeadline; }
    public GuideMode getPreferredMode() { return preferredMode; }
    public void setPreferredMode(GuideMode preferredMode) { this.preferredMode = preferredMode; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
