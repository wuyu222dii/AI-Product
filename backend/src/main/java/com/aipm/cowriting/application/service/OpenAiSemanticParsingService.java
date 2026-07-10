package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.ai.SemanticParseResult;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.common.error.BusinessException;
import com.aipm.cowriting.common.error.ErrorCode;
import com.aipm.cowriting.config.OpenAiProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiSemanticParsingService {

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiSemanticParsingService(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                .build();
    }

    public SemanticParseResult parse(String content, String topicContext) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "未配置 OPENAI_API_KEY，无法执行真实 AI 语义解析"
            );
        }
        if (content == null || content.isBlank()) {
            throw new BusinessException(
                    ErrorCode.MATERIAL_PARSE_INCOMPLETE,
                    HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    "当前材料没有可供 AI 理解的文本内容"
            );
        }

        String prompt = """
                Analyze the academic writing input below and return strict JSON only.
                Required JSON shape:
                {
                  "materialCategory": "ASSIGNMENT_REQUIREMENT | REFERENCE_MATERIAL | USER_DRAFT | RESEARCH_RESULT | CHART_OR_DATA | SUPPLEMENT_NOTE | UNKNOWN",
                  "summary": "short summary",
                  "topicRelation": "how this content relates to the current paper topic",
                  "detectedClaims": ["claim1"],
                  "detectedEvidence": ["evidence1"],
                  "detectedRequirements": ["requirement1"],
                  "bibliographicMetadata": {
                    "authors": ["author1"],
                    "year": "2024",
                    "title": "source title",
                    "sourceTitle": "journal, conference, book, website, or source container",
                    "publisher": "publisher name",
                    "url": "https://example.com",
                    "doi": "10.xxxx/example",
                    "publicationType": "JOURNAL_ARTICLE | BOOK | REPORT | WEBPAGE | THESIS | CONFERENCE | UNKNOWN"
                  },
                  "confidenceScore": 0.0
                }
                Rules:
                - materialCategory must be one of the allowed enum values.
                - confidenceScore must be a number between 0 and 1.
                - detectedClaims / detectedEvidence / detectedRequirements must always be arrays.
                - Extract bibliographicMetadata only from explicit source information in the input, filename, DOI, or URL.
                - Do not invent authors, year, title, sourceTitle, publisher, URL, or DOI. Use empty strings/arrays when unknown.
                - For assignment requirements, drafts, data notes, or user research notes that are not citable literature, keep unknown bibliographic fields empty.
                Current topic context:
                %s

                Input:
                %s
                """.formatted(topicContext == null ? "" : topicContext, content);

        Map<String, Object> body = Map.of(
                "model", openAiProperties.getModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", "You are a helpful assistant that outputs valid JSON only."),
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
                UpstreamError upstreamError = parseUpstreamError(response.body());
                throw new BusinessException(
                        ErrorCode.AI_SERVICE_UNAVAILABLE,
                        HttpStatus.BAD_GATEWAY.value(),
                        "OpenAI 语义解析请求失败：" + upstreamError.message(),
                        Map.of(
                                "statusCode", response.statusCode(),
                                "upstreamCode", upstreamError.code(),
                                "upstreamType", upstreamError.type()
                        )
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String outputText = extractOutputText(root);
            JsonNode result = objectMapper.readTree(outputText);

            return new SemanticParseResult(
                    result.path("materialCategory").asText("UNKNOWN"),
                    result.path("summary").asText(""),
                    result.path("topicRelation").asText(""),
                    readStringList(result.path("detectedClaims")),
                    readStringList(result.path("detectedEvidence")),
                    readStringList(result.path("detectedRequirements")),
                    readBibliographicMetadata(result.path("bibliographicMetadata")),
                    BigDecimal.valueOf(result.path("confidenceScore").asDouble(0.5d))
            );
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "解析 OpenAI 返回结果失败"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "OpenAI 语义解析请求被中断"
            );
        }
    }

    private String extractOutputText(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode first = choices.get(0);
            String messageContent = first.path("message").path("content").asText();
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

        JsonNode output = root.path("output");
        if (output.isArray()) {
            for (JsonNode item : output) {
                if (item.hasNonNull("text") && !item.get("text").asText().isBlank()) {
                    return item.get("text").asText();
                }

                JsonNode content = item.path("content");
                if (content.isArray()) {
                    for (JsonNode contentItem : content) {
                        if (contentItem.hasNonNull("text")) {
                            String contentText = contentItem.get("text").asText();
                            if (!contentText.isBlank()) {
                                return contentText;
                            }
                        }

                        if (contentItem.hasNonNull("output_text")) {
                            String contentText = contentItem.get("output_text").asText();
                            if (!contentText.isBlank()) {
                                return contentText;
                            }
                        }
                    }
                }
            }
        }

        String recursiveText = findFirstTextValue(root);
        if (recursiveText != null && !recursiveText.isBlank()) {
            return recursiveText;
        }

        throw new BusinessException(
                ErrorCode.MATERIAL_PARSE_FAILED,
                HttpStatus.BAD_GATEWAY.value(),
                "OpenAI 返回中未找到可解析的文本输出",
                Map.of("response", root.toString())
        );
    }

    private String findFirstTextValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isObject()) {
            JsonNode textNode = node.get("text");
            if (textNode != null && textNode.isTextual() && !textNode.asText().isBlank()) {
                return textNode.asText();
            }
            JsonNode outputTextNode = node.get("output_text");
            if (outputTextNode != null && outputTextNode.isTextual() && !outputTextNode.asText().isBlank()) {
                return outputTextNode.asText();
            }
            var fields = node.fields();
            while (fields.hasNext()) {
                var entry = fields.next();
                String found = findFirstTextValue(entry.getValue());
                if (found != null && !found.isBlank()) {
                    return found;
                }
            }
            return null;
        }
        if (node.isArray()) {
            for (JsonNode child : node) {
                String found = findFirstTextValue(child);
                if (found != null && !found.isBlank()) {
                    return found;
                }
            }
        }
        return null;
    }

    private List<String> readStringList(JsonNode node) {
        List<String> items = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode child : node) {
                String value = textValue(child);
                if (value != null && !value.isBlank()) {
                    items.add(value);
                }
            }
        }
        return items;
    }

    private String textValue(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isTextual() || node.isNumber() || node.isBoolean()) {
            return node.asText();
        }
        if (node.isObject()) {
            for (String field : List.of("claim", "evidence", "requirement", "text", "content", "value", "description")) {
                JsonNode fieldValue = node.get(field);
                if (fieldValue != null && fieldValue.isTextual() && !fieldValue.asText().isBlank()) {
                    return fieldValue.asText();
                }
            }
            return node.toString();
        }
        return node.toString();
    }

    private BibliographicMetadata readBibliographicMetadata(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return BibliographicMetadata.empty();
        }
        return new BibliographicMetadata(
                readStringList(node.path("authors")),
                emptyToNull(node.path("year").asText(null)),
                emptyToNull(node.path("title").asText(null)),
                emptyToNull(node.path("sourceTitle").asText(null)),
                emptyToNull(node.path("publisher").asText(null)),
                emptyToNull(node.path("url").asText(null)),
                emptyToNull(node.path("doi").asText(null)),
                emptyToNull(node.path("publicationType").asText(null))
        );
    }

    private String emptyToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
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

    private UpstreamError parseUpstreamError(String body) {
        if (body == null || body.isBlank()) {
            return new UpstreamError("unknown", "unknown", "上游服务未返回错误详情");
        }
        try {
            JsonNode error = objectMapper.readTree(body).path("error");
            String code = emptyToDefault(error.path("code").asText(null), "unknown");
            String type = emptyToDefault(error.path("type").asText(null), "unknown");
            String message = emptyToDefault(error.path("message").asText(null), snippet(body, 300));
            return new UpstreamError(code, type, message);
        } catch (JsonProcessingException ex) {
            return new UpstreamError("unknown", "unknown", snippet(body, 300));
        }
    }

    private String emptyToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String snippet(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength - 1) + "…";
    }

    private record UpstreamError(String code, String type, String message) {
    }
}
