# AI 论文共写工作台 v2.0.1 当前交付清单

更新时间：`2026-07-11`

当前交付结论：`v1.x MVP 能力完整保留 + v2.0 全学术人群升级 + v2.0.1 学术文档统一工作台首轮收口`。

这里的 100% 指 PRD 中定义的 MVP 验收标准已经完成并通过自动化验证，不包含生产上线能力、完整用户权限、云对象存储、生产级队列和监控体系。

v2.0 基础迁移、章节版本修复和 v2.0.1 质量作用域迁移均已在真实 Supabase 执行并验证。运行时已关闭 JPA 自动建表，后续数据库变更必须继续通过版本化迁移完成。

## 1. 必交付文档

- [README.md](../../README.md)
- [PRD.md](../product/PRD.md)
- [V2_ACADEMIC_UPGRADE.md](../product/V2_ACADEMIC_UPGRADE.md)
- [v2_academic_workspace_api.md](../engineering/v2_academic_workspace_api.md)
- [DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)
- [PRODUCT_COMPLETION_STATUS-6-16.md](PRODUCT_COMPLETION_STATUS-6-16.md)
- [frontend_page_spec.md](../design/frontend_page_spec.md)
- [backend_service_spec.md](../engineering/backend_service_spec.md)
- [api_field_spec.md](../engineering/api_field_spec.md)
- [openapi_contract_draft.md](../engineering/openapi_contract_draft.md)
- [engineering_tasks.md](../engineering/engineering_tasks.md)
- [postgresql_schema.sql](../../postgresql_schema.sql)
- [v1_4_trust_chain.sql](../../backend/db/v1_4_trust_chain.sql)
- [v1_6_trust_delivery_enhancement.sql](../../backend/db/v1_6_trust_delivery_enhancement.sql)
- [v1_9_literature_candidates.sql](../../backend/db/v1_9_literature_candidates.sql)
- [20260711031857_v2_academic_workspace.sql](../../supabase/migrations/20260711031857_v2_academic_workspace.sql)
- [20260711065932_repair_document_section_versions.sql](../../supabase/migrations/20260711065932_repair_document_section_versions.sql)
- [20260711092831_academic_document_quality_scopes.sql](../../supabase/migrations/20260711092831_academic_document_quality_scopes.sql)
- [supabase-setup.md](../guides/supabase-setup.md)
- [frontend/README.md](../../frontend/README.md)

## 2. 必交付前端

前端目录：[frontend](../../frontend)

关键页面：

- [App.jsx](../../frontend/src/App.jsx)
- [ProjectListPage.jsx](../../frontend/src/pages/ProjectListPage.jsx)
- [UploadPage.jsx](../../frontend/src/pages/UploadPage.jsx)
- [ParsingStatusPage.jsx](../../frontend/src/pages/ParsingStatusPage.jsx)
- [MaterialGatePage.jsx](../../frontend/src/pages/MaterialGatePage.jsx)
- [KnowledgeBasePage.jsx](../../frontend/src/pages/KnowledgeBasePage.jsx)
- [AcademicDocumentsPage.jsx](../../frontend/src/pages/AcademicDocumentsPage.jsx)
- [WorkspacePage.jsx](../../frontend/src/pages/WorkspacePage.jsx)
- [ExportPage.jsx](../../frontend/src/pages/ExportPage.jsx)

关键组件与服务：

- [api.js](../../frontend/src/services/api.js)
- [global.css](../../frontend/src/styles/global.css)
- [WorkspaceEditorPanel.jsx](../../frontend/src/components/workspace/WorkspaceEditorPanel.jsx)
- [WorkspaceAiPanel.jsx](../../frontend/src/components/workspace/WorkspaceAiPanel.jsx)
- [WorkspaceReviewSidebar.jsx](../../frontend/src/components/workspace/WorkspaceReviewSidebar.jsx)
- [WorkspaceReviewDrawer.jsx](../../frontend/src/components/workspace/WorkspaceReviewDrawer.jsx)
- [WorkspaceAppealModal.jsx](../../frontend/src/components/workspace/WorkspaceAppealModal.jsx)
- [WorkspaceCoWritePreviewDrawer.jsx](../../frontend/src/components/workspace/WorkspaceCoWritePreviewDrawer.jsx)
- [WorkspaceVersionPanel.jsx](../../frontend/src/components/workspace/WorkspaceVersionPanel.jsx)
- [AcademicDocumentSwitcher.jsx](../../frontend/src/components/academic/AcademicDocumentSwitcher.jsx)
- [AcademicSectionNavigator.jsx](../../frontend/src/components/academic/AcademicSectionNavigator.jsx)
- [AcademicSectionEditor.jsx](../../frontend/src/components/academic/AcademicSectionEditor.jsx)
- [AcademicReadinessPanel.jsx](../../frontend/src/components/academic/AcademicReadinessPanel.jsx)
- [AcademicInspector.jsx](../../frontend/src/components/academic/AcademicInspector.jsx)
- [AcademicChecksDrawer.jsx](../../frontend/src/components/academic/AcademicChecksDrawer.jsx)
- [AcademicCoWritePreviewDrawer.jsx](../../frontend/src/components/academic/AcademicCoWritePreviewDrawer.jsx)
- [AcademicDocumentQualityView.jsx](../../frontend/src/components/academic/AcademicDocumentQualityView.jsx)
- [AcademicSplitModal.jsx](../../frontend/src/components/academic/AcademicSplitModal.jsx)
- [v2-academic-flow.spec.js](../../frontend/e2e/v2-academic-flow.spec.js)
- [playwright.config.js](../../frontend/playwright.config.js)

前端交付确认：

- [x] 路由可正常访问项目列表、上传、解析、材料检查、知识库和学术文档；旧整篇工作台与导出页保留直接路由兼容，但已从导航隐藏。
- [x] 项目创建支持学术阶段、学科、研究范式、首个文档和 AI 使用策略。
- [x] 学术文档页支持多文档切换、章节树拖拽、章节写作 / 整篇检查、动态 readiness、材料范围、选择性章节共写、质量抽屉、版本和组装导出。
- [x] 上传页支持文本、链接、文件、图片与多文件队列。
- [x] 解析页展示真实解析进度、解析质量徽标、问题清单和补充说明入口。
- [x] 材料检查页能阻断材料不足场景，并给出补充建议。
- [x] 材料不足时可检索 Crossref / OpenAlex / Semantic Scholar 元数据候选，使用质量评分、待下载清单和上传关联回流补充真实文献。
- [x] 工作台支持可信链地图、共写预览后应用、审查抽屉、申诉与复查。
- [x] 工作台支持可信链覆盖率、引用一致性提示、原始材料预览入口、共写逐段接受和冲突提示。
- [x] 工作台支持原创实证补强，能提示空泛论证、缺少案例 / 数据 / 来源和 AI 写作味风险。
- [x] `DocumentSection` 是唯一可编辑正文源，整篇视图只读并可从问题跳回对应章节。
- [x] 旧稿拆分必须先预览、再确认，拆分前正文保留在章节版本历史。
- [x] 导出页显示真实下载链接、交付确认、参考文献风险、可信链风险和原创实证风险提示。

## 3. 必交付后端

后端目录：[backend](../../backend)

关键 controller：

- [WorkspaceController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/WorkspaceController.java)
- [MaterialController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/MaterialController.java)
- [RequirementController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/RequirementController.java)
- [SufficiencyController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/SufficiencyController.java)
- [DraftController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/DraftController.java)
- [CoWriteController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/CoWriteController.java)
- [EvidenceBindingController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/EvidenceBindingController.java)
- [KnowledgeBaseController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/KnowledgeBaseController.java)
- [ReviewController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/ReviewController.java)
- [ExportController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/ExportController.java)
- [JobController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/JobController.java)
- [LiteratureSearchController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/LiteratureSearchController.java)
- [WritingRiskController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/WritingRiskController.java)
- [AcademicProfileController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/AcademicProfileController.java)
- [AcademicDocumentController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/AcademicDocumentController.java)
- [AcademicSectionController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/AcademicSectionController.java)
- [AcademicQualityController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/AcademicQualityController.java)

关键 AI 与业务服务：

- [OpenAiSemanticParsingService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiSemanticParsingService.java)
- [OpenAiImageOcrService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiImageOcrService.java)
- [ParseQualityService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ParseQualityService.java)
- [OpenAiDraftGenerationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiDraftGenerationService.java)
- [OpenAiCoWriteService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiCoWriteService.java)
- [EvidenceBindingApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/EvidenceBindingApplicationService.java)
- [KnowledgeBaseApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/KnowledgeBaseApplicationService.java)
- [OpenAiReviewService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiReviewService.java)
- [LiteratureSearchService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/LiteratureSearchService.java)
- [LiteratureCandidateApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/LiteratureCandidateApplicationService.java)
- [WritingRiskApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/WritingRiskApplicationService.java)
- [AcademicProfileApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicProfileApplicationService.java)
- [AcademicDocumentApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicDocumentApplicationService.java)
- [AcademicReadinessApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicReadinessApplicationService.java)
- [AcademicSectionApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicSectionApplicationService.java)
- [AiActionLogApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AiActionLogApplicationService.java)
- [ContentScopeResolverService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ContentScopeResolverService.java)
- [ScopedEvidenceBindingApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ScopedEvidenceBindingApplicationService.java)
- [ScopedEvidenceBindingJobService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ScopedEvidenceBindingJobService.java)
- [AcademicReviewApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicReviewApplicationService.java)
- [AcademicReviewJobService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicReviewJobService.java)
- [AcademicQualityApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicQualityApplicationService.java)
- [AcademicDocumentExportService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicDocumentExportService.java)
- [LegacySectionSplitApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/LegacySectionSplitApplicationService.java)

后端交付确认：

- [x] Spring Boot 可启动。
- [x] `.env` 配置可读取。
- [x] Supabase PostgreSQL 可连接。
- [x] OpenAI 兼容网关可调用。
- [x] 核心 REST API 与前端主链路可联调。
- [x] v2.0 学术画像、多文档、章节、readiness、材料隔离、组装导出和 AI 留痕 API 已接通。
- [x] v2.0.1 章节 / 文档可信链、原创风险、审查复查、质量聚合和选择性共写 API 已接通。
- [x] 真实 Supabase 已完成 v2.0 JSONB、索引、RLS 和旧项目回填迁移。
- [x] 真实 Supabase 已完成 v2.0.1 作用域字段、审查生命周期、共写预览字段、关联表、索引和 RLS 迁移。
- [x] 项目列表已批量读取学术画像，真实 Supabase 下不再逐项目执行兼容初始化查询。
- [x] 章节版本写入使用行锁与历史最大版本，AI 生成期间发生并发修改时返回 `409`，不再触发重复版本 `500`。
- [x] 要求快照支持可选读取；v2 项目没有旧版 snapshot 时返回 `200 + null`，不再记录缺失异常。
- [x] 章节保存只自动重建可信链与本地风险；AI 深度审查仅在用户点击“审查本章 / 整篇”后触发。
- [x] 章节版本变化后旧分析返回 `STALE`；共写预览基准版本冲突返回 `409`。
- [x] 学术文档导出直接读取章节组装快照，不创建可编辑旧 draft。

## 4. 已接真核心能力

- [x] 真实数据库连接。
- [x] 文本材料真实 AI 语义解析。
- [x] 文件材料真实文本抽取。
- [x] 图片 OCR + 语义解析。
- [x] 解析质量清单与补全引导。
- [x] 材料充足性检查。
- [x] 材料不足文献补充入口、多源元数据检索、候选评分与待下载清单。
- [x] 候选文献上传关联；候选原文完成上传和 AI 解析后才进入生成链路。
- [x] 初稿生成。
- [x] 项目知识库构建与检索。
- [x] 段落级材料可信链。
- [x] 材料覆盖率评分与引用一致性检查。
- [x] AI 共写预览后应用。
- [x] 章节共写支持整版 / 逐段 / 差异行应用、冲突提示与审查项关联落库。
- [x] AI 审查、申诉与手动复查。
- [x] 章节 / 整篇可信链、质量聚合、审查和结果时效识别。
- [x] 段落级原创实证不足、空泛论证和 AI 写作味风险提示。
- [x] docx / pdf 导出。

## 5. 自动化测试清单

后端测试覆盖：

- service 层：workspace、material、parse quality、sufficiency、draft、knowledge base、evidence binding、co-write preview、review recheck、writing risk、literature search、export。
- controller 层：workspace、material、draft、co-write、evidence binding、review、writing risk、literature search、export、job 等核心接口。

执行命令：

```bash
cd backend
mvn clean test
```

当前要求：

- [x] `mvn test` 通过，当前 `78` 个后端测试成功，无失败或错误。
- [x] service 层核心路径测试通过。
- [x] controller 层核心接口测试通过。
- [x] 前端 `npm run test` 通过，包含生产构建与 MVP smoke check。
- [x] 前端 `npm run test:e2e` 通过，`2` 个 Chrome 用户流程覆盖画像、上传、多文档、章节生成、共写预览、审查复查与导出。

前端验证命令：

```bash
cd frontend
npm run test
npm run test:e2e
```

## 6. Demo 演示清单

演示前确认：

- [x] 后端已启动。
- [x] 前端已启动。
- [x] OpenAI Key / Base URL / Model 可用。
- [x] Supabase 连接正常。

建议演示路径：

1. 新建研究项目并选择学术阶段、研究范式和首个文档。
2. 上传文本、文件或图片。
3. 执行预处理与 AI 解析。
4. 查看解析质量清单，并按提示补充材料。
5. 执行材料充足性检查；材料不足时检索真实文献候选、加入待下载清单并回上传页关联原文。
6. 上传候选文献原文并完成 AI 解析后，重新检查当前文档 readiness。
7. 构建知识库后进入“学术文档”，切换开题或学位论文并查看默认章节树。
8. 为当前文档选择材料，生成或手动编辑一个章节。
9. 选中章节正文生成共写预览，演示逐段或差异行应用，以及旧基准版本 `409` 保护。
10. 打开本章质量抽屉，查看可信链、原创补强、审查、申诉和单项复查。
11. 切换“整篇检查”，查看聚合质量并点击问题返回对应章节。
12. 组装并导出 docx 或 pdf；确认整篇视图不提供全文编辑。

Demo 指南见：[DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)

## 7. 当前仍属后续增强项

以下内容不阻塞当前 MVP 100% 交付：

- [ ] 用户登录、租户隔离和权限体系。
- [ ] 云对象存储正式化。
- [ ] 生产级异步队列和失败重试。
- [ ] 完整日志、监控与告警。
- [ ] 前端组件级测试与更多异常路径 E2E。
- [ ] PDF 页码级精准原文跳转。
- [ ] 正式论文级 DOCX / PDF 排版增强。
- [ ] 更细粒度词级 diff 与复杂冲突合并。

## 8. 一句话交付结论

`当前项目保留 v1.x 兼容能力，完成 v2.0 全学术人群升级与 v2.0.1 学术文档统一工作台首轮收口；章节已成为唯一正文源，可信审查、选择性共写和整篇交付已迁入主工作台。后端 78 个测试、前端 build/smoke、2 个 Playwright E2E、真实 Supabase 迁移和 API 冒烟均已通过，尚未达到生产上线标准。`
