# AI 论文共写工作台 v1 Demo 使用说明

## 1. 当前 Demo 能力

当前版本可以完整演示主链路：

1. 创建论文项目。
2. 上传文本、文件、图片或链接作为写作输入。
3. 执行真实预处理与 AI 语义解析。
4. 查看解析质量清单，并按提示补充不完整材料。
5. 执行材料充足性检查。
6. 生成真实初稿。
7. 构建项目知识库并查看可信链。
8. 在工作台中生成共写预览，确认后应用为新版本。
9. 查看真实审查结果，发起申诉或手动复查。
10. 导出真实 `docx / pdf`，并查看交付确认与参考文献风险提示。

当前已接真的核心能力：

- 真实数据库连接。
- 真实文本材料解析。
- 真实文件材料解析。
- 真实图片 OCR。
- 真实 AI 语义解析。
- 真实解析质量清单与补全引导。
- 真实材料充足性检查。
- 真实初稿生成。
- 真实项目知识库。
- 真实段落级材料可信链。
- 真实共写预览后应用。
- 真实审查 / 复审。
- 真实导出。

## 2. 启动前准备

确保 `backend/.env` 中至少配置：

```bash
SUPABASE_DB_HOST=...
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=...
SUPABASE_DB_PASSWORD=...
OPENAI_API_KEY=...
OPENAI_BASE_URL=...
OPENAI_MODEL=...
```

数据库说明见：[supabase-setup.md](supabase-setup.md)

## 3. 启动方式

后端：

```bash
cd backend
mvn spring-boot:run
```

前端：

```bash
cd frontend
npm install
npm run dev
```

默认地址：

- 后端：`http://localhost:8080`
- 前端：`http://localhost:5173`
- 前端代理：`/api -> http://localhost:8080`

## 4. 自动化验证

后端：

```bash
cd backend
mvn clean test
```

前端：

```bash
cd frontend
npm run test
```

当前 MVP 验证口径：

- 后端 `47` 个 service / controller 测试通过。
- 前端 `npm run test` 会执行生产构建与 MVP smoke check。

## 5. 推荐演示路径

### 路径 A：文本直传演示

适合快速演示完整能力。

1. 打开前端首页。
2. 新建论文项目。
3. 在上传页粘贴一段课程论文要求、一段研究笔记或调研结果、一条外部链接或补充说明。
4. 提交并进入解析页。
5. 执行预处理与 AI 解析。
6. 查看每份材料的解析质量清单。
7. 若提示缺少老师要求、文献信息或研究证据，点击“填入补充说明”并重新解析。
8. 进入材料检查。
9. 若材料充足，生成初稿。
10. 在工作台查看可信链、生成共写预览、应用新版本、查看审查项、发起申诉或复查。
11. 导出 docx 或 pdf。

### 路径 B：文件上传演示

适合突出文件理解能力。

建议使用：

- [test-material.txt](../../test-material.txt)
- [test-ocr.png](../../test-ocr.png)

演示步骤：

1. 新建项目。
2. 在上传页选择文件。
3. 将图片或文本文件标记为关键材料。
4. 提交并进入解析页。
5. 观察解析质量检查页的完成度、质量徽标和问题清单。
6. 对需要补充的材料点击“填入补充说明”，提交后重新解析。
7. 进入材料检查、初稿生成和工作台可信链演示。

## 6. 页面展示重点

### 项目首页

- 能创建项目。
- 项目进入后有状态延续。

### 上传页

- 支持文本、链接、文件混合输入。
- 支持多文件队列。
- 支持拖拽上传与关键材料标记。

### 解析质量检查页

- 有总进度。
- 有关键材料完成度。
- 有逐项解析状态。
- 有质量徽标：可用于生成 / 建议确认 / 需要补充 / 解析失败。
- 有解析质量清单：低置信、分类不确定、摘要缺失、证据不足、文献信息不完整、老师要求缺失等。
- 可以把问题建议一键填入补充说明，并重新解析。
- 关键材料为需要补充或解析失败时不能进入下一步。

### 材料检查页

- 明确告诉用户是否可以生成。
- 材料不足时给出缺失项和补充建议。

### 知识库页

- 可构建或重建项目知识库。
- 可查看已入库片段、关键词和来源材料。
- 可用问题检索相关证据片段。

### 共写工作台

- 中间正文为主。
- 可信链地图展示正文段落、证据片段、原始材料和引用信息。
- AI 共写默认先生成预览，不直接覆盖正文。
- 预览抽屉展示差异摘要、修改理由和可能关联的审查项。
- 审查区支持详情抽屉、申诉和单条手动复查。
- 版本区展示历史版本和差异摘要。

### 导出页

- 发起真实导出。
- 获取真实下载链接。
- 查看交付确认。
- 检查参考文献数量与元数据完整性提示。

## 7. 当前适合怎样演示

适合强调：

- 产品闭环。
- AI 驱动的论文写作体验。
- 从输入材料到初稿生成的全过程。
- 可信链、共写、审查、复审、导出的完整主线。

暂时不适合强调：

- 生产级稳定性。
- 权限体系。
- 高并发。
- 云对象存储。
- 生产级队列与监控告警。

当前版本定位：

`MVP 阶段 100% 收口，适合做完整功能演示和产品验证的 Demo`

## 8. 常见问题

### 材料不足无法生成

说明材料类型不够完整。最少建议同时提供：

- 作业要求类材料。
- 参考文献类材料。
- 研究结果 / 草稿类材料。

### AI 解析失败

常见原因：

- `OPENAI_API_KEY` 配置错误。
- `OPENAI_BASE_URL` 不是兼容 API 地址。
- 网络或代理波动。

解析质量检查页会把失败材料标记为“解析失败”，并提示用户重试、补充文字说明或重新上传更清晰文件。

### 导出失败

先确认：

- 后端已启动。
- draft 已成功生成。
- job 接口可返回 `outputRef.downloadUrl`。

## 9. 相关文件

文档：

- [README.md](../../README.md)
- [PRD.md](../product/PRD.md)
- [DEMO_GUIDE.md](DEMO_GUIDE.md)
- [PRODUCT_COMPLETION_STATUS-6-16.md](../project/PRODUCT_COMPLETION_STATUS-6-16.md)
- [supabase-setup.md](supabase-setup.md)

前端：

- [App.jsx](../../frontend/src/App.jsx)
- [UploadPage.jsx](../../frontend/src/pages/UploadPage.jsx)
- [ParsingStatusPage.jsx](../../frontend/src/pages/ParsingStatusPage.jsx)
- [MaterialGatePage.jsx](../../frontend/src/pages/MaterialGatePage.jsx)
- [KnowledgeBasePage.jsx](../../frontend/src/pages/KnowledgeBasePage.jsx)
- [WorkspacePage.jsx](../../frontend/src/pages/WorkspacePage.jsx)
- [ExportPage.jsx](../../frontend/src/pages/ExportPage.jsx)

后端：

- [MaterialController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/MaterialController.java)
- [MaterialApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/MaterialApplicationService.java)
- [ParseQualityService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ParseQualityService.java)
- [OpenAiSemanticParsingService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiSemanticParsingService.java)
- [OpenAiDraftGenerationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiDraftGenerationService.java)
- [OpenAiCoWriteService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiCoWriteService.java)
- [OpenAiReviewService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiReviewService.java)

## 10. 一句话总结

`现在这套系统已经适合做“上传材料 -> 真实解析质量检查 -> 真实生成 -> 可信链 -> 共写预览 -> 审查复查 -> 真实导出”的完整产品演示。`
