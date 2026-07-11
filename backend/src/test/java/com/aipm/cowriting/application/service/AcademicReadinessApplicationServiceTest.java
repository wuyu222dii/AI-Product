package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.academic.DocumentReadinessResponse;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.AcademicProjectProfileEntity;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.AcademicDocumentType;
import com.aipm.cowriting.domain.model.AcademicStage;
import com.aipm.cowriting.domain.model.DisciplineGroup;
import com.aipm.cowriting.domain.model.DocumentSectionStatus;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.MaterialRole;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.model.ResearchArtifactType;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicReadinessApplicationServiceTest {

    @Mock private AcademicDocumentApplicationService documentService;
    @Mock private AcademicProfileApplicationService profileService;
    @Mock private AiSemanticParseResultRepository parseResultRepository;
    @Mock private DocumentSectionRepository sectionRepository;

    private AcademicReadinessApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AcademicReadinessApplicationService(
                documentService,
                profileService,
                new AcademicRuleCatalog(),
                parseResultRepository,
                sectionRepository
        );
    }

    @Test
    void researchProposalShouldNotRequireExistingResearchResults() {
        Fixture fixture = fixture(AcademicDocumentType.RESEARCH_PROPOSAL, ResearchParadigm.QUANTITATIVE, false);

        DocumentReadinessResponse response = service.check(fixture.document().getId());

        assertThat(response.generationEligible()).isTrue();
        assertThat(response.issues()).noneMatch(item -> "RESEARCH_ARTIFACT_MISSING".equals(item.code()));
    }

    @Test
    void quantitativeMasterThesisShouldRequireResearchArtifacts() {
        Fixture fixture = fixture(AcademicDocumentType.MASTER_THESIS, ResearchParadigm.QUANTITATIVE, false);

        DocumentReadinessResponse response = service.check(fixture.document().getId());

        assertThat(response.generationEligible()).isFalse();
        assertThat(response.issues()).anyMatch(item -> "RESEARCH_ARTIFACT_MISSING".equals(item.code()));
    }

    @Test
    void quantitativeMasterThesisShouldBecomeEligibleAfterResearchArtifactIsParsed() {
        Fixture fixture = fixture(AcademicDocumentType.MASTER_THESIS, ResearchParadigm.QUANTITATIVE, true);

        DocumentReadinessResponse response = service.check(fixture.document().getId());

        assertThat(response.generationEligible()).isTrue();
        assertThat(response.artifactCoverage().get("researchArtifact")).isTrue();
    }

    private Fixture fixture(AcademicDocumentType documentType, ResearchParadigm paradigm, boolean includeResearchArtifact) {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        AcademicDocumentEntity document = new AcademicDocumentEntity();
        document.setId(documentId);
        document.setWorkspaceId(workspaceId);
        document.setDocumentType(documentType);
        document.setRequirementProfileJson(Map.of("targetLength", 30000));

        AcademicProjectProfileEntity profile = new AcademicProjectProfileEntity();
        profile.setWorkspaceId(workspaceId);
        profile.setAcademicStage(documentType == AcademicDocumentType.DOCTORAL_DISSERTATION ? AcademicStage.DOCTORAL : AcademicStage.MASTER);
        profile.setDisciplineGroup(DisciplineGroup.SOCIAL_SCIENCE);
        profile.setResearchParadigm(paradigm);

        List<MaterialEntity> materials = new ArrayList<>();
        List<AiSemanticParseResultEntity> parseResults = new ArrayList<>();
        addMaterial(workspaceId, materials, parseResults, true, MaterialRole.LITERATURE, MaterialCategory.REFERENCE_MATERIAL, ResearchArtifactType.NONE);
        addMaterial(workspaceId, materials, parseResults, false, MaterialRole.SUBMISSION_REQUIREMENT, MaterialCategory.ASSIGNMENT_REQUIREMENT, ResearchArtifactType.NONE);
        if (includeResearchArtifact) {
            addMaterial(workspaceId, materials, parseResults, false, MaterialRole.RESEARCH_ARTIFACT, MaterialCategory.RESEARCH_RESULT, ResearchArtifactType.DATASET);
        }

        DocumentSectionEntity section = new DocumentSectionEntity();
        section.setId(UUID.randomUUID());
        section.setDocumentId(documentId);
        section.setSectionType("INTRODUCTION");
        section.setContent("已有章节内容");
        section.setStatus(DocumentSectionStatus.DRAFTING);

        when(documentService.getDocument(documentId)).thenReturn(document);
        when(profileService.getEntity(workspaceId)).thenReturn(profile);
        when(documentService.resolveMaterials(document)).thenReturn(materials);
        when(parseResultRepository.findByMaterialIdIn(any())).thenReturn(parseResults);
        when(sectionRepository.findByDocumentIdOrderBySortOrderAsc(documentId)).thenReturn(List.of(section));
        return new Fixture(document, profile);
    }

    private void addMaterial(
            UUID workspaceId,
            List<MaterialEntity> materials,
            List<AiSemanticParseResultEntity> parseResults,
            boolean key,
            MaterialRole role,
            MaterialCategory category,
            ResearchArtifactType artifactType
    ) {
        MaterialEntity material = new MaterialEntity();
        material.setId(UUID.randomUUID());
        material.setWorkspaceId(workspaceId);
        material.setKeyMaterial(key);
        material.setParseStage(ParseStage.AI_PARSED);
        materials.add(material);

        AiSemanticParseResultEntity parse = new AiSemanticParseResultEntity();
        parse.setId(UUID.randomUUID());
        parse.setMaterialId(material.getId());
        parse.setMaterialRole(role);
        parse.setMaterialCategory(category);
        parse.setResearchArtifactType(artifactType);
        parseResults.add(parse);
    }

    private record Fixture(AcademicDocumentEntity document, AcademicProjectProfileEntity profile) {}
}
