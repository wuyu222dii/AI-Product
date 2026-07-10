package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.writingrisk.WritingRiskItemResponse;
import com.aipm.cowriting.application.dto.writingrisk.WritingRiskSummaryResponse;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.domain.entity.DraftVersionEntity;
import com.aipm.cowriting.domain.repository.DraftVersionRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class WritingRiskApplicationService {

    private static final Pattern CITATION_PATTERN = Pattern.compile("([（(][^）)]{1,80}[，,]\\s*((19|20)\\d{2}|n\\.d\\.)[）)])|(\\[\\d{1,3}])");
    private static final Pattern NUMBER_PATTERN = Pattern.compile("(\\d+(\\.\\d+)?\\s*(%|％|人|份|次|条|个|项|年|月|天|分|小时|kWh|℃)?)");
    private static final Pattern CASE_PATTERN = Pattern.compile("(案例|访谈|问卷|样本|实验|调研|数据|表\\s*\\d+|图\\s*\\d+|以.+为例|例如)");
    private static final List<String> VAGUE_PHRASES = List.of(
            "具有重要意义",
            "显著提升",
            "有效促进",
            "不可忽视",
            "进一步研究",
            "综上所述",
            "由此可见",
            "在一定程度上",
            "相关部门",
            "各方面",
            "多措并举",
            "具有积极作用",
            "奠定基础",
            "提供保障"
    );
    private static final List<String> ABSTRACT_WORDS = List.of(
            "优化",
            "提升",
            "促进",
            "完善",
            "推动",
            "加强",
            "保障",
            "重要",
            "显著",
            "有效",
            "综合",
            "全面"
    );

    private final DraftVersionRepository draftVersionRepository;

    public WritingRiskApplicationService(DraftVersionRepository draftVersionRepository) {
        this.draftVersionRepository = draftVersionRepository;
    }

    public WritingRiskSummaryResponse evaluate(UUID draftId) {
        DraftVersionEntity draft = draftVersionRepository.findById(draftId)
                .orElseThrow(() -> new BusinessException(ErrorCode.DRAFT_NOT_FOUND, HttpStatus.NOT_FOUND.value(), "draft 不存在"));
        return evaluate(draft);
    }

    public WritingRiskSummaryResponse evaluate(DraftVersionEntity draft) {
        List<ParagraphSlice> paragraphs = splitParagraphs(draft.getDraftText());
        List<WritingRiskItemResponse> items = new ArrayList<>();
        Set<String> globalRecommendations = new LinkedHashSet<>();

        for (int index = 0; index < paragraphs.size(); index += 1) {
            ParagraphSlice paragraph = paragraphs.get(index);
            List<RiskSignal> signals = signals(paragraph.text());
            if (signals.isEmpty()) {
                continue;
            }
            WritingRiskItemResponse item = toRiskItem("p" + (index + 1), paragraph, signals);
            items.add(item);
            globalRecommendations.add(item.suggestedAction());
        }

        int overallScore = score(paragraphs.size(), items);
        return new WritingRiskSummaryResponse(
                draft.getId(),
                status(overallScore, items),
                overallScore,
                items.stream().limit(12).toList(),
                recommendations(globalRecommendations, items)
        );
    }

    public List<Map<String, Object>> reviewItems(DraftVersionEntity draft) {
        return evaluate(draft).items().stream()
                .filter(item -> !"NOTICE".equals(item.level()))
                .limit(5)
                .map(this::toReviewItem)
                .toList();
    }

    private List<RiskSignal> signals(String paragraph) {
        String text = normalize(paragraph);
        if (text.length() < 70) {
            return List.of();
        }
        List<RiskSignal> signals = new ArrayList<>();
        long vaguePhraseCount = VAGUE_PHRASES.stream().filter(text::contains).count();
        long abstractWordCount = ABSTRACT_WORDS.stream().filter(text::contains).count();
        boolean hasCitation = CITATION_PATTERN.matcher(text).find();
        boolean hasNumber = NUMBER_PATTERN.matcher(text).find();
        boolean hasCase = CASE_PATTERN.matcher(text).find();

        if (vaguePhraseCount >= 2) {
            signals.add(new RiskSignal("aigc_style_risk", "出现多处模板化或套话表达"));
        }
        if (abstractWordCount >= 5 && !hasNumber && !hasCase) {
            signals.add(new RiskSignal("generic_unsupported_claim", "抽象评价较多，但缺少具体案例、数据或情境"));
        }
        if (text.length() >= 110 && !hasCitation && !hasNumber && !hasCase) {
            signals.add(new RiskSignal("original_evidence_missing", "该段较长，但没有引用、数据、案例或实证线索"));
        }
        if (repeatedConclusion(text)) {
            signals.add(new RiskSignal("aigc_style_risk", "段落以泛化总结为主，容易显得空泛"));
        }
        return signals;
    }

    private WritingRiskItemResponse toRiskItem(String paragraphId, ParagraphSlice paragraph, List<RiskSignal> signals) {
        String riskType = strongestRiskType(signals);
        String level = signals.size() >= 3 || "original_evidence_missing".equals(riskType) ? "LOCAL_FIX" : "NOTICE";
        return new WritingRiskItemResponse(
                paragraphId,
                targetRange(paragraph),
                riskType,
                level,
                snippet(paragraph.text(), 180),
                signals.stream().map(RiskSignal::message).distinct().toList(),
                suggestedAction(riskType),
                supplementPrompt(riskType),
                coWriteInstruction(riskType)
        );
    }

    private Map<String, Object> toReviewItem(WritingRiskItemResponse item) {
        Map<String, Object> review = new LinkedHashMap<>();
        review.put("reviewType", item.riskType());
        review.put("reviewImpactLevel", item.level());
        review.put("targetRange", item.targetRange());
        review.put("message", "该段存在原创实证或 AI 写作味风险：" + String.join("；", item.signals()));
        review.put("suggestedFix", item.suggestedAction());
        review.put("canBypass", true);
        return review;
    }

    private String strongestRiskType(List<RiskSignal> signals) {
        if (signals.stream().anyMatch(signal -> "original_evidence_missing".equals(signal.riskType()))) {
            return "original_evidence_missing";
        }
        if (signals.stream().anyMatch(signal -> "generic_unsupported_claim".equals(signal.riskType()))) {
            return "generic_unsupported_claim";
        }
        return "aigc_style_risk";
    }

    private String suggestedAction(String riskType) {
        return switch (riskType) {
            case "original_evidence_missing" -> "补充原创研究材料，例如问卷结果、访谈记录、实验数据、课程案例或真实文献依据。";
            case "generic_unsupported_claim" -> "把抽象判断改成“具体场景 + 数据/案例 + 分析解释”的结构。";
            default -> "减少套话和泛化总结，保留必要结论，同时加入更具体的研究对象、条件或证据。";
        };
    }

    private String supplementPrompt(String riskType) {
        return switch (riskType) {
            case "original_evidence_missing" -> "请补充这段可使用的原创实证材料：研究对象、样本/案例、关键数据或观察结果是：";
            case "generic_unsupported_claim" -> "请补充一个能支撑该判断的具体例子、数据或材料来源：";
            default -> "请说明这段想表达的真实观察、课程案例或个人研究发现：";
        };
    }

    private String coWriteInstruction(String riskType) {
        return switch (riskType) {
            case "original_evidence_missing" -> "请只基于已上传材料为选中段落补充原创实证支撑；如果材料不足，请列出需要用户补充的案例、数据或访谈信息，不要编造。";
            case "generic_unsupported_claim" -> "请将选中段落从抽象判断改为具体论证，优先使用已有材料中的案例、数据或来源；不得新增不存在的事实。";
            default -> "请降低选中段落的模板化 AI 写作味，减少空泛套话，并保留学术克制；如果缺少实证支撑，请明确提示需要补充什么。";
        };
    }

    private int score(int paragraphCount, List<WritingRiskItemResponse> items) {
        if (paragraphCount == 0) {
            return 100;
        }
        long localFix = items.stream().filter(item -> "LOCAL_FIX".equals(item.level())).count();
        long notice = items.size() - localFix;
        int penalty = (int) Math.min(65, localFix * 14 + notice * 6);
        return Math.max(0, 100 - penalty);
    }

    private String status(int score, List<WritingRiskItemResponse> items) {
        if (score < 60) {
            return "NEEDS_ORIGINAL_EVIDENCE";
        }
        boolean hasActionableRisk = items.stream().anyMatch(item -> "LOCAL_FIX".equals(item.level()));
        if (hasActionableRisk) {
            return "NEEDS_REVIEW";
        }
        if (score >= 82) {
            return "READY";
        }
        return "NEEDS_REVIEW";
    }

    private List<String> recommendations(Set<String> globalRecommendations, List<WritingRiskItemResponse> items) {
        if (items.isEmpty()) {
            return List.of("当前未发现明显空泛无据段落，仍建议人工通读并确认数据、案例和引用真实。");
        }
        List<String> result = new ArrayList<>(globalRecommendations);
        result.add("本功能不承诺降低检测分数；建议把高风险段落改成有真实材料支撑的原创论证。");
        return result.stream().limit(5).toList();
    }

    private List<ParagraphSlice> splitParagraphs(String text) {
        String source = text == null ? "" : text;
        List<ParagraphSlice> result = new ArrayList<>();
        Matcher matcher = Pattern.compile("\\S(?:.|\\R)*?(?=(\\R\\s*\\R)|$)").matcher(source);
        while (matcher.find()) {
            String paragraph = matcher.group().trim();
            if (paragraph.isBlank()) {
                continue;
            }
            int start = source.indexOf(paragraph, matcher.start());
            result.add(new ParagraphSlice(start, start + paragraph.length(), paragraph));
        }
        return result;
    }

    private Map<String, Object> targetRange(ParagraphSlice paragraph) {
        return Map.of(
                "start", paragraph.start(),
                "end", paragraph.end(),
                "selectedText", paragraph.text()
        );
    }

    private boolean repeatedConclusion(String text) {
        return (text.contains("综上") || text.contains("由此可见") || text.contains("因此"))
                && !NUMBER_PATTERN.matcher(text).find()
                && !CITATION_PATTERN.matcher(text).find();
    }

    private String normalize(String value) {
        return value == null ? "" : value.replaceAll("\\s+", " ").trim();
    }

    private String snippet(String value, int maxLength) {
        String normalized = normalize(value);
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength - 1) + "…";
    }

    private record ParagraphSlice(int start, int end, String text) {
    }

    private record RiskSignal(String riskType, String message) {
    }
}
