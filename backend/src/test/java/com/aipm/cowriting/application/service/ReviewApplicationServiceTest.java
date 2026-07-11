package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.ai.ReviewGenerationResult;
import com.aipm.cowriting.application.dto.review.ReviewItemResponse;
import com.aipm.cowriting.application.dto.review.UpdateReviewStatusRequest;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.RequirementSnapshotEntity;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.model.ReviewImpactLevel;
import com.aipm.cowriting.domain.repository.AiSemanticParseResultRepository;
import com.aipm.cowriting.domain.repository.AppealCaseRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.RequirementSnapshotRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.ReviewRecheckLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewApplicationServiceTest {

    @Mock
    private DraftVersionRepository draftVersionRepository;
    @Mock
    private RequirementSnapshotRepository requirementSnapshotRepository;
    @Mock
    private ReviewItemRepository reviewItemRepository;
    @Mock
    private AppealCaseRepository appealCaseRepository;
    @Mock
    private AiSemanticParseResultRepository aiSemanticParseResultRepository;
    @Mock
    private ReviewRecheckLogRepository reviewRecheckLogRepository;
    @Mock
    private OpenAiReviewService openAiReviewService;
    @Mock
    private WritingRiskApplicationService writingRiskApplicationService;
    @Mock
    private ContentScopeResolverService contentScopeResolverService;

    private ReviewApplicationService reviewApplicationService;

    @BeforeEach
    void setUp() {
        reviewApplicationService = new ReviewApplicationService(
                draftVersionRepository,
                requirementSnapshotRepository,
                reviewItemRepository,
                appealCaseRepository,
                aiSemanticParseResultRepository,
                reviewRecheckLogRepository,
                openAiReviewService,
                writingRiskApplicationService,
                contentScopeResolverService,
                new ObjectMapper()
        );
        lenient().when(writingRiskApplicationService.reviewItems(any(DraftVersionEntity.class))).thenReturn(List.of());
    }

    @Test
    void refreshForDraftShouldAppendDeterministicCitationReviews() {
        UUID workspaceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();

        DraftVersionEntity draft = new DraftVersionEntity();
        draft.setId(UUID.randomUUID());
        draft.setWorkspaceId(workspaceId);
        draft.setDraftText("智能教室能源管理可以提升节能效率，但正文暂时没有引用标记。");
        draft.setSourceTraceMapJson("{\"p1\":[\"" + materialId + "\"]}");

        RequirementSnapshotEntity snapshot = new RequirementSnapshotEntity();
        snapshot.setWorkspaceId(workspaceId);
        snapshot.setCitationStyle("APA");
        snapshot.setSpecialRequirementsJson("{}");

        when(requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId)).thenReturn(Optional.of(snapshot));
        when(openAiReviewService.generateReview(any(), any(), any())).thenReturn(new ReviewGenerationResult(List.of()));
        when(aiSemanticParseResultRepository.findByMaterialIdIn(any())).thenReturn(List.of());
        when(reviewItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ReviewItemEntity> items = reviewApplicationService.refreshForDraft(draft);

        assertThat(items).extracting(ReviewItemEntity::getReviewType)
                .contains("citation_missing", "reference_metadata_incomplete");
    }

    @Test
    void refreshForDraftShouldFlagCitationFormatMismatch() {
        UUID workspaceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();

        DraftVersionEntity draft = new DraftVersionEntity();
        draft.setId(UUID.randomUUID());
        draft.setWorkspaceId(workspaceId);
        draft.setDraftText("已有研究表明智能管理能降低能耗[1]。");
        draft.setSourceTraceMapJson("{\"p1\":[\"" + materialId + "\"]}");

        RequirementSnapshotEntity snapshot = new RequirementSnapshotEntity();
        snapshot.setWorkspaceId(workspaceId);
        snapshot.setCitationStyle("APA");
        snapshot.setSpecialRequirementsJson("{}");

        when(requirementSnapshotRepository.findFirstByWorkspaceIdOrderByVersionDesc(workspaceId)).thenReturn(Optional.of(snapshot));
        when(openAiReviewService.generateReview(any(), any(), any())).thenReturn(new ReviewGenerationResult(List.of()));
        when(aiSemanticParseResultRepository.findByMaterialIdIn(any())).thenReturn(List.of());
        when(reviewItemRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<ReviewItemEntity> items = reviewApplicationService.refreshForDraft(draft);

        assertThat(items).extracting(ReviewItemEntity::getReviewType)
                .contains("citation_format_mismatch");
    }

    @Test
    void updateStatusShouldMarkReviewAsResolved() {
        UUID reviewId = UUID.randomUUID();
        ReviewItemEntity entity = new ReviewItemEntity();
        entity.setId(reviewId);
        entity.setReviewType("missing_evidence");
        entity.setReviewImpactLevel(ReviewImpactLevel.LOCAL_FIX);
        entity.setTargetRangeJson("{}");
        entity.setMessage("缺少证据");
        entity.setCanBypass(true);
        entity.setReviewStatus("OPEN");

        when(reviewItemRepository.findById(reviewId)).thenReturn(Optional.of(entity));
        when(reviewItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ReviewItemResponse response = reviewApplicationService.updateStatus(
                reviewId,
                new UpdateReviewStatusRequest("RESOLVED", "已经补充证据")
        );

        assertThat(response.reviewStatus()).isEqualTo("RESOLVED");
        assertThat(response.resolutionNote()).isEqualTo("已经补充证据");
        assertThat(response.resolvedAt()).isNotNull();
    }
}
