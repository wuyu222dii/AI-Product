package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.academic.DocumentQualitySummaryResponse;
import com.aipm.cowriting.application.dto.academic.SectionQualitySummaryResponse;
import com.aipm.cowriting.application.dto.evidence.DocumentEvidenceSummaryResponse;
import com.aipm.cowriting.application.dto.evidence.EvidenceBindingSummaryResponse;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.writingrisk.WritingRiskItemResponse;
import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import com.aipm.cowriting.application.model.ContentScope;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class AcademicQualityApplicationService {

    private final ContentScopeResolverService scopeResolver;
    private final ScopedEvidenceBindingApplicationService evidenceService;
    private final AcademicReviewApplicationService reviewService;
    private final WritingRiskApplicationService writingRiskService;

    public AcademicQualityApplicationService(
            ContentScopeResolverService scopeResolver,
            ScopedEvidenceBindingApplicationService evidenceService,
            AcademicReviewApplicationService reviewService,
            WritingRiskApplicationService writingRiskService
    ) {
        this.scopeResolver = scopeResolver;
        this.evidenceService = evidenceService;
        this.reviewService = reviewService;
        this.writingRiskService = writingRiskService;
    }

    public WritingRiskSummaryResponse sectionWritingRisks(UUID sectionId) {
        return writingRiskService.evaluate(scopeResolver.section(sectionId));
    }

    public WritingRiskSummaryResponse documentWritingRisks(UUID documentId) {
        List<ContentScope> scopes = scopeResolver.documentSections(documentId);
        List<WritingRiskSummaryResponse> summaries = scopes.stream().map(writingRiskService::evaluate).toList();
        return aggregateWritingRisks(documentId, summaries);
    }

    private WritingRiskSummaryResponse aggregateWritingRisks(
            UUID documentId,
            List<WritingRiskSummaryResponse> summaries
    ) {
        List<WritingRiskItemResponse> items = summaries.stream().flatMap(item -> item.items().stream()).limit(40).toList();
        int score = summaries.isEmpty() ? 100 : Math.round((float) summaries.stream().mapToInt(WritingRiskSummaryResponse::overallScore).sum() / summaries.size());
        List<String> recommendations = summaries.stream().flatMap(item -> item.recommendations().stream()).distinct().limit(8).toList();
        String status = score >= 82 ? "READY" : score >= 60 ? "NEEDS_REVIEW" : "NEEDS_ORIGINAL_EVIDENCE";
        return new WritingRiskSummaryResponse(null, status, score, items, recommendations, "DOCUMENT", documentId, null, null);
    }

    public DocumentQualitySummaryResponse qualitySummary(UUID documentId) {
        List<ContentScope> scopes = scopeResolver.documentSections(documentId);
        DocumentEvidenceSummaryResponse evidence = evidenceService.documentSummary(documentId, scopes);
        Map<UUID, WritingRiskSummaryResponse> risksBySection = scopes.stream()
                .collect(Collectors.toMap(ContentScope::sectionId, writingRiskService::evaluate));
        WritingRiskSummaryResponse risks = aggregateWritingRisks(documentId, new ArrayList<>(risksBySection.values()));
        List<ReviewItemResponse> reviews = reviewService.listDocument(documentId, null, null, null);
        Map<UUID, EvidenceBindingSummaryResponse> evidenceBySection = evidence.sections().stream()
                .collect(Collectors.toMap(EvidenceBindingSummaryResponse::sectionId, item -> item));
        Map<UUID, Integer> reviewCounts = reviews.stream().filter(item -> !List.of("RESOLVED", "IGNORED", "SUPERSEDED").contains(item.reviewStatus()))
                .filter(item -> item.sectionId() != null)
                .collect(Collectors.groupingBy(ReviewItemResponse::sectionId, Collectors.summingInt(item -> 1)));
        List<SectionQualitySummaryResponse> sections = scopes.stream().map(scope -> {
            EvidenceBindingSummaryResponse sectionEvidence = evidenceBySection.get(scope.sectionId());
            int coverage = sectionEvidence == null ? 0 : sectionEvidence.coverage().coverageRatio();
            String state = sectionEvidence == null ? "STALE" : sectionEvidence.analysisState();
            return new SectionQualitySummaryResponse(scope.sectionId(), scope.title(), scope.revision(), coverage,
                    risksBySection.get(scope.sectionId()).overallScore(), reviewCounts.getOrDefault(scope.sectionId(), 0), state);
        }).toList();
        int score = Math.max(0, Math.round((evidence.coverage().coverageRatio() * 0.45f) + (risks.overallScore() * 0.35f)
                + (Math.max(0, 100 - reviews.size() * 6) * 0.20f)));
        String status = "STALE".equals(evidence.analysisState()) ? "NEEDS_REFRESH" : score >= 82 ? "READY" : score >= 60 ? "NEEDS_REVIEW" : "NEEDS_EVIDENCE";
        List<String> recommendations = new ArrayList<>();
        if ("STALE".equals(evidence.analysisState())) recommendations.add("部分章节可信链尚未针对当前版本重建。");
        if (evidence.coverage().missingParagraphs() > 0) recommendations.add("优先处理缺来源段落，再进行整篇导出。");
        if (!reviews.isEmpty()) recommendations.add("当前有 " + reviews.size() + " 条审查项，修改后可逐项复查。");
        recommendations.addAll(risks.recommendations().stream().limit(3).toList());
        return new DocumentQualitySummaryResponse(documentId, status, score, evidence, risks, reviews, sections,
                recommendations.stream().distinct().limit(6).toList());
    }
}
