package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class CrossrefLiteratureClient {

    private static final String CROSSREF_WORKS_URL = "https://api.crossref.org/works";
    private static final int TIMEOUT_SECONDS = 8;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public CrossrefLiteratureClient(ObjectMapper objectMapper) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build(), objectMapper);
    }

    CrossrefLiteratureClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<LiteratureSearchItem> search(String query, int limit) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create(CROSSREF_WORKS_URL + "?query.bibliographic=" + encodedQuery + "&rows=" + limit);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("User-Agent", "AIPM-Cowriting-Workbench/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Crossref responded with status " + response.statusCode());
        }
        return parseItems(response.body(), limit);
    }

    List<LiteratureSearchItem> parseItems(String body, int limit) throws IOException {
        JsonNode items = objectMapper.readTree(body).path("message").path("items");
        if (!items.isArray()) {
            return List.of();
        }

        List<LiteratureSearchItem> result = new ArrayList<>();
        for (JsonNode item : items) {
            String title = firstText(item.path("title"));
            if (isBlank(title)) {
                continue;
            }
            List<String> authors = authors(item.path("author"));
            String year = year(item);
            String sourceTitle = firstText(item.path("container-title"));
            String publisher = text(item.path("publisher"));
            String doi = text(item.path("DOI"));
            String url = !isBlank(doi) ? "https://doi.org/" + doi : text(item.path("URL"));
            String abstractSnippet = snippet(cleanAbstract(text(item.path("abstract"))), 320);
            String citationPreview = citationPreview(authors, year, title, sourceTitle, doi, url);

            result.add(new LiteratureSearchItem(
                    "Crossref",
                    title,
                    authors,
                    year,
                    sourceTitle,
                    publisher,
                    doi,
                    url,
                    abstractSnippet,
                    citationPreview
            ));
            if (result.size() >= limit) {
                break;
            }
        }
        return result;
    }

    private List<String> authors(JsonNode authorNode) {
        if (!authorNode.isArray()) {
            return List.of();
        }
        List<String> authors = new ArrayList<>();
        for (JsonNode author : authorNode) {
            String literal = text(author.path("literal"));
            String given = text(author.path("given"));
            String family = text(author.path("family"));
            String name = !isBlank(literal)
                    ? literal
                    : (firstNonBlank(given, "") + " " + firstNonBlank(family, "")).trim();
            if (!isBlank(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private String year(JsonNode item) {
        String issued = yearFromDateParts(item.path("issued").path("date-parts"));
        if (!isBlank(issued)) {
            return issued;
        }
        String publishedOnline = yearFromDateParts(item.path("published-online").path("date-parts"));
        if (!isBlank(publishedOnline)) {
            return publishedOnline;
        }
        return yearFromDateParts(item.path("published-print").path("date-parts"));
    }

    private String yearFromDateParts(JsonNode dateParts) {
        if (!dateParts.isArray() || dateParts.isEmpty() || !dateParts.get(0).isArray() || dateParts.get(0).isEmpty()) {
            return null;
        }
        return dateParts.get(0).get(0).asText(null);
    }

    private String firstText(JsonNode node) {
        if (node.isArray() && !node.isEmpty()) {
            return text(node.get(0));
        }
        return text(node);
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return isBlank(value) ? null : value.trim();
    }

    private String cleanAbstract(String abstractText) {
        if (isBlank(abstractText)) {
            return null;
        }
        return abstractText
                .replaceAll("<[^>]+>", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String snippet(String value, int maxLength) {
        if (isBlank(value)) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private String citationPreview(
            List<String> authors,
            String year,
            String title,
            String sourceTitle,
            String doi,
            String url
    ) {
        List<String> parts = new ArrayList<>();
        if (!authors.isEmpty()) {
            parts.add(String.join(", ", authors));
        }
        if (!isBlank(year)) {
            parts.add("(" + year + ")");
        }
        parts.add(title);
        if (!isBlank(sourceTitle)) {
            parts.add(sourceTitle);
        }
        if (!isBlank(doi)) {
            parts.add("https://doi.org/" + doi);
        } else if (!isBlank(url)) {
            parts.add(url);
        }
        return String.join(". ", parts);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String firstNonBlank(String value, String fallback) {
        return isBlank(value) ? fallback : value;
    }
}
