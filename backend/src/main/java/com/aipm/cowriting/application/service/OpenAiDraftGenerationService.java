package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.DraftGenerationResult;
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
public class OpenAiDraftGenerationService {

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiDraftGenerationService(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                .build();
    }

    public DraftGenerationResult generate(
            Map<String, Object> requirementContext,
            List<Map<String, Object>> materialContext,
            String mode
    ) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "未配置 OPENAI_API_KEY，无法执行真实初稿生成"
            );
        }
        if (materialContext == null || materialContext.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INSUFFICIENT_MATERIAL,
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    "缺少可用于生成初稿的已解析材料"
            );
        }

        String modeInstruction = switch (normalizeMode(mode)) {
            case "quick" -> """
                    Mode: QUICK
                    - Prioritize a faster, cleaner, shorter first draft.
                    - Keep paragraph count lean and structure obvious.
                    - Reduce ornamental transitions.
                    """;
            case "academic" -> """
                    Mode: ACADEMIC
                    - Use more formal academic phrasing.
                    - Strengthen claim-evidence-analysis relationships.
                    - Emphasize literature and argument rigor.
                    """;
            default -> """
                    Mode: STABLE
                    - Prefer clear, requirement-aligned, conservative drafting.
                    - Keep structure steady and avoid unsupported expansion.
                    - Optimize for a dependable course-paper first draft.
                    """;
        };

        String prompt = """
                Generate a first academic draft in Chinese based only on the provided requirement context and parsed material context.
                Return strict JSON only with exactly this shape:
                {
                  "titleSuggestion": "string",
                  "outline": {
                    "sections": [
                      { "title": "string", "purpose": "string" }
                    ]
                  },
                  "paragraphSkeletons": [
                    {
                      "paragraphId": "p1",
                      "goal": "string",
                      "evidenceHints": ["material-id-1", "material-id-2"]
                    }
                  ],
                  "draftText": "multi paragraph academic draft text in Chinese",
                  "sourceTraceMap": {
                    "p1": ["material-id-1"]
                  }
                }

                Rules:
                - Use only the provided material context.
                - Keep the draft aligned to the requirement context.
                - Use Chinese.
                - The draft should be suitable for a course paper first draft, not final polished output.
                - The sourceTraceMap and evidenceHints should reference the provided material ids when possible.
                - Use bibliographicMetadata when referring to literature. Do not invent authors, years, titles, publishers, URLs, or DOI values.
                - Apply the selected mode guidance faithfully.

                Selected mode guidance:
                %s

                Requirement context:
                %s

                Parsed material context:
                %s
                """.formatted(
                modeInstruction,
                writeJson(requirementContext),
                writeJson(materialContext)
        );

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
                        "OpenAI 初稿生成请求失败",
                        Map.of("statusCode", response.statusCode(), "body", response.body())
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = extractOutputText(root);
            JsonNode result = objectMapper.readTree(content);

            return new DraftGenerationResult(
                    result.path("titleSuggestion").asText("AI 论文共写初稿"),
                    objectMapper.convertValue(result.path("outline"), Map.class),
                    objectMapper.convertValue(result.path("paragraphSkeletons"), List.class),
                    result.path("draftText").asText(""),
                    objectMapper.convertValue(result.path("sourceTraceMap"), Map.class)
            );
        } catch (HttpTimeoutException e) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.GATEWAY_TIMEOUT.value(),
                    "OpenAI 初稿生成请求超时，请稍后重试或缩短目标字数",
                    Map.of("timeoutSeconds", openAiProperties.getTimeoutSeconds())
            );
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "解析 OpenAI 初稿生成结果失败",
                    Map.of("reason", e.getClass().getSimpleName(), "message", String.valueOf(e.getMessage()))
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenAI 初稿生成请求被中断"
            );
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
                "OpenAI 返回中未找到可解析的初稿内容",
                Map.of("response", root.toString())
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

    private String normalizeMode(String mode) {
        if (mode == null || mode.isBlank() || "default".equalsIgnoreCase(mode)) {
            return "stable";
        }
        return mode.trim().toLowerCase();
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.value(), "JSON 序列化失败");
        }
    }
}
