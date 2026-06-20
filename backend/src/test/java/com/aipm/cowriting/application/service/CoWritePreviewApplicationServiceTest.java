package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aipm.cowriting.application.dto.ai.CoWriteResult;
import com.aipm.cowriting.application.dto.cowrite.CoWritePreviewResponse;
import com.aipm.cowriting.application.dto.cowrite.CoWriteRequest;
import com.aipm.cowriting.domain.entity.CoWritePreviewReviewLinkEntity;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.entity.ReviewItemEntity;
import com.aipm.cowriting.domain.entity.WorkspaceEntity;
import com.aipm.cowriting.domain.model.GenerationStatus;
import com.aipm.cowriting.domain.model.ReviewImpactLevel;
import com.aipm.cowriting.domain.repository.CoWritePreviewRepository;
import com.aipm.cowriting.domain.repository.CoWritePreviewReviewLinkRepository;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import com.aipm.cowriting.domain.repository.ReviewItemRepository;
import com.aipm.cowriting.domain.repository.WorkspaceRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CoWritePreviewApplicationServiceTest {

    @Mock
    private WorkspaceRepository workspaceRepository;
    @Mock
    private DraftVersionRepository draftVersionRepository;
    @Mock
    private CoWritePreviewRepository coWritePreviewRepository;
    @Mock
    private CoWritePreviewReviewLinkRepository coWritePreviewReviewLinkRepository;
    @Mock
    private ReviewItemRepository reviewItemRepository;
    @Mock
    private OpenAiCoWriteService openAiCoWriteService;
    @Mock
    private ReviewApplicationService reviewApplicationService;
    @Mock
    private EvidenceBindingApplicationService evidenceBindingApplicationService;

    private CoWritePreviewApplicationService service;

    @BeforeEach
    void setUp() {
        service = new CoWritePreviewApplicationService(
                workspaceRepository,
                draftVersionRepository,
                coWritePreviewRepository,
                coWritePreviewReviewLinkRepository,
                reviewItemRepository,
                openAiCoWriteService,
                reviewApplicationService,
                evidenceBindingApplicationService,
                new ObjectMapper()
        );
    }

    @Test
    void previewShouldPersistRelatedReviewLinksWhenTargetRangeOverlaps() {
        UUID workspaceId = UUID.randomUUID();
        UUID draftId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();

        WorkspaceEntity workspace = new WorkspaceEntity();
        workspace.setId(workspaceId);

        DraftVersionEntity draft = new DraftVersionEntity();
        draft.setId(draftId);
        draft.setWorkspaceId(workspaceId);
        draft.setVersionNo(1);
        draft.setTitleSuggestion("原标题");
        draft.setOutlineJson("{}");
        draft.setParagraphSkeletonsJson("[]");
        draft.setDraftText("第一段内容。\n\n第二段需要补证据。");
        draft.setSourceTraceMapJson("{}");
        draft.setGenerationStatus(GenerationStatus.SUCCESS);
        draft.setCreatedBy("system-ai");
        draft.setCreatedAt(OffsetDateTime.now());

        ReviewItemEntity review = new ReviewItemEntity();
        review.setId(reviewId);
        review.setWorkspaceId(workspaceId);
        review.setDraftVersionId(draftId);
        review.setReviewType("missing_evidence");
        review.setReviewImpactLevel(ReviewImpactLevel.LOCAL_FIX);
        review.setTargetRangeJson("{\"start\":6,\"end\":18}");
        review.setMessage("缺少证据");
        review.setCanBypass(true);
        review.setReviewStatus("OPEN");
        review.setCreatedAt(OffsetDateTime.now());

        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(draftVersionRepository.findById(draftId)).thenReturn(Optional.of(draft));
        when(openAiCoWriteService.coWrite(any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new CoWriteResult("新标题", "第一段内容补充了证据。\n\n第二段需要补证据。", Map.of()));
        when(reviewItemRepository.findByDraftVersionIdOrderByCreatedAtAsc(draftId)).thenReturn(List.of(review));
        when(coWritePreviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(coWritePreviewReviewLinkRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CoWritePreviewResponse response = service.preview(
                workspaceId,
                new CoWriteRequest(
                        draftId,
                        "rewrite_selection",
                        Map.of("start", 0, "end", 12),
                        "补充证据",
                        Map.of("keepCitations", true)
                )
        );

        assertThat(response.diffSummary())
                .extractingByKey("recheckSuggestion")
                .isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CoWritePreviewReviewLinkEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(coWritePreviewReviewLinkRepository).saveAll(captor.capture());

        assertThat(captor.getValue()).hasSize(1);
        CoWritePreviewReviewLinkEntity link = captor.getValue().get(0);
        assertThat(link.getReviewItemId()).isEqualTo(reviewId);
        assertThat(link.getRelationType()).isEqualTo("TARGET_RANGE_OVERLAP");
        assertThat(link.getRelationReason()).contains("重叠");
    }
}
