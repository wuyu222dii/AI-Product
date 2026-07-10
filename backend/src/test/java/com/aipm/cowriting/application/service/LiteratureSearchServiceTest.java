package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchRequest;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.WorkspaceStatus;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import java.io.IOException;
import java.time.OffsetDateTime;
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
class LiteratureSearchServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private RequirementSnapshotRepository requirementSnapshotRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private AiSemanticParseResultRepository parseResultRepository;
    @Mock
    private CrossrefLiteratureClient crossrefLiteratureClient;

    private LiteratureSearchService literatureSearchService;

    @BeforeEach
    void setUp() {
        literatureSearchService = new LiteratureSearchService(
                workspaceRepository,
                requirementSnapshotRepository,
                materialRepository,
                parseResultRepository,
                crossrefLiteratureClient
        );
    }

    @Test
    void searchShouldBuildDefaultQueryFromWorkspaceRequirementAndMaterialSummary() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        WorkspaceEntity workspace = workspace(workspaceId, "智能教室能源管理系统");
        RequirementSnapshotEntity snapshot = new RequirementSnapshotEntity();
        snapshot.setTopic("机器学习预测高校教室能耗");
        MaterialEntity material = new MaterialEntity();
        material.setId(UUID.randomUUID());
        AiSemanticParseResultEntity parseResult = new AiSemanticParseResultEntity();
        parseResult.setSummary("已有材料强调物联网采集和能耗预测模型");

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId)).thenReturn(Optional.of(snapshot));
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(material));
        when(parseResultRepository.findByMaterialIdIn(List.of(material.getId()))).thenReturn(List.of(parseResult));
        when(crossrefLiteratureClient.search(anyString(), eq(5))).thenReturn(List.of(item()));

        var response = literatureSearchService.search(
                workspaceId,
                new LiteratureSearchRequest(null, "crossref", 5, "reference_material")
        );

        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(crossrefLiteratureClient).search(queryCaptor.capture(), eq(5));
        assertThat(queryCaptor.getValue())
                .contains("智能教室能源管理系统")
                .contains("机器学习预测高校教室能耗")
                .contains("已有材料强调物联网采集和能耗预测模型")
                .contains("research literature");
        assertThat(response.items()).hasSize(1);
        assertThat(response.providerStatus()).containsEntry("crossref", "SUCCESS");
        assertThat(response.externalSearchLinks()).extracting("provider")
                .contains("Google Scholar", "CNKI", "Crossref");
    }

    @Test
    void searchShouldReturnExternalLinksWhenCrossrefFails() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace(workspaceId, "AI 写作")));
        when(crossrefLiteratureClient.search("AI writing", 10)).thenThrow(new IOException("timeout"));

        var response = literatureSearchService.search(
                workspaceId,
                new LiteratureSearchRequest("AI writing", "crossref", null, "reference_material")
        );

        assertThat(response.items()).isEmpty();
        assertThat(response.providerStatus()).containsEntry("crossref", "FAILED");
        assertThat(response.externalSearchLinks().stream().map(link -> link.url()).toList())
                .anySatisfy(url -> assertThat(url).contains("scholar.google.com").contains("AI+writing"))
                .anySatisfy(url -> assertThat(url).contains("oversea.cnki.net").contains("AI+writing"));
    }

    private WorkspaceEntity workspace(UUID workspaceId, String title) {
        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(workspaceId);
        workspace.setUserId(UUID.randomUUID());
        workspace.setTitle(title);
        workspace.setStatus(WorkspaceStatus.DRAFT);
        workspace.setCreatedAt(OffsetDateTime.now());
        workspace.setUpdatedAt(OffsetDateTime.now());
        return workspace;
    }

    private LiteratureSearchItem item() {
        return new LiteratureSearchItem(
                "Crossref",
                "Intelligent classroom energy management",
                List.of("Jane Doe"),
                "2024",
                "Energy Informatics",
                "Example Publisher",
                "10.1234/example",
                "https://doi.org/10.1234/example",
                "Machine learning reduces energy waste.",
                "Jane Doe. (2024). Intelligent classroom energy management."
        );
    }
}
