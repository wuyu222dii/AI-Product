package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.CoWriteResult;
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
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiCoWriteService {

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCoWriteService(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                .build();
    }

    public CoWriteResult coWrite(
            String action,
            String instruction,
            Map<String, Object> controls,
            Map<String, Object> targetRange,
            String titleSuggestion,
            String currentDraftText,
            Map<String, Object> outline,
            Map<String, Object> sourceTraceMap
    ) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "未配置 OPENAI_API_KEY，无法执行真实共写"
            );
        }

        String normalizedAction = normalizeAction(action);
        String currentText = currentDraftText == null ? "" : currentDraftText;
        SelectedRange selectedRange = selectedRange(targetRange, currentText);
        String prompt = selectedRange == null
                ? buildFullDraftPrompt(normalizedAction, instruction, controls, targetRange, titleSuggestion, currentText, outline, sourceTraceMap)
                : buildSelectionPrompt(normalizedAction, instruction, controls, selectedRange, titleSuggestion, currentText, outline, sourceTraceMap);

        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful academic writing assistant that outputs valid JSON only."),
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
                        "AI 共写服务暂时不可用，请稍后重试；如果正文较长，请先选中一小段再执行共写",
                        Map.of("statusCode", response.statusCode(), "body", response.body())
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractOutputText(root);
            JsonNode result = objectMapper.readTree(content);
            String title = result.path("titleSuggestion").asText(titleSuggestion == null ? "" : titleSuggestion);
            Map<String, Object> traceMap = objectMapper.convertValue(result.path("sourceTraceMap"), Map.class);

            if (selectedRange != null) {
                String replacementText = result.path("replacementText").asText("");
                if (replacementText.isBlank()) {
                    throw new BusinessException(
                            ErrorCode.AI_SERVICE_UNAVAILABLE,
                            HttpStatus.BAD_GATEWAY.value(),
                            "AI 共写返回内容为空，请稍后重试或缩小选区"
                    );
                }
                String mergedDraft = currentText.substring(0, selectedRange.start())
                        + replacementText
                        + currentText.substring(selectedRange.end());
                return new CoWriteResult(title, mergedDraft, traceMap.isEmpty() ? sourceTraceMap : traceMap);
            }

            return new CoWriteResult(
                    title,
                    result.path("draftText").asText(currentText),
                    traceMap.isEmpty() ? sourceTraceMap : traceMap
            );
        } catch (HttpTimeoutException e) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.GATEWAY_TIMEOUT.value(),
                    "AI 共写请求超时，请稍后重试；如果正文较长，请先选中一小段再执行",
                    Map.of("timeoutSeconds", openAiProperties.getTimeoutSeconds())
            );
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "解析 AI 共写结果失败，请稍后重试",
                    Map.of("reason", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage()))
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "AI 共写请求被中断，请稍后重试"
            );
        }
    }

    private String buildFullDraftPrompt(
            String normalizedAction,
            String instruction,
            Map<String, Object> controls,
            Map<String, Object> targetRange,
            String titleSuggestion,
            String currentDraftText,
            Map<String, Object> outline,
            Map<String, Object> sourceTraceMap
    ) {
        return """
                You are revising an academic course paper draft in Chinese.
                Action type: %s
                Action guidance: %s
                User instruction: %s
                Structured controls:
                %s
                Target range: %s

                Return strict JSON only:
                {
                  "titleSuggestion": "string",
                  "draftText": "full revised draft text in Chinese",
                  "sourceTraceMap": {}
                }

                Rules:
                - Keep the writing aligned with the requested action.
                - Follow Structured controls as hard constraints when they conflict with stylistic preferences.
                - rewriteDepth=light means only local wording changes; balanced means moderate rewriting; deep means larger structure/expression changes are allowed.
                - If keepCitations=true, preserve existing citation markers unless the user explicitly asks to repair citation format.
                - If keepData=true, do not change numbers, names, locations, dates, or measured findings.
                - If noNewSources=true, do not introduce citations or evidence not present in the current source trace map.
                - If keepStudentVoice=true, avoid overly polished template-like phrasing and keep the student's original stance and wording habits where possible.
                - If target range contains start/end/selectedText, prioritize revising that range while preserving the rest of the draft.
                - Return the full revised draft, not partial snippets.
                - Keep the paper coherent and suitable as a course paper draft.
                - Keep the student's topic, evidence, and teacher requirements as the highest priority.
                - Reduce obvious AI-like phrasing: avoid empty transitions, inflated claims, repetitive summaries, and generic value judgments.
                - Preserve title unless a better title is necessary.

                Current title:
                %s

                Current outline:
                %s

                Current source trace map:
                %s

                Current draft:
                %s
                """.formatted(
                normalizedAction,
                actionGuidance(normalizedAction),
                instruction == null ? "" : instruction,
                writeJson(controls == null ? Map.of() : controls),
                writeJson(targetRange == null ? Map.of("mode", "full_draft") : targetRange),
                titleSuggestion == null ? "" : titleSuggestion,
                writeJson(outline),
                writeJson(sourceTraceMap),
                currentDraftText
        );
    }

    private String buildSelectionPrompt(
            String normalizedAction,
            String instruction,
            Map<String, Object> controls,
            SelectedRange selectedRange,
            String titleSuggestion,
            String currentDraftText,
            Map<String, Object> outline,
            Map<String, Object> sourceTraceMap
    ) {
        return """
                You are revising a selected passage from a Chinese academic course paper.
                Action type: %s
                Action guidance: %s
                User instruction: %s
                Structured controls:
                %s

                Return strict JSON only:
                {
                  "titleSuggestion": "string",
                  "replacementText": "revised selected passage only in Chinese",
                  "sourceTraceMap": {}
                }

                Rules:
                - Return only the replacement text for the selected passage, not the full draft.
                - Follow Structured controls as hard constraints when they conflict with stylistic preferences.
                - rewriteDepth=light means only local wording changes; balanced means moderate rewriting; deep means larger structure/expression changes are allowed.
                - If keepCitations=true, preserve existing citation markers unless the user explicitly asks to repair citation format.
                - If keepData=true, do not change numbers, names, locations, dates, or measured findings.
                - If noNewSources=true, do not introduce citations or evidence not present in the current source trace map.
                - If keepStudentVoice=true, avoid overly polished template-like phrasing and keep the student's original stance and wording habits where possible.
                - Preserve the student's topic, evidence alignment, and teacher requirements.
                - Keep the replacement passage compatible with the before/after context.
                - Do not invent citations or data.
                - Reduce obvious AI-like phrasing: avoid empty transitions, inflated claims, repetitive summaries, and generic value judgments.

                Current title:
                %s

                Current outline:
                %s

                Current source trace map:
                %s

                Before selected passage:
                %s

                Selected passage:
                %s

                After selected passage:
                %s
                """.formatted(
                normalizedAction,
                actionGuidance(normalizedAction),
                instruction == null ? "" : instruction,
                writeJson(controls == null ? Map.of() : controls),
                titleSuggestion == null ? "" : titleSuggestion,
                writeJson(outline),
                writeJson(sourceTraceMap),
                contextBefore(currentDraftText, selectedRange.start()),
                selectedRange.text(),
                contextAfter(currentDraftText, selectedRange.end())
        );
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

    private String extractOutputText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            String messageContent = choices.get(0).path("message").path("content").asText();
            if (!messageContent.isBlank()) {
                return messageContent;
            }
        }

        if (root.hasNonNull("output_text")) {
            String outputText = root.get("output_text").asText();
            if (!outputText.isBlank()) {
                return outputText;
            }
        }

        throw new BusinessException(
                ErrorCode.AI_SERVICE_UNAVAILABLE,
                HttpStatus.BAD_GATEWAY.value(),
                "AI 共写返回中未找到可解析内容，请稍后重试",
                Map.of("response", root.toString())
        );
    }

    private SelectedRange selectedRange(Map<String, Object> targetRange, String draftText) {
        if (targetRange == null || !"selection".equalsIgnoreCase(String.valueOf(targetRange.getOrDefault("mode", "")))) {
            return null;
        }
        int start = intValue(targetRange.get("start"), -1);
        int end = intValue(targetRange.get("end"), -1);
        if (start < 0 || end <= start || start >= draftText.length()) {
            return null;
        }
        int safeEnd = Math.min(end, draftText.length());
        return new SelectedRange(start, safeEnd, draftText.substring(start, safeEnd));
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (RuntimeException ex) {
            return fallback;
        }
    }

    private String contextBefore(String draftText, int start) {
        int from = Math.max(0, start - 900);
        return draftText.substring(from, start);
    }

    private String contextAfter(String draftText, int end) {
        int to = Math.min(draftText.length(), end + 900);
        return draftText.substring(end, to);
    }

    private String normalizeAction(String action) {
        if (action == null || action.isBlank()) {
            return "rewrite_selection";
        }
        return action.trim().toLowerCase();
    }

    private String actionGuidance(String action) {
        return switch (action) {
            case "add_evidence" -> "Add or strengthen evidence for the selected claim. Prefer concrete source-linked wording and do not invent citations.";
            case "adjust_structure" -> "Improve paragraph order, topic sentences, transitions, and argumentative hierarchy without changing the core conclusion.";
            case "reduce_repetition" -> "Remove information repetition while preserving necessary structural recap in introductions, section summaries, and conclusions.";
            case "improve_expression" -> "Make the prose clearer, more natural, and more student-authored; reduce generic AI tone and keep academic restraint.";
            case "expand_argument" -> "Expand the selected argument with mechanism, condition, evidence interpretation, or limitation, not empty filler.";
            case "shorten_text" -> "Compress the selected text by keeping claims and evidence, removing redundant modifiers and repeated explanations.";
            default -> "Rewrite the selected text or full draft according to the user's instruction while preserving meaning and evidence alignment.";
        };
    }

    private record SelectedRange(int start, int end, String text) {
    }
}
