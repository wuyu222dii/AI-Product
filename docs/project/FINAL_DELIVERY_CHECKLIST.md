# AI 论文共写工作台 v1 最终交付清单

更新时间：`2026-06-18`

当前交付结论：`MVP 阶段 100% 收口`。这里的 100% 指 PRD 第 25 节定义的 MVP 验收标准已经完成并通过自动化验证，不包含生产上线能力、完整用户权限、云对象存储、生产级队列和监控体系。

## 1. 交付目标

本清单用于在项目交付、演示、汇报或内部移交前，统一检查：

- 文档是否齐全
- 前后端代码是否可运行
- 核心能力是否已接真
- 自动化测试是否可执行
- Demo 是否可完整走通
- 当前遗留问题是否已明确

---

## 2. 必交付文档

以下文档应作为正式交付包的一部分保留：

- [README.md](../../README.md)
- [PRD.md](../product/PRD.md)
- [DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)
- [current_progress_status.md](../project/current_progress_status.md)
- [frontend_page_spec.md](../design/frontend_page_spec.md)
- [backend_service_spec.md](../engineering/backend_service_spec.md)
- [api_field_spec.md](../engineering/api_field_spec.md)
- [openapi_contract_draft.md](../engineering/openapi_contract_draft.md)
- [postgresql_schema.sql](/Users/harry/Documents/AIPM/postgresql_schema.sql)
- [engineering_tasks.md](../engineering/engineering_tasks.md)
- [backend/SUPABASE_SETUP.md](../guides/supabase-setup.md)
- [frontend/README.md](/Users/harry/Documents/AIPM/frontend/README.md)

---

## 3. 必交付前端

前端交付目录：

- [frontend](/Users/harry/Documents/AIPM/frontend)

关键文件：

- [frontend/src/App.jsx](/Users/harry/Documents/AIPM/frontend/src/App.jsx)
- [frontend/src/pages/ProjectListPage.jsx](/Users/harry/Documents/AIPM/frontend/src/pages/ProjectListPage.jsx)
- [frontend/src/pages/UploadPage.jsx](/Users/harry/Documents/AIPM/frontend/src/pages/UploadPage.jsx)
- [frontend/src/pages/ParsingStatusPage.jsx](/Users/harry/Documents/AIPM/frontend/src/pages/ParsingStatusPage.jsx)
- [frontend/src/pages/MaterialGatePage.jsx](/Users/harry/Documents/AIPM/frontend/src/pages/MaterialGatePage.jsx)
- [frontend/src/pages/WorkspacePage.jsx](/Users/harry/Documents/AIPM/frontend/src/pages/WorkspacePage.jsx)
- [frontend/src/pages/ExportPage.jsx](/Users/harry/Documents/AIPM/frontend/src/pages/ExportPage.jsx)
- [frontend/src/services/api.js](/Users/harry/Documents/AIPM/frontend/src/services/api.js)
- [frontend/src/styles/global.css](/Users/harry/Documents/AIPM/frontend/src/styles/global.css)

工作台子组件：

- [frontend/src/components/workspace/WorkspaceReviewSidebar.jsx](/Users/harry/Documents/AIPM/frontend/src/components/workspace/WorkspaceReviewSidebar.jsx)
- [frontend/src/components/workspace/WorkspaceReviewDrawer.jsx](/Users/harry/Documents/AIPM/frontend/src/components/workspace/WorkspaceReviewDrawer.jsx)
- [frontend/src/components/workspace/WorkspaceAppealModal.jsx](/Users/harry/Documents/AIPM/frontend/src/components/workspace/WorkspaceAppealModal.jsx)
- [frontend/src/components/workspace/WorkspaceEditorPanel.jsx](/Users/harry/Documents/AIPM/frontend/src/components/workspace/WorkspaceEditorPanel.jsx)
- [frontend/src/components/workspace/WorkspaceAiPanel.jsx](/Users/harry/Documents/AIPM/frontend/src/components/workspace/WorkspaceAiPanel.jsx)
- [frontend/src/components/workspace/WorkspaceVersionPanel.jsx](/Users/harry/Documents/AIPM/frontend/src/components/workspace/WorkspaceVersionPanel.jsx)

前端构建要求：

- `npm install`
- `npm run build`

交付时需确认：

- [x] 路由可正常访问 `/projects /upload /parsing /gate /workspace /export`
- [x] 上传页支持文本、链接、文件混合输入
- [x] 解析状态页显示真实进度
- [x] 工作台支持真实共写、审查、申诉与版本区
- [x] 导出页显示真实下载链接与交付确认状态

---

## 4. 必交付后端

后端交付目录：

- [backend](/Users/harry/Documents/AIPM/backend)

关键代码结构：

- `common`
- `domain`
- `application`
- `interfaces/rest`

关键 controller：

- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/WorkspaceController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/WorkspaceController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/MaterialController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/MaterialController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/RequirementController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/RequirementController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/SufficiencyController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/SufficiencyController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/DraftController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/DraftController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/CoWriteController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/CoWriteController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/ReviewController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/ReviewController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/ExportController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/ExportController.java)
- [backend/src/main/java/com/aipm/cowriting/interfaces/rest/JobController.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/interfaces/rest/JobController.java)

关键 AI 能力服务：

- [backend/src/main/java/com/aipm/cowriting/application/service/OpenAiSemanticParsingService.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/application/service/OpenAiSemanticParsingService.java)
- [backend/src/main/java/com/aipm/cowriting/application/service/OpenAiImageOcrService.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/application/service/OpenAiImageOcrService.java)
- [backend/src/main/java/com/aipm/cowriting/application/service/OpenAiDraftGenerationService.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/application/service/OpenAiDraftGenerationService.java)
- [backend/src/main/java/com/aipm/cowriting/application/service/OpenAiCoWriteService.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/application/service/OpenAiCoWriteService.java)
- [backend/src/main/java/com/aipm/cowriting/application/service/OpenAiReviewService.java](/Users/harry/Documents/AIPM/backend/src/main/java/com/aipm/cowriting/application/service/OpenAiReviewService.java)

后端启动要求：

- `mvn spring-boot:run`

交付时需确认：

- [x] Spring Boot 可成功启动
- [x] `.env` 配置正确读取
- [x] 数据库可连接
- [x] OpenAI 配置可用
- [x] 核心 REST API 可访问

---

## 5. 已接真的核心能力清单

以下能力当前应视为“已接真”并应在交付说明中明确：

- [x] 真实数据库连接
- [x] 文本材料真实语义解析
- [x] 文件材料真实文本抽取
- [x] 图片 OCR + 语义解析
- [x] 材料充足性检查
- [x] 初稿生成
- [x] AI 共写
- [x] AI 审查
- [x] AI 复审
- [x] 导出 docx/pdf

---

## 6. 自动化测试清单

后端测试文件：

- [backend/src/test/java/com/aipm/cowriting/application/service/WorkspaceApplicationServiceTest.java](/Users/harry/Documents/AIPM/backend/src/test/java/com/aipm/cowriting/application/service/WorkspaceApplicationServiceTest.java)
- [backend/src/test/java/com/aipm/cowriting/application/service/SufficiencyApplicationServiceTest.java](/Users/harry/Documents/AIPM/backend/src/test/java/com/aipm/cowriting/application/service/SufficiencyApplicationServiceTest.java)
- [backend/src/test/java/com/aipm/cowriting/application/service/DraftApplicationServiceTest.java](/Users/harry/Documents/AIPM/backend/src/test/java/com/aipm/cowriting/application/service/DraftApplicationServiceTest.java)
- [backend/src/test/java/com/aipm/cowriting/interfaces/rest/WorkspaceControllerTest.java](/Users/harry/Documents/AIPM/backend/src/test/java/com/aipm/cowriting/interfaces/rest/WorkspaceControllerTest.java)
- [backend/src/test/java/com/aipm/cowriting/interfaces/rest/DraftControllerTest.java](/Users/harry/Documents/AIPM/backend/src/test/java/com/aipm/cowriting/interfaces/rest/DraftControllerTest.java)
- [backend/src/test/java/com/aipm/cowriting/interfaces/rest/MaterialControllerTest.java](/Users/harry/Documents/AIPM/backend/src/test/java/com/aipm/cowriting/interfaces/rest/MaterialControllerTest.java)

执行命令：

```bash
cd /Users/harry/Documents/AIPM/backend
mvn test
```

当前要求：

- [x] `mvn clean test` 通过，当前 `40` 个后端测试成功
- [x] 核心 service 层测试通过
- [x] 核心 controller 层测试通过
- [x] 前端 `npm run test` 通过，包含生产构建与 MVP smoke check

---

## 7. Demo 演示清单

演示前应确保：

- [x] 后端已启动
- [x] 前端已启动
- [x] OpenAI Key 可用
- [x] Supabase 连接正常

建议演示路径：

1. 新建项目
2. 上传文本 / 文件 / 图片
3. 执行解析
4. 查看解析进度
5. 进入材料检查
6. 生成初稿
7. 执行共写
8. 查看审查结果
9. 发起申诉
10. 导出文件

Demo 指南见：

- [DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)

---

## 8. 交付前必查

### 前端
- [x] `npm install`
- [x] `npm run test`
- [x] `npm run build`
- [x] 路由页面可进入
- [x] 上传/解析/共写/导出页面无明显报错

### 后端
- [x] `mvn clean test`
- [x] `mvn spring-boot:run`
- [x] 核心接口可访问
- [x] 真实链路至少手工走通一遍

### 联调
- [x] 前端能创建 workspace
- [x] 前端上传材料后能看到解析状态变化
- [x] 充足性检查结果合理
- [x] draft 能成功生成
- [x] co-write 能生成新版本
- [x] review-items 能返回真实结果
- [x] appeal 能返回复审结果
- [x] export 能拿到下载地址

---

## 9. 当前仍属后续增强项

以下内容不阻塞当前 MVP 100% 交付，应明确为后续产品化增强，而不是误判为 MVP 未完成：

- [ ] 对象存储正式化
- [ ] 真正异步任务队列
- [ ] 完整鉴权 / 权限体系
- [ ] 更完整日志与监控
- [ ] 前端 Playwright / Cypress E2E 测试
- [ ] 更细粒度版本 diff
- [ ] 更强的文件格式支持

---

## 10. 一句话交付结论

`当前项目已经达到 MVP 阶段 100% 交付状态：PRD MVP 验收项已完成，前后端主链路可运行，真实 AI / Supabase / 导出链路已接通，后端 40 个测试与前端 MVP smoke test 均已通过。`
