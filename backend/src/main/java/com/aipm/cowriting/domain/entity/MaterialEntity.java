package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.ParseStage;
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
@Table(name = "materials")
public class MaterialEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID workspaceId;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false)
    private String fileType;

    @Column(nullable = false)
    private String sourceType;

    @Column(nullable = false)
    private String rawFileUrl;

    @Column
    private String storagePath;

    @Column(columnDefinition = "TEXT")
    private String plainTextContent;

    @Column(columnDefinition = "TEXT")
    private String supplementText;

    private String externalLink;

    @Column(nullable = false)
    private boolean isKeyMaterial;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ParseStage parseStage;

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

    public UUID getWorkspaceId() {
        return workspaceId;
    }

    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getRawFileUrl() {
        return rawFileUrl;
    }

    public void setRawFileUrl(String rawFileUrl) {
        this.rawFileUrl = rawFileUrl;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public String getPlainTextContent() {
        return plainTextContent;
    }

    public void setPlainTextContent(String plainTextContent) {
        this.plainTextContent = plainTextContent;
    }

    public String getExternalLink() {
        return externalLink;
    }

    public void setExternalLink(String externalLink) {
        this.externalLink = externalLink;
    }

    public String getSupplementText() {
        return supplementText;
    }

    public void setSupplementText(String supplementText) {
        this.supplementText = supplementText;
    }

    public boolean isKeyMaterial() {
        return isKeyMaterial;
    }

    public void setKeyMaterial(boolean keyMaterial) {
        isKeyMaterial = keyMaterial;
    }

    public ParseStage getParseStage() {
        return parseStage;
    }

    public void setParseStage(ParseStage parseStage) {
        this.parseStage = parseStage;
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
