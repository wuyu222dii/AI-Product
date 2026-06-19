package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipm.cowriting.application.dto.material.ParseQualityReport;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ParseQualityServiceTest {

    private ParseQualityService service;

    @BeforeEach
    void setUp() {
        service = new ParseQualityService();
    }

    @Test
    void shouldReturnReadyWhenParsedContentIsComplete() {
        MaterialEntity material = material(ParseStage.AI_PARSED, BigDecimal.valueOf(0.92));
        AiSemanticParseResultEntity parseResult = parseResult(
                MaterialCategory.REFERENCE_MATERIAL,
                BigDecimal.valueOf(0.92),
                "文献讨论高校教室能源管理。",
                "可支撑论文中的技术背景。"
        );

        ParseQualityReport report = service.evaluate(
                material,
                parseResult,
                MaterialCategory.REFERENCE_MATERIAL,
                List.of("智能管理可以降低能耗"),
                List.of("实验显示能耗下降 12%"),
                List.of(),
                new BibliographicMetadata(List.of("张三"), "2022", "高校能源管理研究", "教育技术", null, null, null, "JOURNAL_ARTICLE")
        );

        assertThat(report.status()).isEqualTo("READY");
        assertThat(report.issues()).isEmpty();
        assertThat(report.completeness().get("bibliographicMetadata")).isTrue();
    }

    @Test
    void shouldRequestConfirmationWhenCategoryIsUnknown() {
        MaterialEntity material = material(ParseStage.AI_PARSED, BigDecimal.valueOf(0.81));
        AiSemanticParseResultEntity parseResult = parseResult(
                MaterialCategory.UNKNOWN,
                BigDecimal.valueOf(0.81),
                "材料描述课堂能耗。",
                "可能与论文主题有关。"
        );

        ParseQualityReport report = service.evaluate(
                material,
                parseResult,
                MaterialCategory.UNKNOWN,
                List.of("课堂能耗存在波动"),
                List.of("样本记录显示峰值"),
                List.of(),
                BibliographicMetadata.empty()
        );

        assertThat(report.status()).isEqualTo("NEEDS_CONFIRMATION");
        assertThat(report.issues()).extracting("code").contains("CATEGORY_UNKNOWN");
    }

    @Test
    void shouldReportMissingBibliographicFieldsForReferenceMaterial() {
        MaterialEntity material = material(ParseStage.AI_PARSED, BigDecimal.valueOf(0.78));
        AiSemanticParseResultEntity parseResult = parseResult(
                MaterialCategory.REFERENCE_MATERIAL,
                BigDecimal.valueOf(0.78),
                "文献提供技术背景。",
                "可用于相关研究章节。"
        );

        ParseQualityReport report = service.evaluate(
                material,
                parseResult,
                MaterialCategory.REFERENCE_MATERIAL,
                List.of("已有研究关注智能能耗管理"),
                List.of("文献摘要提到节能效果"),
                List.of(),
                BibliographicMetadata.empty()
        );

        assertThat(report.status()).isEqualTo("NEEDS_CONFIRMATION");
        assertThat(report.issues()).extracting("code")
                .contains("BIBLIOGRAPHIC_AUTHORS_MISSING", "BIBLIOGRAPHIC_YEAR_MISSING", "BIBLIOGRAPHIC_TITLE_MISSING");
    }

    @Test
    void shouldRequireSupplementWhenAssignmentRequirementsAreMissing() {
        MaterialEntity material = material(ParseStage.AI_PARSED, BigDecimal.valueOf(0.86));
        AiSemanticParseResultEntity parseResult = parseResult(
                MaterialCategory.ASSIGNMENT_REQUIREMENT,
                BigDecimal.valueOf(0.86),
                "这是一份课程论文说明。",
                "决定后续生成约束。"
        );

        ParseQualityReport report = service.evaluate(
                material,
                parseResult,
                MaterialCategory.ASSIGNMENT_REQUIREMENT,
                List.of(),
                List.of(),
                List.of(),
                BibliographicMetadata.empty()
        );

        assertThat(report.status()).isEqualTo("NEEDS_SUPPLEMENT");
        assertThat(report.issues()).extracting("code").contains("REQUIREMENTS_MISSING");
    }

    @Test
    void shouldRequireSupplementWhenResearchResultLacksClaimsOrEvidence() {
        MaterialEntity material = material(ParseStage.AI_PARSED, BigDecimal.valueOf(0.88));
        AiSemanticParseResultEntity parseResult = parseResult(
                MaterialCategory.RESEARCH_RESULT,
                BigDecimal.valueOf(0.88),
                "这是一份调研材料。",
                "可用于正文论证。"
        );

        ParseQualityReport report = service.evaluate(
                material,
                parseResult,
                MaterialCategory.RESEARCH_RESULT,
                List.of(),
                List.of(),
                List.of(),
                BibliographicMetadata.empty()
        );

        assertThat(report.status()).isEqualTo("NEEDS_SUPPLEMENT");
        assertThat(report.issues()).extracting("code").contains("CLAIMS_MISSING", "EVIDENCE_MISSING");
    }

    @Test
    void shouldReturnFailedWhenAiParseFailed() {
        MaterialEntity material = material(ParseStage.AI_FAILED, BigDecimal.ZERO);

        ParseQualityReport report = service.evaluate(
                material,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                null
        );

        assertThat(report.status()).isEqualTo("FAILED");
        assertThat(report.issues()).extracting("code").contains("AI_PARSE_FAILED");
    }

    private MaterialEntity material(ParseStage parseStage, BigDecimal confidenceScore) {
        MaterialEntity material = new MaterialEntity();
        material.setId(UUID.randomUUID());
        material.setWorkspaceId(UUID.randomUUID());
        material.setFilename("material.txt");
        material.setFileType("txt");
        material.setSourceType("upload");
        material.setRawFileUrl("/mock/material.txt");
        material.setPlainTextContent("课堂能源智能管理系统研究材料。");
        material.setKeyMaterial(true);
        material.setParseStage(parseStage);
        material.setConfidenceScore(confidenceScore);
        material.setCreatedAt(OffsetDateTime.now());
        return material;
    }

    private AiSemanticParseResultEntity parseResult(
            MaterialCategory category,
            BigDecimal confidenceScore,
            String summary,
            String topicRelation
    ) {
        AiSemanticParseResultEntity parseResult = new AiSemanticParseResultEntity();
        parseResult.setId(UUID.randomUUID());
        parseResult.setMaterialId(UUID.randomUUID());
        parseResult.setMaterialCategory(category);
        parseResult.setSummary(summary);
        parseResult.setTopicRelation(topicRelation);
        parseResult.setDetectedClaimsJson("[]");
        parseResult.setDetectedEvidenceJson("[]");
        parseResult.setDetectedRequirementsJson("[]");
        parseResult.setConfidenceScore(confidenceScore);
        parseResult.setCreatedAt(OffsetDateTime.now());
        return parseResult;
    }
}
