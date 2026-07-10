package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.literature.ExternalSearchLink;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchResponse;
import com.aipm.cowriting.application.service.LiteratureSearchService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = LiteratureSearchController.class)
@Import(GlobalExceptionHandler.class)
class LiteratureSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LiteratureSearchService literatureSearchService;

    @Test
    void searchShouldReturnLiteratureCandidatesAndExternalLinks() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(literatureSearchService.search(eq(workspaceId), any()))
                .thenReturn(new LiteratureSearchResponse(
                        "智能教室 能源管理",
                        List.of(new LiteratureSearchItem(
                                "Crossref",
                                "Intelligent classroom energy management",
                                List.of("Jane Doe"),
                                "2024",
                                "Energy Informatics",
                                "Example Publisher",
                                "10.1234/example",
                                "https://doi.org/10.1234/example",
                                "Machine learning reduces energy waste.",
                                "Jane Doe. (2024). Intelligent classroom energy management."
                        )),
                        List.of(new ExternalSearchLink(
                                "Google Scholar",
                                "https://scholar.google.com/scholar?q=test"
                        )),
                        Map.of("crossref", "SUCCESS")
                ));

        mockMvc.perform(post("/api/v1/workspaces/{id}/literature-search", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "智能教室 能源管理",
                                  "source": "crossref",
                                  "limit": 10,
                                  "missingItemType": "reference_material"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].provider").value("Crossref"))
                .andExpect(jsonPath("$.data.items[0].doi").value("10.1234/example"))
                .andExpect(jsonPath("$.data.externalSearchLinks[0].provider").value("Google Scholar"))
                .andExpect(jsonPath("$.data.providerStatus.crossref").value("SUCCESS"));
    }
}
