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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OpenAlexLiteratureClient {

    private static final String OPENALEX_WORKS_URL = "https://api.openalex.org/works";
    private static final int TIMEOUT_SECONDS = 8;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public OpenAlexLiteratureClient(ObjectMapper objectMapper) {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .build(), objectMapper);
    }

    OpenAlexLiteratureClient(HttpClient httpClient, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    public List<LiteratureSearchItem> search(String query, int limit, LiteratureSearchRequest searchRequest) throws IOException, InterruptedException {
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String filter = openAlexFilter(searchRequest);
        String filterParam = filter.isBlank() ? "" : "&filter=" + URLEncoder.encode(filter, StandardCharsets.UTF_8);
        URI uri = URI.create(OPENALEX_WORKS_URL + "?search=" + encodedQuery + "&per-page=" + limit + filterParam);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                .header("Accept", "application/json")
                .header("User-Agent", "AIPM-Cowriting-Workbench/1.0")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("OpenAlex responded with status " + response.statusCode());
        }
        return parseItems(response.body(), limit);
    }

    List<LiteratureSearchItem> parseItems(String body, int limit) throws IOException {
        JsonNode results = objectMapper.readTree(body).path("results");
        if (!results.isArray()) {
            return List.of();
        }
        List<LiteratureSearchItem> items = new ArrayList<>();
        for (JsonNode item : results) {
            String title = text(item.path("display_name"));
            if (isBlank(title)) {
                continue;
            }
            List<String> authors = authors(item.path("authorships"));
            String year = text(item.path("publication_year"));
            String doi = normalizeDoi(text(item.path("doi")));
            JsonNode source = item.path("primary_location").path("source");
            String sourceTitle = text(source.path("display_name"));
            String publisher = text(source.path("host_organization_name"));
            String url = !isBlank(doi) ? "https://doi.org/" + doi : firstNonBlank(
                    text(item.path("primary_location").path("landing_page_url")),
                    text(item.path("id"))
            );
            String abstractSnippet = snippet(reconstructAbstract(item.path("abstract_inverted_index")), 320);
            items.add(new LiteratureSearchItem(
                    "OpenAlex",
                    title,
                    authors,
                    year,
                    sourceTitle,
                    publisher,
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
            if (items.size() >= limit) {
                break;
            }
        }
        return items;
    }

    private List<String> authors(JsonNode authorships) {
        if (!authorships.isArray()) {
            return List.of();
        }
        List<String> authors = new ArrayList<>();
        for (JsonNode authorship : authorships) {
            String name = text(authorship.path("author").path("display_name"));
            if (!isBlank(name)) {
                authors.add(name);
            }
        }
        return authors;
    }

    private String reconstructAbstract(JsonNode invertedIndex) {
        if (!invertedIndex.isObject()) {
            return null;
        }
        Map<Integer, String> words = new TreeMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = invertedIndex.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            if (!entry.getValue().isArray()) {
                continue;
            }
            for (JsonNode position : entry.getValue()) {
                words.put(position.asInt(), entry.getKey());
            }
        }
        return String.join(" ", words.values());
    }

    private String openAlexFilter(LiteratureSearchRequest request) {
        if (request == null) {
            return "";
        }
        List<String> filters = new ArrayList<>();
        if (request.yearFrom() != null) {
            filters.add("from_publication_date:" + request.yearFrom() + "-01-01");
        }
        if (request.yearTo() != null) {
            filters.add("to_publication_date:" + request.yearTo() + "-12-31");
        }
        String type = firstOpenAlexType(request.workTypes());
        if (!isBlank(type)) {
            filters.add("type:" + type);
        }
        return String.join(",", filters);
    }

    private String firstOpenAlexType(List<String> workTypes) {
        if (workTypes == null || workTypes.isEmpty()) {
            return null;
        }
        return switch (workTypes.get(0)) {
            case "journal_article" -> "article";
            case "book_chapter" -> "book-chapter";
            case "conference_paper" -> "proceedings-article";
            case "book" -> "book";
            case "dataset" -> "dataset";
            default -> null;
        };
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

    private String normalizeDoi(String doi) {
        if (isBlank(doi)) {
            return null;
        }
        return doi.replace("https://doi.org/", "").trim();
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

    private String firstNonBlank(String first, String second) {
        return isBlank(first) ? second : first;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
