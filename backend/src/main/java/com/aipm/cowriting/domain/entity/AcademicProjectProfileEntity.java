package com.aipm.cowriting.domain.entity;

import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.AiUsagePolicy;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "academic_project_profiles")
public class AcademicProjectProfileEntity {

    @Id
    private UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AcademicStage academicStage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private DisciplineGroup disciplineGroup;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ResearchParadigm researchParadigm;

    @Column(nullable = false, length = 32)
    private String primaryLanguage;

    @Column(nullable = false, length = 64)
    private String defaultCitationStyle;

    @Column(length = 300)
    private String institution;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 48)
    private AiUsagePolicy aiUsagePolicy;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> aiPolicyJson;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    @Column(nullable = false)
    private OffsetDateTime updatedAt;

    public UUID getWorkspaceId() { return workspaceId; }
    public void setWorkspaceId(UUID workspaceId) { this.workspaceId = workspaceId; }
    public AcademicStage getAcademicStage() { return academicStage; }
    public void setAcademicStage(AcademicStage academicStage) { this.academicStage = academicStage; }
    public DisciplineGroup getDisciplineGroup() { return disciplineGroup; }
    public void setDisciplineGroup(DisciplineGroup disciplineGroup) { this.disciplineGroup = disciplineGroup; }
    public ResearchParadigm getResearchParadigm() { return researchParadigm; }
    public void setResearchParadigm(ResearchParadigm researchParadigm) { this.researchParadigm = researchParadigm; }
    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }
    public String getDefaultCitationStyle() { return defaultCitationStyle; }
    public void setDefaultCitationStyle(String defaultCitationStyle) { this.defaultCitationStyle = defaultCitationStyle; }
    public String getInstitution() { return institution; }
    public void setInstitution(String institution) { this.institution = institution; }
    public AiUsagePolicy getAiUsagePolicy() { return aiUsagePolicy; }
    public void setAiUsagePolicy(AiUsagePolicy aiUsagePolicy) { this.aiUsagePolicy = aiUsagePolicy; }
    public Map<String, Object> getAiPolicyJson() { return aiPolicyJson; }
    public void setAiPolicyJson(Map<String, Object> aiPolicyJson) { this.aiPolicyJson = aiPolicyJson; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
}
