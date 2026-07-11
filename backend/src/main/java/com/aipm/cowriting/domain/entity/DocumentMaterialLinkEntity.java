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
        name = "document_material_links",
        uniqueConstraints = @UniqueConstraint(name = "uk_document_material_link", columnNames = {"document_id", "material_id"}),
        indexes = {
                @Index(name = "idx_document_material_links_document", columnList = "document_id, included"),
                @Index(name = "idx_document_material_links_material", columnList = "material_id")
        }
)
public class DocumentMaterialLinkEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID documentId;

    @Column(nullable = false)
    private UUID materialId;

    @Column(nullable = false, length = 64)
    private String role;

    @Column(nullable = false)
    private boolean included;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getMaterialId() { return materialId; }
    public void setMaterialId(UUID materialId) { this.materialId = materialId; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public boolean isIncluded() { return included; }
    public void setIncluded(boolean included) { this.included = included; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
