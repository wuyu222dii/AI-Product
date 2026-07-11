package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.AppealReviewResult;
import com.aipm.cowriting.application.dto.ai.ReviewGenerationResult;
import com.aipm.cowriting.application.dto.ai.ReviewRecheckResult;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.config.OpenAiProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiReviewService {

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiReviewService(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                .build();
    }

    public ReviewGenerationResult generateReview(
            String draftText,
            Map<String, Object> requirementSnapshot,
            Map<String, Object> sourceTraceMap
    ) {
        String prompt = """
                Review this academic document draft and return strict JSON only.
                Return JSON shape:
                {
                  "items": [
                    {
                      "reviewType": "missing_evidence | requirement_conflict | repetition_issue | logic_gap | factual_risk | citation_missing | citation_format_mismatch | reference_orphan | reference_not_cited | reference_metadata_incomplete | aigc_style_risk | generic_unsupported_claim | original_evidence_missing",
                      "reviewImpactLevel": "NOTICE | LOCAL_FIX | MUST_CONFIRM",
                      "targetRange": { "start": 0, "end": 120 },
                      "message": "string",
                      "suggestedFix": "string",
                      "canBypass": true
                    }
                  ]
                }

                Rules:
                - Return at most 5 items.
                - Use NOTICE for optional improvements.
                - Use LOCAL_FIX for local paragraph or evidence issues.
                - Use MUST_CONFIRM only for issues that affect correctness, requirement alignment, or major structure.
                - Treat unverifiable or invented citations as MUST_CONFIRM.
                - Treat citation format mismatch or uncited references as LOCAL_FIX unless it affects source authenticity.
                - Use aigc_style_risk / generic_unsupported_claim / original_evidence_missing only as writing-quality guidance.
                - Do not claim to bypass AI detection or plagiarism checks; focus on concrete evidence, original cases, data, and academic rigor.
                - Apply confirmed institution, supervisor, course, journal, or user submission requirements before platform defaults.
                - Do not infer an academic stage or document type beyond the supplied project and document context.
                - If no issue exists, return an empty items array.

                Requirement snapshot:
                %s

                Source trace map:
                %s

                Draft:
                %s
                """.formatted(writeJson(requirementSnapshot), writeJson(sourceTraceMap), draftText);

        JsonNode root = callJson(prompt);
        return new ReviewGenerationResult(objectMapper.convertValue(root.path("items"), List.class));
    }

    public AppealReviewResult reviewAppeal(
            Map<String, Object> reviewItem,
            String draftText,
            String userReason,
            Map<String, Object> evidence
    ) {
        String prompt = """
                Re-evaluate this review item appeal and return strict JSON only.
                Return JSON shape:
                {
                  "reviewOutcome": "maintained | downgraded_to_notice | downgraded_to_local_fix | withdrawn",
                  "downgradedImpactLevel": "NOTICE | LOCAL_FIX | MUST_CONFIRM | null"
                }

                Original review item:
                %s

                Draft:
                %s

                User appeal reason:
                %s

                Additional evidence:
                %s
                """.formatted(writeJson(reviewItem), draftText, userReason, writeJson(evidence));

        JsonNode root = callJson(prompt);
        return new AppealReviewResult(
                root.path("reviewOutcome").asText("maintained"),
                root.path("downgradedImpactLevel").isNull() ? null : root.path("downgradedImpactLevel").asText(null)
        );
    }

    public ReviewRecheckResult recheckReviewItem(
            Map<String, Object> reviewItem,
            String draftText
    ) {
        String prompt = """
                Recheck exactly one academic review item against the current draft and return strict JSON only.
                Return JSON shape:
                {
                  "outcome": "RESOLVED | STILL_OPEN | DOWNGRADED | NEEDS_MORE_EVIDENCE",
                  "downgradedImpactLevel": "NOTICE | LOCAL_FIX | MUST_CONFIRM | null",
                  "note": "short Chinese explanation for the author"
                }

                Rules:
                - Only evaluate this single review item.
                - RESOLVED means the current draft now clearly fixes the issue.
                - STILL_OPEN means the issue still exists.
                - DOWNGRADED means the issue still exists but is less severe.
                - NEEDS_MORE_EVIDENCE means the issue cannot be confirmed without more uploaded material or source context.
                - Do not invent facts or sources.

                Review item:
                %s

                Current draft:
                %s
                """.formatted(writeJson(reviewItem), draftText == null ? "" : draftText);

        JsonNode root = callJson(prompt);
        return new ReviewRecheckResult(
                root.path("outcome").asText("STILL_OPEN"),
                root.path("downgradedImpactLevel").isNull() ? null : root.path("downgradedImpactLevel").asText(null),
                root.path("note").asText("")
        );
    }

    private JsonNode callJson(String prompt) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE.value(), "未配置 OPENAI_API_KEY，无法执行真实审查");
        }
        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a strict academic writing reviewer that outputs valid JSON only."),
                        Map.of("role", "user", "content", prompt)
                ),
                "response_format", Map.of("type", "json_object")
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl(openAiProperties.getBaseUrl()) + "/chat/completions"))
                    .timeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                    .header("Authorization", "Bearer " + openAiProperties.getApiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 400) {
                throw new BusinessException(
                        ErrorCode.AI_SERVICE_UNAVAILABLE,
                        HttpStatus.BAD_GATEWAY.value(),
                        "OpenAI 审查请求失败",
                        Map.of("statusCode", response.statusCode(), "body", response.body())
                );
            }
            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText();
            return objectMapper.readTree(content);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY.value(), "解析 OpenAI 审查结果失败");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.AI_SERVICE_UNAVAILABLE, HttpStatus.BAD_GATEWAY.value(), "OpenAI 审查请求被中断");
        }
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        String normalized = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (!normalized.endsWith("/v1")) {
            normalized = normalized + "/v1";
        }
        return normalized;
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }
}
