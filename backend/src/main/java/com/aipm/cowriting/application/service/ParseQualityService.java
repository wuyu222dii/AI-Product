package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.material.ParseQualityIssue;
import com.aipm.cowriting.application.dto.material.ParseQualityReport;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.domain.entity.AiSemanticParseResultEntity;
import com.aipm.cowriting.domain.entity.MaterialEntity;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ParseQualityService {

    private static final BigDecimal LOW_CONFIDENCE_THRESHOLD = BigDecimal.valueOf(0.55d);
    private static final String LEVEL_MUST_CONFIRM = "MUST_CONFIRM";
    private static final String LEVEL_LOCAL_FIX = "LOCAL_FIX";

    public ParseQualityReport evaluate(
            MaterialEntity material,
            AiSemanticParseResultEntity parseResult,
            MaterialCategory effectiveCategory,
            List<String> claims,
            List<String> evidence,
            List<String> requirements,
            BibliographicMetadata bibliographicMetadata
    ) {
        List<ParseQualityIssue> issues = new ArrayList<>();
        BigDecimal score = normalizeScore(resolveConfidence(material, parseResult));
        ParseStage parseStage = material.getParseStage();

        boolean hasReadableInput = hasText(material.getPlainTextContent())
                || hasText(material.getSupplementText())
                || hasText(material.getExternalLink())
                || hasText(material.getStoragePath());
        boolean hasParseResult = parseResult != null;
        boolean hasSummary = hasText(parseResult == null ? null : parseResult.getSummary());
        boolean hasTopicRelation = hasText(parseResult == null ? null : parseResult.getTopicRelation());
        boolean hasCategory = effectiveCategory != null && effectiveCategory != MaterialCategory.UNKNOWN;
        boolean hasClaims = claims != null && !claims.isEmpty();
        boolean hasEvidence = evidence != null && !evidence.isEmpty();
        boolean hasRequirements = requirements != null && !requirements.isEmpty();
        boolean hasBibliographicMetadata = isBibliographicMetadataComplete(bibliographicMetadata);

        if (parseStage == ParseStage.AI_FAILED) {
            issues.add(new ParseQualityIssue(
                    "AI_PARSE_FAILED",
                    LEVEL_MUST_CONFIRM,
                    "AI 解析失败",
                    "这份材料没有得到可用的 AI 解析结果，后续生成可能缺少关键依据。",
                    "请重试解析；如果文件不清晰，请补充文字说明或重新上传更清晰的文件。",
                    "请粘贴这份材料中最关键的文字内容，或说明它对论文的作用："
            ));
        }

        if (!hasReadableInput) {
            issues.add(new ParseQualityIssue(
                    "NO_PARSEABLE_TEXT",
                    LEVEL_MUST_CONFIRM,
                    "缺少可解析内容",
                    "系统暂时没有读到可用于理解的文字、链接或补充说明。",
                    "请重新上传清晰文件，或直接补充该材料的核心内容。",
                    "这份材料的核心内容是："
            ));
        }

        if (!hasParseResult && parseStage != ParseStage.AI_FAILED) {
            issues.add(new ParseQualityIssue(
                    "AI_PARSE_NOT_COMPLETED",
                    LEVEL_MUST_CONFIRM,
                    "尚未完成 AI 解析",
                    "这份材料还没有 AI 语义解析结果，暂时不能可靠进入后续生成。",
                    "请点击重新解析该项，或补充说明后再次解析。",
                    "请补充这份材料的类型、核心内容，以及它和论文题目的关系："
            ));
        }

        if (hasParseResult && score.compareTo(LOW_CONFIDENCE_THRESHOLD) < 0) {
            issues.add(new ParseQualityIssue(
                    "LOW_CONFIDENCE",
                    LEVEL_LOCAL_FIX,
                    "解析置信度较低",
                    "AI 对这份材料的理解不够确定，建议你快速确认摘要和分类是否正确。",
                    "如果摘要不准确，请补充材料背景、图表含义或核心结论。",
                    "请补充这份材料的核心内容、来源背景或图表含义："
            ));
        }

        if (hasParseResult && !hasCategory) {
            issues.add(new ParseQualityIssue(
                    "CATEGORY_UNKNOWN",
                    LEVEL_LOCAL_FIX,
                    "材料角色不确定",
                    "AI 没有明确判断这份材料是写作要求、参考文献、研究成果还是已有草稿。",
                    "请使用材料角色纠正，或补充说明材料用途。",
                    "这份材料的角色是（学校/导师/课程/期刊要求、参考资料、研究成果、草稿、图表数据）："
            ));
        }

        if (hasParseResult && (!hasSummary || !hasTopicRelation)) {
            issues.add(new ParseQualityIssue(
                    "CONTEXT_MISSING",
                    LEVEL_LOCAL_FIX,
                    "摘要或主题关系不完整",
                    "解析结果缺少摘要或与论文主题的关系说明，后续生成时可能引用不稳。",
                    "请补充这份材料主要讲什么，以及它为什么能支撑你的论文。",
                    "这份材料主要讲了什么，以及它和论文题目的关系是："
            ));
        }

        if (effectiveCategory == MaterialCategory.REFERENCE_MATERIAL) {
            addBibliographicIssues(issues, bibliographicMetadata);
        }

        if (effectiveCategory == MaterialCategory.ASSIGNMENT_REQUIREMENT && !hasRequirements) {
            issues.add(new ParseQualityIssue(
                    "REQUIREMENTS_MISSING",
                    LEVEL_MUST_CONFIRM,
                    "写作与提交要求不完整",
                    "这份材料被识别为要求文件，但没有解析出明确的篇幅、格式、引用规范或截止日期等约束。",
                    "请补充学校、导师、课程、期刊或用户确认的关键要求，避免后续生成偏离目标规范。",
                    "写作与提交要求包括：篇幅、格式、引用规范、截止日期、选题或投稿限制："
            ));
        }

        if (effectiveCategory == MaterialCategory.RESEARCH_RESULT || effectiveCategory == MaterialCategory.CHART_OR_DATA) {
            if (!hasClaims) {
                issues.add(new ParseQualityIssue(
                        "CLAIMS_MISSING",
                        LEVEL_MUST_CONFIRM,
                        "缺少研究结论",
                        "这份材料像研究结果或图表数据，但没有提炼出可写入正文的核心结论。",
                        "请补充你希望正文使用的研究发现或数据结论。",
                        "这份研究结果/图表支持的核心结论是："
                ));
            }
            if (!hasEvidence) {
                issues.add(new ParseQualityIssue(
                        "EVIDENCE_MISSING",
                        LEVEL_MUST_CONFIRM,
                        "缺少证据说明",
                        "这份材料缺少数据来源、样本、图表含义或关键证据说明。",
                        "请补充证据来源和数据含义，避免 AI 生成时误用材料。",
                        "请补充数据来源、样本、图表含义或关键证据："
                ));
            }
        }

        Map<String, Boolean> completeness = new LinkedHashMap<>();
        completeness.put("summary", hasSummary);
        completeness.put("topicRelation", hasTopicRelation);
        completeness.put("category", hasCategory);
        completeness.put("claims", hasClaims);
        completeness.put("evidence", hasEvidence);
        completeness.put("requirements", hasRequirements);
        completeness.put("bibliographicMetadata", hasBibliographicMetadata);

        String status = resolveStatus(parseStage, issues);
        return new ParseQualityReport(status, score, List.copyOf(issues), completeness, nextAction(status));
    }

    private void addBibliographicIssues(List<ParseQualityIssue> issues, BibliographicMetadata bibliographicMetadata) {
        if (bibliographicMetadata == null
                || bibliographicMetadata.authors() == null
                || bibliographicMetadata.authors().stream().noneMatch(this::hasText)) {
            issues.add(new ParseQualityIssue(
                    "BIBLIOGRAPHIC_AUTHORS_MISSING",
                    LEVEL_LOCAL_FIX,
                    "缺少作者",
                    "该参考资料缺少明确作者，导出参考文献时可能不完整。",
                    "补充文献作者，或确认材料中确实没有作者。",
                    "这份文献的作者是："
            ));
        }
        if (bibliographicMetadata == null || !hasText(bibliographicMetadata.year())) {
            issues.add(new ParseQualityIssue(
                    "BIBLIOGRAPHIC_YEAR_MISSING",
                    LEVEL_LOCAL_FIX,
                    "缺少年份",
                    "该参考资料缺少明确年份，导出参考文献时可能不完整。",
                    "补充文献年份，或确认材料中确实没有年份。",
                    "这份文献的年份是："
            ));
        }
        if (bibliographicMetadata == null || !hasText(bibliographicMetadata.title())) {
            issues.add(new ParseQualityIssue(
                    "BIBLIOGRAPHIC_TITLE_MISSING",
                    LEVEL_LOCAL_FIX,
                    "缺少题名",
                    "该参考资料缺少明确题名，正文引用和参考文献列表可能不准确。",
                    "补充文献题名，或确认材料中确实没有题名。",
                    "这份文献的题名是："
            ));
        }
    }

    private String resolveStatus(ParseStage parseStage, List<ParseQualityIssue> issues) {
        if (parseStage == ParseStage.AI_FAILED) {
            return "FAILED";
        }
        boolean hasMustConfirm = issues.stream().anyMatch(issue -> LEVEL_MUST_CONFIRM.equals(issue.level()));
        if (hasMustConfirm) {
            return "NEEDS_SUPPLEMENT";
        }
        return issues.isEmpty() ? "READY" : "NEEDS_CONFIRMATION";
    }

    private String nextAction(String status) {
        return switch (status) {
            case "READY" -> "当前解析结果可用于后续材料检查。";
            case "NEEDS_CONFIRMATION" -> "建议确认解析结果，必要时补充说明；不会阻断继续。";
            case "NEEDS_SUPPLEMENT" -> "请按问题清单补充信息后重新解析。";
            case "FAILED" -> "解析失败，请重试、补充说明或重传材料。";
            default -> "请检查解析结果是否符合你的理解。";
        };
    }

    private BigDecimal resolveConfidence(MaterialEntity material, AiSemanticParseResultEntity parseResult) {
        if (parseResult != null && parseResult.getConfidenceScore() != null) {
            return parseResult.getConfidenceScore();
        }
        if (material.getConfidenceScore() != null) {
            return material.getConfidenceScore();
        }
        return material.getParseStage() == ParseStage.AI_FAILED ? BigDecimal.ZERO : BigDecimal.valueOf(0.5d);
    }

    private BigDecimal normalizeScore(BigDecimal score) {
        if (score == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal clamped = score.max(BigDecimal.ZERO).min(BigDecimal.ONE);
        return clamped.setScale(2, RoundingMode.HALF_UP);
    }

    private boolean isBibliographicMetadataComplete(BibliographicMetadata metadata) {
        return metadata != null
                && metadata.authors() != null
                && metadata.authors().stream().anyMatch(this::hasText)
                && hasText(metadata.year())
                && hasText(metadata.title());
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}
