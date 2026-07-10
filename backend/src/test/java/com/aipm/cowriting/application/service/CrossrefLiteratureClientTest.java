package com.aipm.cowriting.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

class CrossrefLiteratureClientTest {

    @Test
    void parseItemsShouldMapCrossrefMetadata() throws Exception {
        CrossrefLiteratureClient client = new CrossrefLiteratureClient(new ObjectMapper());

        var items = client.parseItems("""
                {
                  "message": {
                    "items": [
                      {
                        "title": ["Intelligent classroom energy management"],
                        "author": [
                          { "given": "Jane", "family": "Doe" },
                          { "literal": "Research Group" }
                        ],
                        "issued": { "date-parts": [[2024, 5, 1]] },
                        "container-title": ["Energy Informatics"],
                        "publisher": "Example Publisher",
                        "DOI": "10.1234/example",
                        "abstract": "<jats:p>Machine learning reduces energy waste.</jats:p>"
                      }
                    ]
                  }
                }
                """, 10);

        assertThat(items).hasSize(1);
        assertThat(items.get(0).provider()).isEqualTo("Crossref");
        assertThat(items.get(0).title()).isEqualTo("Intelligent classroom energy management");
        assertThat(items.get(0).authors()).containsExactly("Jane Doe", "Research Group");
        assertThat(items.get(0).year()).isEqualTo("2024");
        assertThat(items.get(0).sourceTitle()).isEqualTo("Energy Informatics");
        assertThat(items.get(0).url()).isEqualTo("https://doi.org/10.1234/example");
        assertThat(items.get(0).abstractSnippet()).isEqualTo("Machine learning reduces energy waste.");
        assertThat(items.get(0).citationPreview()).contains("Jane Doe", "2024", "10.1234/example");
    }
}
