package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.literature.ExternalSearchLink;
import com.aipm.cowriting.application.dto.literature.LiteratureCandidateResponse;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchItem;
import com.aipm.cowriting.application.dto.literature.LiteratureSearchResponse;
import com.aipm.cowriting.application.service.LiteratureCandidateApplicationService;
import com.aipm.cowriting.application.service.LiteratureSearchService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.time.OffsetDateTime;
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
@AuthenticatedApiTest
class LiteratureSearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LiteratureSearchService literatureSearchService;

    @MockBean
    private LiteratureCandidateApplicationService literatureCandidateApplicationService;

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
                                "Jane Doe. (2024). Intelligent classroom energy management.",
                                92,
                                "推荐引用",
                                List.of("DOI 完整，可追溯", "近五年文献"),
                                List.of(),
                                "doi:10.1234/example",
                                "下载后重点确认研究对象、方法和关键结论。"
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
                .andExpect(jsonPath("$.data.items[0].qualityLabel").value("推荐引用"))
                .andExpect(jsonPath("$.data.items[0].matchedReasons[0]").value("DOI 完整，可追溯"))
                .andExpect(jsonPath("$.data.externalSearchLinks[0].provider").value("Google Scholar"))
                .andExpect(jsonPath("$.data.providerStatus.crossref").value("SUCCESS"));
    }

    @Test
    void saveAndListCandidateShouldUseCandidateService() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        LiteratureCandidateResponse response = candidateResponse(workspaceId, candidateId);
        when(literatureCandidateApplicationService.save(eq(workspaceId), any())).thenReturn(response);
        when(literatureCandidateApplicationService.list(workspaceId)).thenReturn(List.of(response));

        mockMvc.perform(post("/api/v1/workspaces/{id}/literature-candidates", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "provider": "Crossref",
                                  "title": "Intelligent classroom energy management",
                                  "authors": ["Jane Doe"],
                                  "year": "2024",
                                  "doi": "10.1234/example",
                                  "qualityScore": 92,
                                  "qualityLabel": "推荐引用",
                                  "duplicateGroupKey": "doi:10.1234/example"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("TO_DOWNLOAD"))
                .andExpect(jsonPath("$.data.qualityLabel").value("推荐引用"));

        mockMvc.perform(get("/api/v1/workspaces/{id}/literature-candidates", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(candidateId.toString()))
                .andExpect(jsonPath("$.data[0].duplicateGroupKey").value("doi:10.1234/example"));
    }

    private LiteratureCandidateResponse candidateResponse(UUID workspaceId, UUID candidateId) {
        OffsetDateTime now = OffsetDateTime.now();
        return new LiteratureCandidateResponse(
                candidateId,
                workspaceId,
                "Crossref",
                "Intelligent classroom energy management",
                List.of("Jane Doe"),
                "2024",
                "Energy Informatics",
                "Example Publisher",
                "10.1234/example",
                "https://doi.org/10.1234/example",
                "Machine learning reduces energy waste.",
                "Jane Doe. (2024). Intelligent classroom energy management.",
                92,
                "推荐引用",
                List.of("DOI 完整，可追溯"),
                List.of(),
                "doi:10.1234/example",
                "下载后重点确认研究对象、方法和关键结论。",
                "TO_DOWNLOAD",
                null,
                now,
                now
        );
    }
}
