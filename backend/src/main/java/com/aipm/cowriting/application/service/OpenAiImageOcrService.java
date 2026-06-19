package com.aipm.cowriting.application.service;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class OpenAiImageOcrService {

    private final OpenAiProperties openAiProperties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiImageOcrService(OpenAiProperties openAiProperties, ObjectMapper objectMapper) {
        this.openAiProperties = openAiProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(openAiProperties.getTimeoutSeconds()))
                .build();
    }

    public String extractText(Path imagePath, String fileType) {
        if (openAiProperties.getApiKey() == null || openAiProperties.getApiKey().isBlank()) {
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.SERVICE_UNAVAILABLE.value(),
                    "未配置 OPENAI_API_KEY，无法执行图片 OCR"
            );
        }
        try {
            byte[] bytes = Files.readAllBytes(imagePath);
            String mimeType = mapMimeType(fileType);
            String dataUrl = "data:" + mimeType + ";base64," + Base64.getEncoder().encodeToString(bytes);

            Map<String, Object> body = Map.of(
                    "model", openAiProperties.getModel(),
                    "messages", List.of(
                            Map.of(
                                    "role", "system",
                                    "content", "You are an OCR assistant. Extract all visible text from the image and return plain text only."
                            ),
                            Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of("type", "text", "text", "Extract all visible text from this image. Return plain text only."),
                                            Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                                    )
                            )
                    )
            );

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
                        "OpenAI 图片 OCR 请求失败",
                        Map.of("statusCode", response.statusCode(), "body", response.body())
                );
            }

            JsonNode root = objectMapper.readTree(response.body());
            String content = root.path("choices").get(0).path("message").path("content").asText("");
            return content == null ? "" : content.trim();
        } catch (IOException e) {
            throw new BusinessException(
                    ErrorCode.MATERIAL_PARSE_FAILED,
                    HttpStatus.BAD_GATEWAY.value(),
                    "图片 OCR 结果解析失败"
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(
                    ErrorCode.AI_SERVICE_UNAVAILABLE,
                    HttpStatus.BAD_GATEWAY.value(),
                    "图片 OCR 请求被中断"
            );
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

    private String mapMimeType(String fileType) {
        return switch (fileType.toLowerCase()) {
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "heic" -> "image/heic";
            case "tif", "tiff" -> "image/tiff";
            default -> "application/octet-stream";
        };
    }
}
