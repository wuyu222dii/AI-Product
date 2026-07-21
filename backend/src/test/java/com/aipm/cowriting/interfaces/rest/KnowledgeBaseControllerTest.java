package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.knowledge.KnowledgeBuildResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeChunkResponse;
import com.aipm.cowriting.application.dto.knowledge.KnowledgeSearchResponse;
import com.aipm.cowriting.application.service.KnowledgeBaseApplicationService;
import com.aipm.cowriting.common.api.PagedResponse;
import com.aipm.cowriting.common.api.Pagination;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = KnowledgeBaseController.class)
@Import(GlobalExceptionHandler.class)
@AuthenticatedApiTest
class KnowledgeBaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private KnowledgeBaseApplicationService knowledgeBaseApplicationService;

    @Test
    void buildShouldReturnKnowledgeSummary() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(knowledgeBaseApplicationService.build(workspaceId))
                .thenReturn(new KnowledgeBuildResponse(
                        workspaceId,
                        2,
                        6,
                        "LEXICAL_READY",
                        "已构建项目知识库，当前使用关键词检索。"
                ));

        mockMvc.perform(post("/api/v1/workspaces/{id}/knowledge-base/build", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.chunkCount").value(6))
                .andExpect(jsonPath("$.data.status").value("LEXICAL_READY"));
    }

    @Test
    void chunksShouldReturnPagedKnowledgeChunks() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        KnowledgeChunkResponse chunk = new KnowledgeChunkResponse(
                UUID.randomUUID(),
                workspaceId,
                materialId,
                "research-note.txt",
                1,
                "人工智能工具提升了资料整理效率。",
                "人工智能工具提升了资料整理效率。",
                List.of("人工智能工具", "资料整理效率"),
                0,
                OffsetDateTime.now()
        );
        when(knowledgeBaseApplicationService.list(workspaceId))
                .thenReturn(new PagedResponse<>(List.of(chunk), new Pagination(1, 1, 1)));

        mockMvc.perform(get("/api/v1/workspaces/{id}/knowledge-base/chunks", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].materialTitle").value("research-note.txt"))
                .andExpect(jsonPath("$.data.items[0].keywords[0]").value("人工智能工具"));
    }

    @Test
    void searchShouldReturnMatchedChunks() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        UUID materialId = UUID.randomUUID();
        KnowledgeChunkResponse chunk = new KnowledgeChunkResponse(
                UUID.randomUUID(),
                workspaceId,
                materialId,
                "research-note.txt",
                1,
                "人工智能工具提升了资料整理效率。",
                "人工智能工具提升了资料整理效率。",
                List.of("人工智能工具", "资料整理效率"),
                0.82,
                OffsetDateTime.now()
        );
        when(knowledgeBaseApplicationService.search(eq(workspaceId), any()))
                .thenReturn(new KnowledgeSearchResponse(workspaceId, "资料整理效率", 1, List.of(chunk)));

        mockMvc.perform(post("/api/v1/workspaces/{id}/knowledge-base/search", workspaceId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "query": "资料整理效率",
                                  "limit": 5
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].score").value(0.82));
    }
}
