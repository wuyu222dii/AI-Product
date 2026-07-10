package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchRequest;
import java.time.Year;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public final class LiteratureQualityService {

    private LiteratureQualityService() {
    }

    public static List<LiteratureSearchItem> enrichAndDedupe(List<LiteratureSearchItem> rawItems, LiteratureSearchRequest request) {
        Map<String, LiteratureSearchItem> grouped = new LinkedHashMap<>();
        for (LiteratureSearchItem rawItem : rawItems) {
            LiteratureSearchItem enriched = enrich(rawItem, request, Set.of(rawItem.provider()));
            LiteratureSearchItem existing = grouped.get(enriched.duplicateGroupKey());
            if (existing == null || score(enriched) > score(existing)) {
                grouped.put(enriched.duplicateGroupKey(), enriched);
            } else if (existing != null) {
                grouped.put(existing.duplicateGroupKey(), mergeProviders(existing, enriched));
            }
        }
        return grouped.values().stream()
                .sorted((left, right) -> Integer.compare(score(right), score(left)))
                .toList();
    }

    public static LiteratureSearchItem enrich(LiteratureSearchItem item, LiteratureSearchRequest request, Set<String> providers) {
        List<String> missingMetadata = missingMetadata(item);
        int qualityScore = qualityScore(item, missingMetadata);
        String duplicateGroupKey = duplicateGroupKey(item.doi(), item.title());
        List<String> matchedReasons = matchedReasons(item, request, providers, missingMetadata);
        return new LiteratureSearchItem(
                item.provider(),
                item.title(),
                item.authors() == null ? List.of() : item.authors(),
                item.year(),
                item.sourceTitle(),
                item.publisher(),
                item.doi(),
                item.url(),
                item.abstractSnippet(),
                item.citationPreview(),
                qualityScore,
                qualityLabel(qualityScore, missingMetadata),
                matchedReasons,
                missingMetadata,
                duplicateGroupKey,
                recommendedUse(request == null ? null : request.searchIntent())
        );
    }

    public static String duplicateGroupKey(String doi, String title) {
        if (doi != null && !doi.isBlank()) {
            return "doi:" + doi.trim().toLowerCase(Locale.ROOT);
        }
        String normalizedTitle = title == null ? "untitled" : title.toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}]+", "");
        if (normalizedTitle.length() > 96) {
            normalizedTitle = normalizedTitle.substring(0, 96);
        }
        return "title:" + (normalizedTitle.isBlank() ? "untitled" : normalizedTitle);
    }

    private static LiteratureSearchItem mergeProviders(LiteratureSearchItem preferred, LiteratureSearchItem duplicate) {
        Set<String> reasons = new LinkedHashSet<>(preferred.matchedReasons() == null ? List.of() : preferred.matchedReasons());
        reasons.add("多来源命中：" + preferred.provider() + " / " + duplicate.provider());
        return new LiteratureSearchItem(
                preferred.provider(),
                preferred.title(),
                preferred.authors(),
                preferred.year(),
                preferred.sourceTitle(),
                preferred.publisher(),
                preferred.doi(),
                preferred.url(),
                preferred.abstractSnippet(),
                preferred.citationPreview(),
                preferred.qualityScore(),
                preferred.qualityLabel(),
                new ArrayList<>(reasons),
                preferred.missingMetadata(),
                preferred.duplicateGroupKey(),
                preferred.recommendedUse()
        );
    }

    private static int score(LiteratureSearchItem item) {
        return item.qualityScore() == null ? 0 : item.qualityScore();
    }

    private static int qualityScore(LiteratureSearchItem item, List<String> missingMetadata) {
        int score = 0;
        if (!isBlank(item.title())) score += 20;
        if (item.authors() != null && !item.authors().isEmpty()) score += 15;
        if (!isBlank(item.year())) score += 15;
        if (!isBlank(item.doi())) score += 20;
        if (!isBlank(item.sourceTitle()) || !isBlank(item.publisher())) score += 10;
        if (!isBlank(item.abstractSnippet())) score += 10;
        if (isRecent(item.year())) score += 10;
        if (missingMetadata.size() >= 3) score -= 10;
        return Math.max(0, Math.min(100, score));
    }

    private static List<String> missingMetadata(LiteratureSearchItem item) {
        List<String> missing = new ArrayList<>();
        if (item.authors() == null || item.authors().isEmpty()) missing.add("作者");
        if (isBlank(item.year())) missing.add("年份");
        if (isBlank(item.doi())) missing.add("DOI");
        if (isBlank(item.sourceTitle()) && isBlank(item.publisher())) missing.add("期刊/出版社");
        if (isBlank(item.abstractSnippet())) missing.add("摘要");
        return missing;
    }

    private static String qualityLabel(int score, List<String> missingMetadata) {
        if (score >= 80 && missingMetadata.size() <= 1) return "推荐引用";
        if (score >= 55) return "需人工确认";
        return "信息不完整";
    }

    private static List<String> matchedReasons(
            LiteratureSearchItem item,
            LiteratureSearchRequest request,
            Set<String> providers,
            List<String> missingMetadata
    ) {
        List<String> reasons = new ArrayList<>();
        if (providers != null && !providers.isEmpty()) {
            reasons.add("来源：" + String.join(" / ", providers));
        }
        if (!isBlank(item.doi())) reasons.add("DOI 完整，可追溯");
        if (isRecent(item.year())) reasons.add("近五年文献");
        if (missingMetadata.isEmpty()) reasons.add("文献信息完整");
        if (!isBlank(item.abstractSnippet())) reasons.add("有摘要可初筛");
        String intent = request == null ? null : request.searchIntent();
        if ("method".equalsIgnoreCase(intent)) reasons.add("适合优先核对研究方法");
        if ("case".equalsIgnoreCase(intent)) reasons.add("适合寻找案例材料");
        if ("data".equalsIgnoreCase(intent)) reasons.add("适合寻找数据或实证依据");
        if ("theory".equalsIgnoreCase(intent)) reasons.add("适合理论基础或相关研究");
        return reasons.stream().distinct().limit(6).toList();
    }

    private static String recommendedUse(String searchIntent) {
        return switch (String.valueOf(searchIntent).toLowerCase(Locale.ROOT)) {
            case "theory" -> "适合作为理论基础或相关研究候选，下载后重点确认核心概念、观点和可引用页码。";
            case "method" -> "适合作为研究方法候选，下载后重点确认样本、变量、模型、实验或分析流程。";
            case "case" -> "适合作为案例材料候选，下载后重点确认场景、对象、背景和结论是否与你的论文一致。";
            case "data" -> "适合作为数据或实证依据候选，下载后重点确认数据来源、样本规模、指标和限制。";
            default -> "可作为参考文献候选，下载原文后确认研究对象、方法、关键结论和正文可引用位置。";
        };
    }

    private static boolean isRecent(String year) {
        if (isBlank(year)) {
            return false;
        }
        try {
            int currentYear = Year.now().getValue();
            int parsed = Integer.parseInt(year.trim());
            return parsed >= currentYear - 5 && parsed <= currentYear + 1;
        } catch (NumberFormatException ex) {
            return false;
        }
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
