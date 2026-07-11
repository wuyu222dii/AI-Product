package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.knowledge.KnowledgeBuildResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchRequest;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchResponse;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.AcademicDocumentEntity;
import com.aipm.cowriting.domain.entity.KnowledgeChunkEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.KnowledgeChunkRepository;
import com.aipm.cowriting.domain.repository.MaterialRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class KnowledgeBaseApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private MaterialRepository materialRepository;
    @Mock
    private AiSemanticParseResultRepository aiSemanticParseResultRepository;
    @Mock
    private KnowledgeChunkRepository knowledgeChunkRepository;
    @Mock
    private AcademicDocumentApplicationService academicDocumentApplicationService;

    private KnowledgeBaseApplicationService knowledgeBaseApplicationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        knowledgeBaseApplicationService = new KnowledgeBaseApplicationService(
                workspaceRepository,
                materialRepository,
                aiSemanticParseResultRepository,
                knowledgeChunkRepository,
                academicDocumentApplicationService,
                objectMapper
        );
    }

    @Test
    void buildShouldCreateChunksFromAiParsedMaterials() {
        UUID workspaceId = UUID.randomUUID();
        MaterialEntity material = parsedMaterial(workspaceId);
        AiSemanticParseResultEntity parseResult = parseResult(material.getId());
        List<KnowledgeChunkEntity> savedChunks = new ArrayList<>();

        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(material));
        when(aiSemanticParseResultRepository.findByMaterialIdIn(List.of(material.getId()))).thenReturn(List.of(parseResult));
        when(knowledgeChunkRepository.saveAll(any())).thenAnswer(invocation -> {
            savedChunks.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        });

        KnowledgeBuildResponse response = knowledgeBaseApplicationService.build(workspaceId);

        assertThat(response.status()).isEqualTo("LEXICAL_READY");
        assertThat(response.materialCount()).isEqualTo(1);
        assertThat(response.chunkCount()).isGreaterThan(0);
        assertThat(savedChunks).isNotEmpty();
        assertThat(savedChunks.get(0).getChunkText()).contains("资料整理效率");
        assertThat(savedChunks.get(0).getChunkText())
                .contains("张三、李四，2024，AI 学习工具研究，教育技术")
                .doesNotContain("\"title\"");
        assertThat(savedChunks)
                .allSatisfy(chunk -> assertThat(chunk.getChunkText())
                        .doesNotContain("|")
                        .doesNotContain("####")
                        .doesNotContain("======"));
    }

    @Test
    void buildShouldPreferSemanticChunksAndFilterUnreadableRawNoise() {
        UUID workspaceId = UUID.randomUUID();
        MaterialEntity material = parsedMaterial(workspaceId);
        material.setPlainTextContent("||||||||||||\n======\n##_#_#_#_#\n~~~~~~~");
        material.setSupplementText(null);
        AiSemanticParseResultEntity parseResult = parseResult(material.getId());
        List<KnowledgeChunkEntity> savedChunks = new ArrayList<>();

        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(materialRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(material));
        when(aiSemanticParseResultRepository.findByMaterialIdIn(List.of(material.getId()))).thenReturn(List.of(parseResult));
        when(knowledgeChunkRepository.saveAll(any())).thenAnswer(invocation -> {
            savedChunks.addAll(invocation.getArgument(0));
            return invocation.getArgument(0);
        });

        KnowledgeBuildResponse response = knowledgeBaseApplicationService.build(workspaceId);

        assertThat(response.status()).isEqualTo("LEXICAL_READY");
        assertThat(savedChunks).hasSize(1);
        assertThat(savedChunks.get(0).getChunkText())
                .contains("解析摘要")
                .contains("AI 工具对大学生学习方式有双重影响")
                .doesNotContain("||||")
                .doesNotContain("~~~~");
    }

    @Test
    void searchShouldReturnLexicalMatchedChunks() {
        UUID workspaceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        MaterialEntity material = new MaterialEntity();
        material.setId(materialId);
        material.setFilename("research-note.txt");

        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setId(UUID.randomUUID());
        chunk.setWorkspaceId(workspaceId);
        chunk.setMaterialId(materialId);
        chunk.setChunkIndex(1);
        chunk.setChunkText("人工智能工具提升了资料整理效率，也改变了学生阅读文献的方式。");
        chunk.setSourceExcerpt("人工智能工具提升了资料整理效率。");
        chunk.setKeywordsJson("[\"人工智能工具\",\"资料整理效率\"]");
        chunk.setCreatedAt(OffsetDateTime.now());

        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId)).thenReturn(List.of(chunk));
        when(materialRepository.findAllById(List.of(materialId))).thenReturn(List.of(material));

        KnowledgeSearchResponse response = knowledgeBaseApplicationService.search(
                workspaceId,
                new KnowledgeSearchRequest("资料整理效率", 5)
        );

        assertThat(response.total()).isEqualTo(1);
        assertThat(response.items().get(0).materialTitle()).isEqualTo("research-note.txt");
        assertThat(response.items().get(0).score()).isGreaterThan(0);
    }

    @Test
    void searchShouldRespectDocumentMaterialScope() {
        UUID workspaceId = UUID.randomUUID();
        UUID documentId = UUID.randomUUID();
        MaterialEntity included = material("included.pdf");
        MaterialEntity excluded = material("excluded.pdf");
        KnowledgeChunkEntity includedChunk = chunk(workspaceId, included.getId(), "研究方法采用问卷调查与回归分析");
        KnowledgeChunkEntity excludedChunk = chunk(workspaceId, excluded.getId(), "研究方法采用问卷调查与回归分析");
        AcademicDocumentEntity document = new AcademicDocumentEntity();
        document.setId(documentId);
        document.setWorkspaceId(workspaceId);

        when(workspaceRepository.existsById(workspaceId)).thenReturn(true);
        when(academicDocumentApplicationService.getDocument(documentId)).thenReturn(document);
        when(academicDocumentApplicationService.resolveMaterials(document)).thenReturn(List.of(included));
        when(knowledgeChunkRepository.findByWorkspaceIdOrderByCreatedAtDesc(workspaceId))
                .thenReturn(List.of(includedChunk, excludedChunk));
        when(materialRepository.findAllById(List.of(included.getId()))).thenReturn(List.of(included));

        KnowledgeSearchResponse response = knowledgeBaseApplicationService.search(
                workspaceId,
                new KnowledgeSearchRequest("问卷调查", 8, documentId, List.of(), List.of())
        );

        assertThat(response.items()).extracting(item -> item.materialId()).containsExactly(included.getId());
    }

    private MaterialEntity material(String filename) {
        MaterialEntity material = new MaterialEntity();
        material.setId(UUID.randomUUID());
        material.setFilename(filename);
        return material;
    }

    private KnowledgeChunkEntity chunk(UUID workspaceId, UUID materialId, String text) {
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setId(UUID.randomUUID());
        chunk.setWorkspaceId(workspaceId);
        chunk.setMaterialId(materialId);
        chunk.setChunkIndex(1);
        chunk.setChunkText(text);
        chunk.setSourceExcerpt(text);
        chunk.setKeywordsJson("[\"问卷调查\",\"回归分析\"]");
        chunk.setCreatedAt(OffsetDateTime.now());
        return chunk;
    }

    private MaterialEntity parsedMaterial(UUID workspaceId) {
        MaterialEntity material = new MaterialEntity();
        material.setId(UUID.randomUUID());
        material.setWorkspaceId(workspaceId);
        material.setFilename("research-note.txt");
        material.setFileType("txt");
        material.setSourceType("pasted_text");
        material.setRawFileUrl("/mock/material");
        material.setKeyMaterial(false);
        material.setParseStage(ParseStage.AI_PARSED);
        material.setConfidenceScore(BigDecimal.valueOf(0.90));
        material.setPlainTextContent("""
                #### OCR page
                | 指标 | 数值 |
                |---|---|
                | 效率提升 | 30% |
                ======
                研究笔记显示，人工智能工具提升了资料整理效率，但也可能削弱独立思考深度。
                """);
        material.setCreatedAt(OffsetDateTime.now());
        return material;
    }

    private AiSemanticParseResultEntity parseResult(UUID materialId) {
        AiSemanticParseResultEntity parseResult = new AiSemanticParseResultEntity();
        parseResult.setId(UUID.randomUUID());
        parseResult.setMaterialId(materialId);
        parseResult.setMaterialCategory(MaterialCategory.RESEARCH_RESULT);
        parseResult.setSummary("AI 工具对大学生学习方式有双重影响。");
        parseResult.setTopicRelation("可支撑论文中关于学习效率与独立思考的讨论。");
        parseResult.setDetectedClaimsJson("[\"AI 工具提升资料整理效率\"]");
        parseResult.setDetectedEvidenceJson("[\"研究笔记显示资料整理效率提高\"]");
        parseResult.setDetectedRequirementsJson("[]");
        parseResult.setBibliographicMetadataJson("""
                {"authors":["张三","李四"],"year":"2024","title":"AI 学习工具研究","sourceTitle":"教育技术","publisher":null,"url":"https://example.com","doi":null,"publicationType":"JOURNAL_ARTICLE"}
                """);
        parseResult.setConfidenceScore(BigDecimal.valueOf(0.90));
        parseResult.setCreatedAt(OffsetDateTime.now());
        return parseResult;
    }
}
