# AI 论文共写工作台 v1 最终交付清单

更新时间：`2026-06-20`

当前交付结论：`MVP 阶段 100% 收口 + v1.5 AI 解析质量清单完成`。

这里的 100% 指 PRD 中定义的 MVP 验收标准已经完成并通过自动化验证，不包含生产上线能力、完整用户权限、云对象存储、生产级队列和监控体系。

## 1. 必交付文档

- [README.md](../../README.md)
- [PRD.md](../product/PRD.md)
- [DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)
- [PRODUCT_COMPLETION_STATUS-6-16.md](PRODUCT_COMPLETION_STATUS-6-16.md)
- [frontend_page_spec.md](../design/frontend_page_spec.md)
- [backend_service_spec.md](../engineering/backend_service_spec.md)
- [api_field_spec.md](../engineering/api_field_spec.md)
- [openapi_contract_draft.md](../engineering/openapi_contract_draft.md)
- [engineering_tasks.md](../engineering/engineering_tasks.md)
- [postgresql_schema.sql](../../postgresql_schema.sql)
- [v1_4_trust_chain.sql](../../backend/db/v1_4_trust_chain.sql)
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

前端交付确认：

- [x] 路由可正常访问项目列表、上传、解析、材料检查、知识库、工作台、导出页。
- [x] 上传页支持文本、链接、文件、图片与多文件队列。
- [x] 解析页展示真实解析进度、解析质量徽标、问题清单和补充说明入口。
- [x] 材料检查页能阻断材料不足场景，并给出补充建议。
- [x] 工作台支持可信链地图、共写预览后应用、审查抽屉、申诉与复查。
- [x] 导出页显示真实下载链接、交付确认和参考文献风险提示。

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

关键 AI 与业务服务：

- [OpenAiSemanticParsingService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiSemanticParsingService.java)
- [OpenAiImageOcrService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiImageOcrService.java)
- [ParseQualityService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ParseQualityService.java)
- [OpenAiDraftGenerationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiDraftGenerationService.java)
- [OpenAiCoWriteService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiCoWriteService.java)
- [EvidenceBindingApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/EvidenceBindingApplicationService.java)
- [KnowledgeBaseApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/KnowledgeBaseApplicationService.java)
- [OpenAiReviewService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiReviewService.java)

后端交付确认：

- [x] Spring Boot 可启动。
- [x] `.env` 配置可读取。
- [x] Supabase PostgreSQL 可连接。
- [x] OpenAI 兼容网关可调用。
- [x] 核心 REST API 与前端主链路可联调。

## 4. 已接真核心能力

- [x] 真实数据库连接。
- [x] 文本材料真实 AI 语义解析。
- [x] 文件材料真实文本抽取。
- [x] 图片 OCR + 语义解析。
- [x] 解析质量清单与补全引导。
- [x] 材料充足性检查。
- [x] 初稿生成。
- [x] 项目知识库构建与检索。
- [x] 段落级材料可信链。
- [x] AI 共写预览后应用。
- [x] AI 审查、申诉与手动复查。
- [x] docx / pdf 导出。

## 5. 自动化测试清单

后端测试覆盖：

- service 层：workspace、material、parse quality、sufficiency、draft、knowledge base、evidence binding、co-write preview、review recheck、export。
- controller 层：workspace、material、draft、co-write、evidence binding、review、export、job 等核心接口。

执行命令：

```bash
cd backend
mvn clean test
```

当前要求：

- [x] `mvn clean test` 通过，当前 `47` 个后端测试成功。
- [x] service 层核心路径测试通过。
- [x] controller 层核心接口测试通过。
- [x] 前端 `npm run test` 通过，包含生产构建与 MVP smoke check。

前端验证命令：

```bash
cd frontend
npm run test
```

## 6. Demo 演示清单

演示前确认：

- [x] 后端已启动。
- [x] 前端已启动。
- [x] OpenAI Key / Base URL / Model 可用。
- [x] Supabase 连接正常。

建议演示路径：

1. 新建项目。
2. 上传文本、文件或图片。
3. 执行预处理与 AI 解析。
4. 查看解析质量清单，并按提示补充材料。
5. 执行材料充足性检查。
6. 生成初稿。
7. 查看项目知识库与材料可信链。
8. 生成共写预览，确认后应用为新版本。
9. 查看审查项、发起申诉或手动复查。
10. 导出 docx 或 pdf。

Demo 指南见：[DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)

## 7. 当前仍属后续增强项

以下内容不阻塞当前 MVP 100% 交付：

- [ ] 用户登录、租户隔离和权限体系。
- [ ] 云对象存储正式化。
- [ ] 生产级异步队列和失败重试。
- [ ] 完整日志、监控与告警。
- [ ] Playwright / Cypress E2E 测试。
- [ ] PDF 页码级原文跳转与材料覆盖率评分。
- [ ] 更细粒度版本 diff 与引用一致性检查。

## 8. 一句话交付结论

`当前项目已经达到 MVP 阶段 100% 交付状态，并完成 v1.5 AI 解析质量清单：PRD MVP 验收项已完成，真实 AI / Supabase / 知识库 / 可信链 / 共写预览 / 审查复查 / 导出链路已接通，后端 47 个测试与前端 MVP smoke test 均已通过。`
