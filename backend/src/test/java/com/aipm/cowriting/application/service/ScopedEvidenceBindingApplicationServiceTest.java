package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.model.ContentScope;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.EvidenceBindingRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScopedEvidenceBindingApplicationServiceTest {

    @Mock private ContentScopeResolverService scopeResolver;
    @Mock private EvidenceBindingRepository bindingRepository;
    @Mock private MaterialRepository materialRepository;
    @Mock private AiSemanticParseResultRepository parseResultRepository;
    @Mock private KnowledgeChunkRepository knowledgeChunkRepository;

    private ScopedEvidenceBindingApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ScopedEvidenceBindingApplicationService(
                scopeResolver,
                bindingRepository,
                materialRepository,
                parseResultRepository,
                knowledgeChunkRepository,
                new ObjectMapper()
        );
    }

    @Test
    void rebuildShouldReturnMissingBindingsWhenSectionHasNoMaterials() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        UUID sectionId = UUID.randomUUID();
        ContentScope scope = new ContentScope(
                "SECTION",
                workspaceId,
                documentId,
                sectionId,
                null,
                3,
                "绪论",
                "第一段没有来源。\n\n第二段同样需要补充材料。",
                Map.of(),
                "APA",
                Map.of(),
                List.of()
        );
        when(scopeResolver.section(sectionId)).thenReturn(scope);
        when(bindingRepository.findBySectionIdOrderBySectionVersionNoDescCreatedAtAsc(sectionId)).thenReturn(List.of());
        when(knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of());
        when(bindingRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceBindingSummaryResponse response = service.rebuildSection(sectionId);

        assertThat(response.analysisState()).isEqualTo("CURRENT");
        assertThat(response.sectionVersionNo()).isEqualTo(3);
        assertThat(response.paragraphs()).hasSize(2);
        assertThat(response.paragraphs()).allSatisfy(paragraph -> {
            assertThat(paragraph.bindingStatus()).isEqualTo("MISSING");
            assertThat(paragraph.bindings()).hasSize(1);
            assertThat(paragraph.bindings().get(0).materialId()).isNull();
        });
        assertThat(response.coverage().missingParagraphs()).isEqualTo(2);
        assertThat(response.coverage().coverageRatio()).isZero();
    }
}
