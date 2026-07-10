package com.aipm.cowriting.application.service;

import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchRequest;
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
public class SemanticScholarLiteratureClient {

    private static final String SEMANTIC_SEARCH_URL = "https://api.semanticscholar.org/graph/v1/paper/search";
    private static final int TIMEOUT_SECONDS = 8;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public SemanticScholarLiteratureClient(ObjectMapper objectMapper) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build(), objectMapper);
    }

    SemanticScholarLiteratureClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<LiteratureSearchItem> search(String query, int limit, LiteratureSearchRequest searchRequest) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        URI uri = URI.create(SEMANTIC_SEARCH_URL
                + "?query=" + encodedQuery
                + "&limit=" + limit
                + semanticYearParam(searchRequest)
                + "&fields=title,authors,year,venue,url,abstract,externalIds,publicationTypes");
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("User-Agent", "AIPM-Cowriting-Workbench/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("Semantic Scholar responded with status " + response.statusCode());
        }
        return parseItems(response.body(), limit);
    }

    List<LiteratureSearchItem> parseItems(String body, int limit) throws IOException {
        JsonNode data = objectMapper.readTree(body).path("data");
        if (!data.isArray()) {
            return List.of();
        }
        List<LiteratureSearchItem> result = new ArrayList<>();
        for (JsonNode item : data) {
            String title = text(item.path("title"));
            if (isBlank(title)) {
                continue;
            }
            List<String> authors = authors(item.path("authors"));
            String year = text(item.path("year"));
            String sourceTitle = text(item.path("venue"));
            String doi = text(item.path("externalIds").path("DOI"));
            String url = !isBlank(doi) ? "https://doi.org/" + doi : text(item.path("url"));
            String abstractSnippet = snippet(text(item.path("abstract")), 320);
            result.add(new LiteratureSearchItem(
                    "Semantic Scholar",
                    title,
                    authors,
                    year,
                    sourceTitle,
                    null,
                    doi,
                    url,
                    abstractSnippet,
                    citationPreview(authors, year, title, sourceTitle, doi, url),
                    null,
                    null,
                    List.of(),
                    List.of(),
                    null,
                    null
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
            String name = text(author.path("name"));
            if (!isBlank(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private String semanticYearParam(LiteratureSearchRequest request) {
        if (request == null || (request.yearFrom() == null && request.yearTo() == null)) {
            return "";
        }
        String from = request.yearFrom() == null ? "" : String.valueOf(request.yearFrom());
        String to = request.yearTo() == null ? "" : String.valueOf(request.yearTo());
        return "&year=" + from + "-" + to;
    }

    private String citationPreview(List<String> authors, String year, String title, String sourceTitle, String doi, String url) {
        List<String> parts = new ArrayList<>();
        if (!authors.isEmpty()) parts.add(String.join(", ", authors));
        if (!isBlank(year)) parts.add("(" + year + ")");
        parts.add(title);
        if (!isBlank(sourceTitle)) parts.add(sourceTitle);
        if (!isBlank(doi)) parts.add("https://doi.org/" + doi);
        else if (!isBlank(url)) parts.add(url);
        return String.join(". ", parts);
    }

    private String text(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return isBlank(value) ? null : value.trim();
    }

    private String snippet(String value, int maxLength) {
        if (isBlank(value)) return null;
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
