package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class WritingRiskApplicationServiceTest {

    @Mock
    private DraftVersionRepository draftVersionRepository;

    private WritingRiskApplicationService service;

    @BeforeEach
    void setUp() {
        service = new WritingRiskApplicationService(draftVersionRepository);
    }

    @Test
    void shouldFlagGenericUnsupportedParagraph() {
        DraftVersionEntity draft = draft("""
                智能教室能源管理具有重要意义，可以显著提升高校管理效率，并有效促进教学环境优化。
                综上所述，该系统能够为各方面管理提供保障，在一定程度上推动学校数字化建设。
                """);

        WritingRiskSummaryResponse response = service.evaluate(draft);

        assertThat(response.overallStatus()).isIn("NEEDS_REVIEW", "NEEDS_ORIGINAL_EVIDENCE");
        assertThat(response.items()).extracting("riskType")
                .contains("generic_unsupported_claim");
        assertThat(response.items().get(0).supplementPrompt()).contains("具体例子");
    }

    @Test
    void shouldFlagOriginalEvidenceMissingForLongUnsupportedParagraph() {
        DraftVersionEntity draft = draft("""
                当前高校教室能源管理在实践中仍然存在较多问题，相关管理流程需要进一步优化，系统建设也需要从多个维度进行完善。
                在实际运行过程中，智能化管理能够推动资源配置效率提升，并为学校后续数字化治理提供重要支撑。
                因此，该研究可以为高校能源管理模式创新提供参考，也能够为后续平台建设和应用推广奠定基础。
                """);

        WritingRiskSummaryResponse response = service.evaluate(draft);

        assertThat(response.items()).extracting("riskType")
                .contains("original_evidence_missing");
        assertThat(response.items().get(0).supplementPrompt()).contains("原创实证");
    }

    @Test
    void shouldLowerRiskWhenParagraphHasDataCaseAndCitation() {
        DraftVersionEntity draft = draft("""
                以 A 高校 3 号教学楼为例，课题组统计了 30 天的照明和空调能耗数据，发现晚间空置时段仍有 18% 的设备保持运行。
                这一结果说明节能管理需要结合实时传感器数据和排课场景进行判断（张三，2024）。
                """);

        WritingRiskSummaryResponse response = service.evaluate(draft);

        assertThat(response.overallScore()).isGreaterThanOrEqualTo(82);
        assertThat(response.items()).isEmpty();
    }

    @Test
    void reviewItemsShouldOnlyReturnActionableRisks() {
        DraftVersionEntity draft = draft("""
                该研究具有重要意义，能够显著提升管理效率，并有效促进资源优化。由此可见，相关部门应多措并举，
                从各方面提供保障，进一步研究也可以为高校管理奠定基础。
                """);

        assertThat(service.reviewItems(draft))
                .isNotEmpty()
                .allSatisfy(item -> assertThat(item.get("reviewImpactLevel")).isEqualTo("LOCAL_FIX"));
    }

    private DraftVersionEntity draft(String text) {
        DraftVersionEntity draft = new DraftVersionEntity();
        draft.setId(UUID.randomUUID());
        draft.setWorkspaceId(UUID.randomUUID());
        draft.setDraftText(text);
        return draft;
    }
}
