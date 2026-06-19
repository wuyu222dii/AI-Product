package com.aipm.cowriting.application.dto.reference;

import java.util.List;

public record BibliographicMetadata(
        List<String> authors,
        String year,
        String title,
        String sourceTitle,
        String publisher,
        String url,
        String doi,
        String publicationType
) {
    public BibliographicMetadata {
        authors = authors == null
                ? List.of()
                : authors.stream()
                        .filter(author -> author != null && !author.isBlank())
                        .map(String::trim)
                        .toList();
        year = normalize(year);
        title = normalize(title);
        sourceTitle = normalize(sourceTitle);
        publisher = normalize(publisher);
        url = normalize(url);
        doi = normalize(doi);
        publicationType = normalize(publicationType);
    }

    public static BibliographicMetadata empty() {
        return new BibliographicMetadata(List.of(), null, null, null, null, null, null, null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
