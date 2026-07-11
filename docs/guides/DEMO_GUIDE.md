# AI 论文共写工作台 v2.0.1 Demo 使用说明

## 1. 当前 Demo 能力

当前版本可以完整演示主链路：

1. 创建研究项目并设置学术阶段、研究范式和首个文档类型。
2. 上传文本、文件、图片或链接作为写作输入。
3. 执行真实预处理与 AI 语义解析。
4. 查看解析质量清单，并按提示补充不完整材料。
5. 按当前文档类型和研究范式执行动态 readiness；若材料不足，可直接进入文献补充入口。
6. 构建项目知识库。
7. 创建或切换开题、学位论文、论文稿等多个学术文档。
8. 在章节树中拖拽整理顺序，再生成、编辑、选区共写并选择性应用为章节新版本。
9. 在本章质量抽屉查看可信链、原创补强、审查、申诉和单项复查。
10. 切换整篇检查，查看章节聚合质量并从问题跳回对应章节。
11. 组装并导出真实 `docx / pdf`；整篇视图不直接编辑全文。

当前已接真的核心能力：

- 真实数据库连接。
- 真实文本材料解析。
- 真实文件材料解析。
- 真实图片 OCR。
- 真实 AI 语义解析。
- 真实解析质量清单与补全引导。
- 真实材料充足性检查。
- 真实材料不足文献补充增强：Crossref / OpenAlex / Semantic Scholar 站内候选文献、质量评分、待下载清单、Google Scholar / 知网外部检索入口。
- 真实初稿生成。
- 真实项目知识库。
- 真实段落级材料可信链。
- 真实材料覆盖率评分与引用一致性检查。
- 真实原创实证补强：段落级 AI 写作味风险、空泛论证和缺实证提示。
- 真实共写预览后应用。
- 真实共写逐段接受、冲突提示和审查项关联落库。
- 真实审查 / 复审。
- 真实导出。
- 真实学术项目画像、多文档和章节树。
- 真实文档级 / 章节级动态 readiness。
- 真实文档材料隔离、章节版本与 AI 使用记录。
- 真实章节 / 文档作用域可信链与质量聚合。
- 真实章节选区共写、整版 / 逐段 / 差异行应用和 `409` 版本冲突保护。
- 真实章节 / 整篇手动 AI 审查、结果时效识别和“已修改待复查”状态。
- 真实旧稿标题识别、预览确认拆分和章节只读组装交付。

## 2. 启动前准备

确保 `backend/.env` 中至少配置：

```bash
SUPABASE_DB_HOST=...
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=...
SUPABASE_DB_PASSWORD=...
HIBERNATE_DDL_AUTO=none
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

后端热部署开发模式：

```bash
cd backend
./scripts/dev-hot.sh
```

说明：普通启动适合稳定演示；热部署模式适合持续开发。保存 Java / 配置文件后，脚本会自动编译并触发 Spring Boot DevTools 重启。

前端：

```bash
cd frontend
npm install
npm run dev
```

说明：前端基于 Vite，开发模式下保存页面或样式文件会自动热更新。

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
npm run test:e2e
```

当前验证口径：

- 后端 `78` 个 service / controller 测试通过。
- 前端 `npm run test` 会执行生产构建与 MVP smoke check。
- 前端 `npm run test:e2e` 已有 `2` 个 Chrome 用户流程，覆盖统一学术文档主线与历史兼容回归。
- v2.0.1 Supabase 迁移、章节可信链、整篇质量、选择性共写、审查复查和导出真实冒烟已通过。

## 5. 推荐演示路径

### 路径 A：文本直传演示

适合快速演示完整能力。

1. 打开前端首页。
2. 新建研究项目，选择本科 / 硕士 / 博士 / 科研人员、研究范式和首个文档。
3. 在上传页粘贴学校 / 导师 / 课程 / 期刊要求、研究笔记、已有草稿或调研结果。
4. 提交并进入解析页。
5. 执行预处理与 AI 解析。
6. 查看每份材料的解析质量清单。
7. 若提示缺少写作要求、文献信息或研究证据，点击“填入补充说明”并重新解析。
8. 进入材料检查。
9. 若提示缺少参考资料，在材料不足页使用“去找可引用文献”选择理论基础 / 研究方法 / 案例 / 数据等检索策略。
10. 使用年份、文献类型和来源筛选检索 Crossref / OpenAlex / Semantic Scholar，查看质量评分、匹配理由和缺失元数据。
11. 把合适的候选加入“待下载清单”，打开来源或外部 Google Scholar / 知网入口，用自己的学校账号下载原文。
12. 返回上传页补传下载后的文献，并在“关联待下载候选文献”里选择对应候选。
13. 完成 AI 解析后重新执行材料检查。
14. readiness 通过后进入知识库，再进入“学术文档”。
15. 创建第二个学术文档，观察同一项目共享材料但章节和版本彼此独立。
16. 选择章节，查看章节级 readiness；对不受研究结果缺口影响的章节先行生成或编辑。
17. 选中一段正文生成章节共写预览，确认引用、数据、来源和作者声音约束。
18. 选择整版、部分段落或差异行应用；修改正文后再尝试旧预览可演示 `409` 防覆盖。
19. 打开“本章检查”，查看可信链、原创风险和审查；手动发起 AI 审查并复查单项问题。
20. 切换“材料”与“AI 记录”，查看文档专属材料范围和操作留痕。
21. 切换“整篇检查”，查看覆盖率、原创实证、审查项和交付建议；点击问题返回章节。
22. 组装并导出 docx 或 pdf，说明导出不会创建第二份可编辑全文。

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
- 有解析质量清单：低置信、分类不确定、摘要缺失、证据不足、文献信息不完整、写作与提交要求缺失等。
- 可以把问题建议一键填入补充说明，并重新解析。
- 关键材料为需要补充或解析失败时不能进入下一步。

### 材料检查页

- 明确告诉用户是否可以生成。
- 材料不足时给出缺失项和补充建议。
- 缺少参考资料时提供“去找可引用文献”入口。
- 支持推荐检索策略：理论基础、研究方法、案例材料、数据实证。
- 支持站内检索 Crossref / OpenAlex / Semantic Scholar 公开元数据候选文献。
- 支持年份范围、文献类型、语言倾向和检索来源筛选。
- 支持候选质量评分、推荐引用 / 需人工确认 / 信息不完整标签、匹配理由和缺失元数据提示。
- 支持把候选加入“待下载清单”，下载原文后回上传页关联候选。
- 支持一键打开 Google Scholar / 知网外部检索页，学生自行登录学校账号下载原文。
- 候选文献可以打开来源、复制 DOI / 引用信息，并引导回上传页补传解析。

### 知识库页

- 可构建或重建项目知识库。
- 可查看已入库片段、关键词和来源材料。
- 可用问题检索相关证据片段。

### 学术文档页

- 可查看和修改学术项目画像。
- 可在同一研究项目中创建并切换多个文档。
- 可查看默认章节树、章节完成度和章节版本，并通过拖拽手柄调整章节顺序。
- readiness 区分文档整体与当前章节，不用等待所有研究结果齐全才开始绪论或综述。
- 材料页签控制当前文档允许使用的项目材料。
- “章节写作”是唯一正文编辑模式；AI 生成和共写只作用于当前章节或选区。
- 共写先预览，可按整版、段落或差异行选择性应用；基准版本变化时拒绝覆盖。
- “本章检查”抽屉集中展示可信链、原创补强和审查项，避免主页面堆叠。
- “整篇检查”只读聚合章节质量、引用一致性与交付建议，可跳回对应章节。
- AI 记录页签展示材料依据、模型、操作和采纳状态。

### 历史兼容工作台

- `/workspace` 与旧 API 仍可用于历史项目回归。
- 导航不再展示“整篇共写（兼容）”，新项目不应从这里维护正文。
- `DocumentSection` 始终是学术文档正文的唯一事实来源。

### 学术文档质量抽屉

- 中间章节正文为主，质量能力按需打开。
- 可信链地图展示正文段落、证据片段、原始材料、引用信息、覆盖率评分和引用一致性提示。
- 原创补强页签展示段落级风险分、AI 写作味风险、空泛论证和原创实证不足，并提供“定位段落 / 用已有材料补强”动作。
- 证据卡可打开原始材料预览入口，文本材料展示摘要，文件材料打开下载 / 预览地址。
- AI 共写默认先生成预览，不直接覆盖正文。
- 预览抽屉展示差异摘要、修改理由、冲突提示、逐段接受和可能关联的审查项。
- 审查区支持详情抽屉、申诉和单条手动复查。
- 章节版本区展示历史版本和差异摘要。

### 导出页

- 发起真实导出。
- 获取真实下载链接。
- 查看交付确认。
- 检查参考文献数量、元数据完整性、可信链覆盖率、引用一致性和原创实证 / AI 写作味风险提示。

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

`v1.x 兼容能力完整保留 + v2.0.1 学术文档统一工作台首轮收口，适合演示章节唯一正文源下的证据驱动研究共创流程`

## 8. 常见问题

### 当前章节材料不足无法生成

说明材料类型不够完整。最少建议同时提供：

- 学校 / 导师 / 课程 / 期刊要求类材料。
- 参考文献类材料。
- 当前文档和章节所需的研究结果 / 草稿类材料。

### AI 解析失败

常见原因：

- `OPENAI_API_KEY` 配置错误。
- `OPENAI_BASE_URL` 不是兼容 API 地址。
- 网络或代理波动。

解析质量检查页会把失败材料标记为“解析失败”，并提示用户重试、补充文字说明或重新上传更清晰文件。

### 导出失败

先确认：

- 后端已启动。
- 当前文档至少有一个可组装章节。
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
- [AcademicDocumentsPage.jsx](../../frontend/src/pages/AcademicDocumentsPage.jsx)
- [WorkspacePage.jsx](../../frontend/src/pages/WorkspacePage.jsx)
- [ExportPage.jsx](../../frontend/src/pages/ExportPage.jsx)

后端：

- [MaterialController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/MaterialController.java)
- [MaterialApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/MaterialApplicationService.java)
- [ParseQualityService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ParseQualityService.java)
- [WritingRiskApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/WritingRiskApplicationService.java)
- [WritingRiskController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/WritingRiskController.java)
- [OpenAiSemanticParsingService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiSemanticParsingService.java)
- [OpenAiDraftGenerationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiDraftGenerationService.java)
- [OpenAiCoWriteService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiCoWriteService.java)
- [OpenAiReviewService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/OpenAiReviewService.java)
- [AcademicQualityController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/AcademicQualityController.java)
- [AcademicSectionApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/AcademicSectionApplicationService.java)
- [ScopedEvidenceBindingApplicationService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ScopedEvidenceBindingApplicationService.java)

## 10. 一句话总结

`现在这套系统已经适合演示“上传材料 -> 解析质量检查 -> 章节生成与选择性共写 -> 本章可信审查 -> 整篇质量聚合 -> 按章节真实导出”的统一产品主线。`
