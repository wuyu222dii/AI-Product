package com.aipm.cowriting.application.service;

import com.aipm.cowriting.domain.model.AcademicDocumentType;
import com.aipm.cowriting.domain.model.ResearchParadigm;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AcademicRuleCatalog {

    public List<SectionTemplate> defaultSections(AcademicDocumentType documentType, ResearchParadigm paradigm) {
        List<SectionTemplate> sections = new ArrayList<>(switch (documentType) {
            case COURSE_PAPER -> List.of(
                    template("INTRODUCTION", "引言", 1),
                    template("LITERATURE_REVIEW", "相关研究与理论基础", 2),
                    template("ANALYSIS", "分析与论证", 3),
                    template("CONCLUSION", "结论", 4)
            );
            case RESEARCH_PROPOSAL -> List.of(
                    template("BACKGROUND", "研究背景与问题提出", 1),
                    template("LITERATURE_REVIEW", "文献综述与研究缺口", 2),
                    template("RESEARCH_QUESTIONS", "研究问题与目标", 3),
                    template("METHODOLOGY", "研究方法与技术路线", 4),
                    template("PLAN_AND_ETHICS", "研究计划、风险与伦理", 5),
                    template("EXPECTED_CONTRIBUTION", "预期成果与贡献", 6)
            );
            case UNDERGRADUATE_THESIS -> degreeSections(false, false);
            case MASTER_THESIS -> degreeSections(true, false);
            case DOCTORAL_DISSERTATION -> degreeSections(true, true);
            case JOURNAL_ARTICLE -> List.of(
                    template("INTRODUCTION", "引言", 1),
                    template("LITERATURE_REVIEW", "相关研究", 2),
                    template("METHODOLOGY", "研究方法", 3),
                    template("RESULTS", "研究结果", 4),
                    template("DISCUSSION", "讨论", 5),
                    template("CONCLUSION", "结论", 6)
            );
            case CONFERENCE_PAPER -> List.of(
                    template("INTRODUCTION", "引言", 1),
                    template("METHOD", "方法", 2),
                    template("RESULTS", "结果", 3),
                    template("CONCLUSION", "结论", 4)
            );
            case LITERATURE_REVIEW -> List.of(
                    template("INTRODUCTION", "综述范围与问题", 1),
                    template("SEARCH_METHOD", "检索与筛选方法", 2),
                    template("THEMATIC_SYNTHESIS", "主题归纳与证据综合", 3),
                    template("RESEARCH_GAPS", "研究缺口", 4),
                    template("CONCLUSION", "结论", 5)
            );
            case RESEARCH_REPORT -> List.of(
                    template("BACKGROUND", "研究背景", 1),
                    template("METHOD", "研究方法", 2),
                    template("FINDINGS", "主要发现", 3),
                    template("RECOMMENDATIONS", "结论与建议", 4)
            );
        });

        if (paradigm == ResearchParadigm.SYSTEMATIC_REVIEW && documentType != AcademicDocumentType.LITERATURE_REVIEW) {
            sections.removeIf(section -> "RESULTS".equals(section.sectionType()) || "METHODOLOGY".equals(section.sectionType()));
            sections.add(Math.min(2, sections.size()), template("SEARCH_METHOD", "检索与筛选方法", 3));
            sections.add(Math.min(3, sections.size()), template("EVIDENCE_SYNTHESIS", "证据综合", 4));
        }
        return normalizeOrder(sections);
    }

    public boolean requiresResearchArtifacts(AcademicDocumentType documentType, ResearchParadigm paradigm) {
        if (documentType == AcademicDocumentType.RESEARCH_PROPOSAL
                || documentType == AcademicDocumentType.COURSE_PAPER
                || documentType == AcademicDocumentType.LITERATURE_REVIEW) {
            return false;
        }
        return paradigm != ResearchParadigm.THEORETICAL
                && paradigm != ResearchParadigm.SYSTEMATIC_REVIEW
                && paradigm != ResearchParadigm.OTHER;
    }

    public boolean sectionRequiresResearchArtifacts(String sectionType, ResearchParadigm paradigm) {
        if (paradigm == ResearchParadigm.THEORETICAL || paradigm == ResearchParadigm.SYSTEMATIC_REVIEW) {
            return false;
        }
        String normalized = sectionType == null ? "" : sectionType.toUpperCase();
        return normalized.contains("RESULT")
                || normalized.contains("FINDING")
                || normalized.contains("DISCUSSION")
                || normalized.contains("ANALYSIS");
    }

    private List<SectionTemplate> degreeSections(boolean includeLimitations, boolean doctoral) {
        List<SectionTemplate> sections = new ArrayList<>(List.of(
                template("INTRODUCTION", "绪论", 1),
                template("LITERATURE_REVIEW", "文献综述与理论基础", 2),
                template("RESEARCH_QUESTIONS", "研究问题与分析框架", 3),
                template("METHODOLOGY", "研究方法", 4),
                template("RESULTS", "研究结果", 5),
                template("DISCUSSION", "讨论", 6)
        ));
        if (doctoral) {
            sections.add(template("ORIGINAL_CONTRIBUTION", "原创贡献与理论推进", 7));
        }
        if (includeLimitations) {
            sections.add(template("LIMITATIONS", "研究局限与展望", doctoral ? 8 : 7));
        }
        sections.add(template("CONCLUSION", "结论", doctoral ? 9 : includeLimitations ? 8 : 7));
        return sections;
    }

    private List<SectionTemplate> normalizeOrder(List<SectionTemplate> sections) {
        List<SectionTemplate> normalized = new ArrayList<>();
        for (int i = 0; i < sections.size(); i++) {
            SectionTemplate section = sections.get(i);
            normalized.add(new SectionTemplate(section.sectionType(), section.title(), i + 1, section.targetLength()));
        }
        return normalized;
    }

    private SectionTemplate template(String type, String title, int order) {
        return new SectionTemplate(type, title, order, null);
    }

    public record SectionTemplate(String sectionType, String title, int sortOrder, Integer targetLength) {
    }
}
