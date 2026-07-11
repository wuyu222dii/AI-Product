package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.DocumentMaterialLinkEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.DocumentMaterialLinkRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionVersionRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicDocumentMaterialIsolationTest {

    @Mock private AcademicDocumentRepository documentRepository;
    @Mock private DocumentSectionRepository sectionRepository;
    @Mock private DocumentSectionVersionRepository sectionVersionRepository;
    @Mock private DocumentMaterialLinkRepository materialLinkRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private AcademicProfileApplicationService profileService;

    private AcademicDocumentApplicationService service;

    @BeforeEach
    void setUp() {
        service = new AcademicDocumentApplicationService(
                documentRepository, sectionRepository, sectionVersionRepository, materialLinkRepository,
                materialRepository, workspaceRepository, profileService, new AcademicRuleCatalog()
        );
    }

    @Test
    void eachDocumentShouldResolveOnlyItsIncludedMaterials() {
        UUID workspaceId = UUID.randomUUID();
        AcademicDocumentEntity proposal = document(workspaceId);
        AcademicDocumentEntity thesis = document(workspaceId);
        MaterialEntity literature = material(workspaceId);
        MaterialEntity dataset = material(workspaceId);

        when(materialLinkRepository.existsByDocumentId(proposal.getId())).thenReturn(true);
        when(materialLinkRepository.existsByDocumentId(thesis.getId())).thenReturn(true);
        when(materialLinkRepository.findByDocumentIdAndIncludedTrue(proposal.getId()))
                .thenReturn(List.of(link(proposal.getId(), literature.getId())));
        when(materialLinkRepository.findByDocumentIdAndIncludedTrue(thesis.getId()))
                .thenReturn(List.of(link(thesis.getId(), dataset.getId())));
        when(materialRepository.findAllById(List.of(literature.getId()))).thenReturn(List.of(literature));
        when(materialRepository.findAllById(List.of(dataset.getId()))).thenReturn(List.of(dataset));

        assertThat(service.resolveMaterials(proposal)).extracting(MaterialEntity::getId).containsExactly(literature.getId());
        assertThat(service.resolveMaterials(thesis)).extracting(MaterialEntity::getId).containsExactly(dataset.getId());
    }

    private AcademicDocumentEntity document(UUID workspaceId) {
        AcademicDocumentEntity entity = new AcademicDocumentEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        return entity;
    }

    private MaterialEntity material(UUID workspaceId) {
        MaterialEntity entity = new MaterialEntity();
        entity.setId(UUID.randomUUID());
        entity.setWorkspaceId(workspaceId);
        return entity;
    }

    private DocumentMaterialLinkEntity link(UUID documentId, UUID materialId) {
        DocumentMaterialLinkEntity entity = new DocumentMaterialLinkEntity();
        entity.setDocumentId(documentId);
        entity.setMaterialId(materialId);
        entity.setIncluded(true);
        return entity;
    }
}
