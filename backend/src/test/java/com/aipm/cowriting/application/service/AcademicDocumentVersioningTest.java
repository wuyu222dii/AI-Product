package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.academic.DocumentSectionResponse;
import com.aipm.cowriting.application.dto.academic.ReorderDocumentSectionsRequest;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionEntity;
import com.aipm.cowriting.domain.entity.DocumentSectionVersionEntity;
import com.aipm.cowriting.domain.model.AcademicDocumentStatus;
import com.aipm.cowriting.domain.model.DocumentSectionStatus;
import com.aipm.cowriting.domain.repository.AcademicDocumentRepository;
import com.aipm.cowriting.domain.repository.DocumentMaterialLinkRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionRepository;
import com.aipm.cowriting.domain.repository.DocumentSectionVersionRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AcademicDocumentVersioningTest {

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
                documentRepository,
                sectionRepository,
                sectionVersionRepository,
                materialLinkRepository,
                materialRepository,
                workspaceRepository,
                profileService,
                new AcademicRuleCatalog()
        );
    }

    @Test
    void aiWriteShouldContinueAfterHighestHistoryVersion() {
        Fixture fixture = fixture(1, 2);
        when(documentRepository.findById(fixture.document().getId())).thenReturn(Optional.of(fixture.document()));

        DocumentSectionResponse response = service.applyAiSectionContent(
                fixture.section().getId(),
                2,
                "新的章节内容",
                Map.of("source", "material-1"),
                "AI_GENERATE",
                "AI 生成章节"
        );

        assertThat(response.versionNo()).isEqualTo(3);
        assertThat(fixture.section().getVersionNo()).isEqualTo(3);
        ArgumentCaptor<DocumentSectionVersionEntity> versionCaptor = ArgumentCaptor.forClass(DocumentSectionVersionEntity.class);
        verify(sectionVersionRepository).save(versionCaptor.capture());
        assertThat(versionCaptor.getValue().getVersionNo()).isEqualTo(3);
        assertThat(versionCaptor.getValue().getContent()).isEqualTo("新的章节内容");
    }

    @Test
    void aiWriteShouldRejectStaleBaseVersion() {
        Fixture fixture = fixture(2, 3);

        assertThatThrownBy(() -> service.applyAiSectionContent(
                fixture.section().getId(),
                2,
                "过期请求生成的内容",
                Map.of(),
                "AI_GENERATE",
                "AI 生成章节"
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("章节在 AI 处理期间已被修改");
    }

    @Test
    void reorderShouldUpdateSortOrderWithoutCreatingContentVersions() {
        AcademicDocumentEntity document = document();
        DocumentSectionEntity introduction = section(document.getId(), "绪论", 1, 4);
        DocumentSectionEntity methodology = section(document.getId(), "研究方法", 2, 3);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(sectionRepository.findByDocumentIdForUpdate(document.getId()))
                .thenReturn(List.of(introduction, methodology));

        List<DocumentSectionResponse> response = service.reorderSections(
                document.getId(),
                new ReorderDocumentSectionsRequest(List.of(methodology.getId(), introduction.getId()))
        );

        assertThat(response).extracting(DocumentSectionResponse::id)
                .containsExactly(methodology.getId(), introduction.getId());
        assertThat(methodology.getSortOrder()).isEqualTo(1);
        assertThat(introduction.getSortOrder()).isEqualTo(2);
        assertThat(methodology.getVersionNo()).isEqualTo(3);
        assertThat(introduction.getVersionNo()).isEqualTo(4);
        verify(sectionRepository).saveAll(List.of(methodology, introduction));
        verify(sectionVersionRepository, never()).save(any(DocumentSectionVersionEntity.class));
    }

    @Test
    void reorderShouldRejectMissingOrDuplicateSectionIds() {
        AcademicDocumentEntity document = document();
        DocumentSectionEntity introduction = section(document.getId(), "绪论", 1, 1);
        DocumentSectionEntity methodology = section(document.getId(), "研究方法", 2, 1);
        when(documentRepository.findById(document.getId())).thenReturn(Optional.of(document));
        when(sectionRepository.findByDocumentIdForUpdate(document.getId()))
                .thenReturn(List.of(introduction, methodology));

        assertThatThrownBy(() -> service.reorderSections(
                document.getId(),
                new ReorderDocumentSectionsRequest(List.of(introduction.getId(), introduction.getId()))
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须包含当前文档的全部章节");
        verify(sectionRepository, never()).saveAll(any());
    }

    private Fixture fixture(int entityVersion, int historyVersion) {
        UUID documentId = UUID.randomUUID();
        DocumentSectionEntity section = section(documentId, "绪论", 1, entityVersion);

        DocumentSectionVersionEntity latestVersion = new DocumentSectionVersionEntity();
        latestVersion.setId(UUID.randomUUID());
        latestVersion.setSectionId(section.getId());
        latestVersion.setVersionNo(historyVersion);

        AcademicDocumentEntity document = document();
        document.setId(documentId);

        when(sectionRepository.findByIdForUpdate(section.getId())).thenReturn(Optional.of(section));
        when(sectionVersionRepository.findFirstBySectionIdOrderByVersionNoDesc(section.getId()))
                .thenReturn(Optional.of(latestVersion));
        return new Fixture(section, document);
    }

    private AcademicDocumentEntity document() {
        AcademicDocumentEntity document = new AcademicDocumentEntity();
        document.setId(UUID.randomUUID());
        document.setWorkspaceId(UUID.randomUUID());
        document.setStatus(AcademicDocumentStatus.WRITING);
        document.setUpdatedAt(OffsetDateTime.now());
        return document;
    }

    private DocumentSectionEntity section(
            UUID documentId,
            String title,
            int sortOrder,
            int versionNo
    ) {
        DocumentSectionEntity section = new DocumentSectionEntity();
        section.setId(UUID.randomUUID());
        section.setDocumentId(documentId);
        section.setSortOrder(sortOrder);
        section.setSectionType("CUSTOM");
        section.setTitle(title);
        section.setContent("旧章节内容");
        section.setStatus(DocumentSectionStatus.DRAFTING);
        section.setSourceTraceMapJson(Map.of());
        section.setVersionNo(versionNo);
        section.setCreatedAt(OffsetDateTime.now());
        section.setUpdatedAt(OffsetDateTime.now());
        return section;
    }

    private record Fixture(DocumentSectionEntity section, AcademicDocumentEntity document) {}
}
