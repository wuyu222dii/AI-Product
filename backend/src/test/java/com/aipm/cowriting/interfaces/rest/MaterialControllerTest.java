package com.aipm.cowriting.interfaces.rest;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.aipm.cowriting.application.dto.job.JobResponse;
import com.aipm.cowriting.application.dto.material.MaterialResponse;
import com.aipm.cowriting.application.dto.material.MaterialPreviewResponse;
import com.aipm.cowriting.application.dto.material.ParseQualityReport;
import com.aipm.cowriting.application.dto.reference.BibliographicMetadata;
import com.aipm.cowriting.application.service.LocalMaterialStorageService;
import com.aipm.cowriting.application.service.MaterialApplicationService;
import com.aipm.cowriting.common.web.GlobalExceptionHandler;
import com.aipm.cowriting.domain.model.MaterialCategory;
import com.aipm.cowriting.domain.model.ParseStage;
import java.math.BigDecimal;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = MaterialController.class)
@Import(GlobalExceptionHandler.class)
class MaterialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MaterialApplicationService materialApplicationService;

    @MockBean
    private LocalMaterialStorageService localMaterialStorageService;

    @Test
    void uploadShouldAcceptMultipartFile() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "files",
                "test-material.txt",
                MediaType.TEXT_PLAIN_VALUE,
                "测试内容".getBytes()
        );
        when(localMaterialStorageService.store(eq(workspaceId), any())).thenReturn("/tmp/test-material.txt");
        when(materialApplicationService.createStub(eq(workspaceId), eq("test-material.txt"), eq("txt"), eq("upload"), eq(true), eq("/tmp/test-material.txt"), eq(null), eq(null)))
                .thenReturn(new MaterialResponse(
                        UUID.randomUUID(),
                        "test-material.txt",
                        "txt",
                        "upload",
                        true,
                        ParseStage.PREPROCESSED,
                        BigDecimal.ZERO,
                        OffsetDateTime.now(),
                        null,
                        null,
                        false,
                        null,
                        null,
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        quality("NEEDS_SUPPLEMENT")
                ));

        mockMvc.perform(multipart("/api/v1/workspaces/{id}/materials", workspaceId)
                        .file(file)
                        .param("sourceType", "upload")
                        .param("isKeyMaterial", "true"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].filename").value("test-material.txt"));
    }

    @Test
    void listShouldReturnMaterials() throws Exception {
        UUID workspaceId = UUID.randomUUID();
        when(materialApplicationService.list(workspaceId)).thenReturn(List.of(
                new MaterialResponse(
                        UUID.randomUUID(),
                        "pasted-text.txt",
                        "txt",
                        "pasted_text",
                        true,
                        ParseStage.AI_PARSED,
                        BigDecimal.valueOf(0.95),
                        OffsetDateTime.now(),
                        MaterialCategory.ASSIGNMENT_REQUIREMENT,
                        MaterialCategory.ASSIGNMENT_REQUIREMENT,
                        false,
                        "课程论文要求摘要",
                        "与论文主题直接相关",
                        List.of("课程论文需要围绕智能管理展开"),
                        List.of("高校课堂能耗数据可作为论据"),
                        List.of("至少引用 5 篇参考资料"),
                        new BibliographicMetadata(
                                List.of("张三"),
                                "2021",
                                "高校课堂能源智能管理研究",
                                "物联网教育应用",
                                "高教出版社",
                                "https://example.com/paper",
                                null,
                                "JOURNAL_ARTICLE"
                        ),
                        quality("READY")
                )
        ));

        mockMvc.perform(get("/api/v1/workspaces/{id}/materials", workspaceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.items[0].parseStage").value("AI_PARSED"))
                .andExpect(jsonPath("$.data.items[0].parseQuality.status").value("READY"))
                .andExpect(jsonPath("$.data.items[0].detectedEvidence[0]").value("高校课堂能耗数据可作为论据"))
                .andExpect(jsonPath("$.data.items[0].bibliographicMetadata.authors[0]").value("张三"))
                .andExpect(jsonPath("$.data.items[0].bibliographicMetadata.year").value("2021"));
    }

    @Test
    void aiParseShouldReturnAcceptedJob() throws Exception {
        UUID materialId = UUID.randomUUID();
        when(materialApplicationService.triggerAiParse(materialId))
                .thenReturn(new JobResponse(UUID.randomUUID().toString(), "success"));

        mockMvc.perform(post("/api/v1/materials/{id}/ai-parse", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "forceRetry": true
                                }
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("success"));
    }

    @Test
    void previewShouldReturnMaterialPreview() throws Exception {
        UUID materialId = UUID.randomUUID();
        when(materialApplicationService.preview(materialId))
                .thenReturn(new MaterialPreviewResponse(
                        materialId,
                        "paper.pdf",
                        "pdf",
                        "file",
                        "材料预览文本",
                        "/api/v1/materials/" + materialId + "/file",
                        null
                ));

        mockMvc.perform(get("/api/v1/materials/{id}/preview", materialId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previewType").value("file"))
                .andExpect(jsonPath("$.data.downloadUrl").value("/api/v1/materials/" + materialId + "/file"));
    }

    @Test
    void supplementShouldAcceptMissingPageRef() throws Exception {
        UUID materialId = UUID.randomUUID();
        doNothing().when(materialApplicationService).supplement(eq(materialId), eq("补充老师要求"), eq(null));

        mockMvc.perform(post("/api/v1/materials/{id}/supplement", materialId)
                        .param("supplementText", "补充老师要求"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accepted").value(true))
                .andExpect(jsonPath("$.data.materialId").value(materialId.toString()))
                .andExpect(jsonPath("$.data.pageRef").doesNotExist());
    }

    @Test
    void updateCategoryShouldReturnUpdatedMaterial() throws Exception {
        UUID materialId = UUID.randomUUID();
        when(materialApplicationService.updateCategory(eq(materialId), any()))
                .thenReturn(new MaterialResponse(
                        materialId,
                        "pasted-text.txt",
                        "txt",
                        "pasted_text",
                        true,
                        ParseStage.AI_PARSED,
                        BigDecimal.valueOf(0.95),
                        OffsetDateTime.now(),
                        MaterialCategory.UNKNOWN,
                        MaterialCategory.ASSIGNMENT_REQUIREMENT,
                        true,
                        "课程论文要求摘要",
                        "与论文主题直接相关",
                        List.of(),
                        List.of(),
                        List.of(),
                        null,
                        quality("NEEDS_CONFIRMATION")
                ));

        mockMvc.perform(patch("/api/v1/materials/{id}/category", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "materialCategory": "ASSIGNMENT_REQUIREMENT"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.categoryOverridden").value(true))
                .andExpect(jsonPath("$.data.effectiveMaterialCategory").value("ASSIGNMENT_REQUIREMENT"));
    }

    @Test
    void updateBibliographicMetadataShouldReturnUpdatedMaterial() throws Exception {
        UUID materialId = UUID.randomUUID();
        when(materialApplicationService.updateBibliographicMetadata(eq(materialId), any()))
                .thenReturn(new MaterialResponse(
                        materialId,
                        "paper.pdf",
                        "pdf",
                        "upload",
                        false,
                        ParseStage.AI_PARSED,
                        BigDecimal.valueOf(0.90),
                        OffsetDateTime.now(),
                        MaterialCategory.REFERENCE_MATERIAL,
                        MaterialCategory.REFERENCE_MATERIAL,
                        false,
                        "参考文献摘要",
                        "支撑核心论点",
                        List.of("智能管理可以降低能耗"),
                        List.of("实验显示能耗下降 12%"),
                        List.of(),
                        new BibliographicMetadata(
                                List.of("李四"),
                                "2022",
                                "智能教室能源管理研究",
                                "教育信息化研究",
                                "教育科学出版社",
                                "https://example.com/ref",
                                "10.1234/example",
                                "JOURNAL_ARTICLE"
                        ),
                        quality("READY")
                ));

        mockMvc.perform(patch("/api/v1/materials/{id}/bibliographic-metadata", materialId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "authors": ["李四"],
                                  "year": "2022",
                                  "title": "智能教室能源管理研究",
                                  "sourceTitle": "教育信息化研究",
                                  "publisher": "教育科学出版社",
                                  "url": "https://example.com/ref",
                                  "doi": "10.1234/example",
                                  "publicationType": "JOURNAL_ARTICLE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bibliographicMetadata.authors[0]").value("李四"))
                .andExpect(jsonPath("$.data.bibliographicMetadata.title").value("智能教室能源管理研究"))
                .andExpect(jsonPath("$.data.detectedEvidence[0]").value("实验显示能耗下降 12%"));
    }

    private static ParseQualityReport quality(String status) {
        return new ParseQualityReport(
                status,
                "READY".equals(status) ? BigDecimal.valueOf(0.95) : BigDecimal.valueOf(0.5),
                List.of(),
                Map.of(
                        "summary", "READY".equals(status),
                        "topicRelation", "READY".equals(status),
                        "category", "READY".equals(status),
                        "claims", "READY".equals(status),
                        "evidence", "READY".equals(status),
                        "requirements", false,
                        "bibliographicMetadata", "READY".equals(status)
                ),
                "READY".equals(status) ? "当前解析结果可用于后续材料检查。" : "请按问题清单补充信息后重新解析。"
        );
    }
}
