# AI 论文共写工作台开发文档

生成日期：2026-07-11
扫描范围：项目文档、前端源码、后端源码、数据库 SQL、测试与本地运行配置
当前结论：项目已经完成 `MVP 主流程 100% + v1.8 原创实证补强 + v1.9 文献补充增强首轮收口`，现已进入产品深度可用性打磨阶段，具备可演示的端到端产品闭环，但尚未达到生产上线标准。

本文档根据当前仓库实际内容整理，重点说明你此前已经完成的产品、设计、工程与 AI 编排工作。说明以代码实际实现为主，同时参考已有 PRD、接口文档、Demo 指南、交付清单和完成度快照。

## 1. 项目总览

`AI 论文共写工作台` 面向本科课程论文场景，核心定位是 `AI 强辅助共写平台`。它不是简单的聊天生成器，而是一套围绕论文材料、要求、证据、共写、审查、复查与导出的完整工作流。

用户当前可以完成的主链路为：

```text
创建项目
-> 上传写作输入
-> 文件预处理 / 文本抽取 / 图片 OCR
-> AI 语义解析
-> 解析质量检查与补全
-> Requirement Snapshot
-> 材料充足性检查 / 文献补充增强入口
-> 生成初稿
-> 构建项目知识库
-> 进入共写工作台
-> 查看可信链 / 知识库 / 引用建议 / 原创实证风险
-> 生成 AI 共写预览
-> 逐句 / 逐段 / 整版应用
-> 审查、申诉、手动复查
-> 版本管理
-> 导出 DOCX / PDF
```

当前正式状态来自：

- `README.md`
- `docs/project/PRODUCT_COMPLETION_STATUS-6-16.md`
- `docs/project/FINAL_DELIVERY_CHECKLIST.md`
- `docs/guides/DEMO_GUIDE.md`

一句话概括：

`你已经把项目从产品概念、页面原型和工程规格，推进成了一个接入真实 Supabase PostgreSQL 与真实 OpenAI 兼容 AI 调用的论文共写 MVP，并进一步补上可信链、共写预览、审查复查、原创实证补强、文献补充回流和导出风险检查等可信交付能力。`

## 2. 你之前已经完成的事情

### 2.1 产品定义与用户问题梳理

你已经完成了从 0 到 1 的产品定位：

- 明确目标用户为本科课程论文写作者。
- 明确核心痛点：材料分散、证据不足、返工成本高、AI 生成不可信、引用和来源难追溯。
- 明确产品角色：AI 是 `创作者 / 共写者`，不是强制裁判。
- 明确产品主线：`上传材料 -> AI 解析 -> 材料充足性检查 -> 初稿生成 -> 共写审查 -> 导出`。
- 明确材料不足时不兜底生成，而是阻断并提示补充。
- 明确用户最终控制权：审查项可忽略、可申诉、可复查，共写结果默认先预览。

产品规格沉淀在：

- `docs/product/PRD.md`
- `docs/design/frontend_page_spec.md`
- `docs/engineering/backend_service_spec.md`
- `docs/engineering/api_field_spec.md`
- `docs/engineering/openapi_contract_draft.md`
- `docs/engineering/engineering_tasks.md`

### 2.2 原型、页面规格与核心工作台设计

你完成过两个 HTML 原型入口：

- `frontend_wireframe.html`
- `cowriting_workspace_prototype.html`

随后又拆出了 P7 共写工作台设计评审与高保真视觉规范：

- `docs/design/P7_workspace_design_review.md`
- `docs/design/P7_workspace_visual_spec.md`

P7 工作台被定义为核心页面，目标是：

- 中间正文为主角。
- 左侧承载推荐任务与审查项。
- 右侧承载 AI 共写操作。
- 下方提供版本记录和回滚安全感。
- 可信链、知识库、引用建议作为正文辅助，而不是喧宾夺主。

近期你还做过一次工作台视觉降噪：

- 正文编辑区变宽。
- 中间辅助区改为 `可信链 / 知识库 / 引用` 标签。
- AI 面板收束为主输入、3 个常用动作和折叠的更多动作。
- 审查卡次级按钮折叠，降低视觉噪音。

### 2.3 前后端工程落地

当前工程已经落成：

- 前端：React 18 + Vite + React Router + 原生 CSS。
- 后端：Spring Boot 3.3.1 + Java 17 + Spring Web + Spring Data JPA。
- 数据库：Supabase PostgreSQL。
- 文件处理：Apache POI、PDFBox、本地上传文件目录。
- AI 调用：OpenAI 兼容 `/chat/completions` 接口。
- 导出：Apache POI 生成 DOCX，PDFBox 生成 PDF。

主要目录：

```text
frontend/
  src/App.jsx
  src/pages/
  src/components/workspace/
  src/hooks/useWorkspace.js
  src/services/api.js
  src/styles/global.css

backend/
  src/main/java/com/aipm/cowriting/
    interfaces/rest/
    application/service/
    application/dto/
    domain/entity/
    domain/repository/
    domain/model/
    common/
    config/
  db/
  scripts/
```

### 2.4 版本演进记录

当前 README 和完成度文档中明确记录了以下版本演进：

| 阶段 | 已完成能力 |
| --- | --- |
| v1.1 | 文件解析补全，支持 PDF / DOCX / TXT / Markdown / 图片 / XLSX / CSV / PPTX / ZIP 等输入，并支持解析失败后的补充说明 |
| v1.2 | 引用与格式增强，支持作者、年份、题名、期刊/出版社、链接等文献元数据，并支持 APA / GB/T 7714 格式切换 |
| v1.3 | 项目知识库 MVP，把已 AI 解析材料转成项目级证据片段，支持关键词检索和片段预览 |
| v1.4 | 可信链与共写闭环，支持段落级证据绑定、异步可信链重建、共写预览后应用、审查项手动复查 |
| v1.5 | AI 解析质量清单，材料响应新增 `parseQuality`，前端展示质量徽标、问题清单、一键填入补充说明和关键材料阻断 |
| v1.6 | 可信交付增强，支持原始材料预览入口、可信链覆盖率、引用一致性、共写逐段接受、冲突提示、共写与审查项关联落库、导出前风险检查 |
| v1.8 | 原创实证补强，支持段落级空泛论证、原创实证不足和 AI 写作味风险派生，并把风险接入工作台、审查、共写与导出 |
| v1.9 | 文献补充增强，支持 Crossref / OpenAlex / Semantic Scholar 多源元数据检索、质量评分、去重、待下载清单和上传关联回流 |

近期 Git 历史也体现了这些工作：

- `improve the cowrite interface`：共写工作台视觉降噪与 AI 面板优化。
- `update_operator`：上传页易用性、知识库可读性、原始材料 UTF-8 预览修复、热部署开发体验。
- `v1.6 Trusted Delivery Enhancement Plan`：材料预览、可信链评分、引用一致性、共写审查关联、导出风险检查。
- `New Literature Retrieval Interface`：材料不足页新增 Crossref 检索和 Google Scholar / 知网外部入口。
- `supply the material evidence`：补齐 v1.8 原创实证补强和 v1.9 多源文献候选、质量评分、待下载与上传关联。
- `Document_Consolidation`：README、Demo 指南、接口字段、OpenAPI、交付清单等文档口径收口。
- `update`：早期完整工程骨架、后端服务、前端页面、测试、样例和文档落地。

## 3. 当前技术架构

### 3.1 前端架构

前端是一个真实多页面 Demo 应用，不是静态展示页。

入口：

- `frontend/src/main.jsx`
- `frontend/src/App.jsx`

路由：

| 路由 | 页面 | 职责 |
| --- | --- | --- |
| `/projects` | `ProjectListPage.jsx` | 创建 / 选择论文项目 |
| `/upload` | `UploadPage.jsx` | 上传文件、粘贴文本、填写链接 |
| `/parsing` | `ParsingStatusPage.jsx` | 预处理、AI 解析、解析质量检查与补全 |
| `/gate` | `MaterialGatePage.jsx` | Requirement Snapshot 与材料充足性检查，合格后生成初稿 |
| `/knowledge-base` | `KnowledgeBasePage.jsx` | 构建与检索项目知识库 |
| `/workspace` | `WorkspacePage.jsx` | 正文编辑、可信链、AI 共写、审查、申诉、版本管理 |
| `/export` | `ExportPage.jsx` | 导出、参考文献草案、交付风险检查 |

前端状态特点：

- 当前 workspace 和 draft 持久化到 `localStorage`。
- 关键路由有状态保护，没有 workspace 会回到 `/projects`，没有 draft 会回到 `/gate`。
- API 统一封装在 `frontend/src/services/api.js`。
- 工作台复杂状态集中在 `frontend/src/hooks/useWorkspace.js`。
- `npm run test` 会先构建，再执行 `scripts/mvp-smoke.mjs` 检查主路由、核心 API、导出页、共写预览、可信链、解析质量等关键标记。

### 3.2 后端架构

后端采用分层结构：

```text
interfaces/rest      REST Controller
application/service  应用服务与 AI 编排
application/dto      请求/响应 DTO
domain/entity        JPA 实体
domain/repository    Spring Data JPA 仓库
domain/model         枚举状态
common               API 响应、错误处理、Web 公共逻辑
config               OpenAI 配置
```

Controller 覆盖：

| Controller | 主要职责 |
| --- | --- |
| `WorkspaceController` | 项目创建、列表、详情 |
| `MaterialController` | 上传、列表、预处理、AI 解析、补充说明、材料预览、原始文件、分类纠正、文献元数据编辑 |
| `RequirementController` | Requirement Snapshot 创建与读取 |
| `SufficiencyController` | 材料充足性检查 |
| `DraftController` | 初稿生成、草稿详情、保存、恢复、版本列表 |
| `CoWriteController` | 兼容旧共写、共写预览、应用预览、放弃预览 |
| `EvidenceBindingController` | 可信链获取、异步重建、绑定状态更新 |
| `ReviewController` | 审查项列表、状态更新、申诉、手动复查、申诉查询 |
| `KnowledgeBaseController` | 知识库构建、片段列表、关键词检索 |
| `LiteratureSearchController` | 多源文献元数据检索、候选保存与待下载清单查询 |
| `WritingRiskController` | 获取草稿的段落级原创实证与 AI 写作味风险 |
| `ExportController` | DOCX / PDF 导出与下载 |
| `JobController` | 内存 job 状态查询 |

统一 API 前缀为 `/api/v1`。

### 3.3 数据库与持久化

当前代码实际使用的核心实体：

| 实体 | 表 | 职责 |
| --- | --- | --- |
| `WorkspaceEntity` | `workspaces` | 项目、用户 ID、标题、状态、当前 draft |
| `MaterialEntity` | `materials` | 上传材料、文本内容、补充说明、外链、关键材料标记、解析状态 |
| `AiSemanticParseResultEntity` | `ai_semantic_parse_results` | AI 解析结果、材料分类、摘要、论点、证据、老师要求、文献元数据、置信度 |
| `RequirementSnapshotEntity` | `requirement_snapshots` | 论文题目、字数、截止日期、引用格式、特殊要求、版本 |
| `MaterialSufficiencyResultEntity` | `material_sufficiency_results` | 材料是否足够、缺失项、补充建议、原因 |
| `DraftVersionEntity` | `draft_versions` | 版本号、题目建议、大纲、段落骨架、正文、来源追溯 |
| `KnowledgeChunkEntity` | `knowledge_chunks` | 项目知识库片段、关键词、来源材料 |
| `EvidenceBindingEntity` | `evidence_bindings` | 段落级证据绑定、材料、知识片段、置信度、状态、引用文本 |
| `CoWritePreviewEntity` | `cowrite_previews` | AI 共写候选正文、控制参数、diff 摘要、状态 |
| `CoWritePreviewReviewLinkEntity` | `cowrite_preview_review_links` | 共写预览与可能影响审查项的关联 |
| `ReviewItemEntity` | `review_items` | 审查项、影响等级、定位范围、建议、状态、复查字段 |
| `ReviewRecheckLogEntity` | `review_recheck_logs` | 单条审查项复查历史与依据快照 |
| `AppealCaseEntity` | `appeal_cases` | 审查申诉理由、证据和复审结果 |
| `LiteratureCandidateEntity` | `literature_candidates` | 候选文献元数据、质量评分、待下载状态和已关联材料 |

基础 SQL 在：

- `postgresql_schema.sql`
- `backend/src/main/resources/static/postgresql_schema.sql`

迁移补丁：

- `backend/db/v1_4_trust_chain.sql`
- `backend/db/v1_6_trust_delivery_enhancement.sql`
- `backend/db/v1_9_literature_candidates.sql`

需要注意的历史边界：

- SQL 草案中保留了 `preprocessed_contents`、`generation_jobs`、`user_actions` 等设计对象。
- 当前代码实际把预处理文本放在 `materials.plainTextContent` / `supplementText`，job 使用 `JobApplicationService` 的内存 `ConcurrentHashMap`，还不是数据库持久化队列。
- v1.9 迁移 SQL 已交付，JPA 可自动补建表；当前文档没有记录该 SQL 已被手动执行为正式 Supabase 迁移。
- 新建 Supabase 表是否暴露到 Data API 取决于项目设置；当前前端不直连该表，业务访问仍由 Spring Boot / JPA 承担。

### 3.4 状态模型

后端枚举：

| 枚举 | 值 |
| --- | --- |
| `WorkspaceStatus` | `DRAFT`, `PROCESSING`, `READY`, `BLOCKED`, `ARCHIVED` |
| `ParseStage` | `PREPROCESSED`, `AI_PARSED`, `AI_PARTIAL`, `AI_FAILED` |
| `MaterialCategory` | `ASSIGNMENT_REQUIREMENT`, `REFERENCE_MATERIAL`, `USER_DRAFT`, `RESEARCH_RESULT`, `CHART_OR_DATA`, `SUPPLEMENT_NOTE`, `UNKNOWN` |
| `GenerationStatus` | `SUCCESS`, `PARTIAL`, `FAILED`, `BLOCKED_INSUFFICIENT_MATERIAL` |
| `ReviewImpactLevel` | `NOTICE`, `LOCAL_FIX`, `MUST_CONFIRM` |
| `JobStatus` | `QUEUED`, `RUNNING`, `SUCCESS`, `PARTIAL`, `FAILED` |

审查项状态用字符串：

- `OPEN`
- `RESOLVED`
- `IGNORED`

证据绑定状态用字符串：

- `CONFIRMED`
- `WEAK`
- `MISSING`
- `USER_CONFIRMED`

共写预览状态用字符串：

- `READY`
- `APPLIED`
- `DISCARDED`

## 4. 后端核心逻辑

### 4.1 Workspace 项目服务

入口：

- `WorkspaceController`
- `WorkspaceApplicationService`

已实现：

- 创建 workspace。
- 列出 workspace。
- 获取 workspace 详情。
- workspace 记录 `userId`、`title`、`status`、`currentDraftVersionId`、`createdAt`、`updatedAt`。
- 当前 Demo 中用户体系没有正式接入，`userId` 更像占位字段。

### 4.2 材料上传与本地存储

入口：

- `MaterialController`
- `MaterialApplicationService`
- `LocalMaterialStorageService`

已实现：

- 支持 multipart 文件、粘贴文本、外部链接三类输入。
- `files / plainText / externalLink` 至少传一个。
- 文件保存到后端工作目录下的 `uploaded-materials/{workspaceId}/`。
- 文件名会做基础 sanitize，避免特殊字符污染路径。
- 上传后创建 `MaterialEntity`，初始状态为 `PREPROCESSED`。
- 上传材料会把 workspace 状态改为 `PROCESSING`。
- 支持关键材料标记 `isKeyMaterial`。

前端上传页已经增强：

- 三步式引导。
- 文件、文本、链接分卡片。
- 支持拖拽上传。
- 支持多文件队列。
- 自动把 PDF / DOCX / 图片 / XLSX / CSV / PPTX / ZIP 等文件识别为重要依据。
- 提交后进入解析质量检查页。

### 4.3 文件预处理与文本抽取

入口：

- `MaterialExtractionService`
- `OpenAiImageOcrService`

已实现文件类型：

| 类型 | 处理方式 |
| --- | --- |
| `txt`, `md`, `csv` | UTF-8 直接读取 |
| `pdf` | PDFBox 提取文本 |
| `docx` | Apache POI Word Extractor |
| `xlsx` | Apache POI 读取 sheet / row / cell 文本 |
| `pptx` | Apache POI 读取幻灯片文字和 shape 名称 |
| `zip` | 读取 ZIP 中的 txt / md / csv，其他格式在 ZIP 内暂不深度解析 |
| 图片 | 调用 OpenAI 兼容模型做 OCR |
| `ppt` | 明确返回不支持老版 `.ppt` |

图片 OCR：

- 把图片读取为 Base64 data URL。
- 调用 `/chat/completions`，消息 content 包含 `image_url`。
- 返回纯文本。

### 4.4 AI 语义解析

入口：

- `OpenAiSemanticParsingService`
- `MaterialApplicationService.triggerAiParse`

已实现：

- 调用 OpenAI 兼容 `/chat/completions`。
- 强制 `response_format: { type: "json_object" }`。
- 要求输出：
  - `materialCategory`
  - `summary`
  - `topicRelation`
  - `detectedClaims`
  - `detectedEvidence`
  - `detectedRequirements`
  - `bibliographicMetadata`
  - `confidenceScore`
- 明确要求 AI 不得编造作者、年份、题名、来源、出版社、URL、DOI。
- 支持从不同响应结构中提取输出文本：`choices[0].message.content`、`output_text`、`output[].content[].text` 等。
- 解析结果写入 `ai_semantic_parse_results`。
- 材料状态改为 `AI_PARSED`。

材料分类还有本地启发式补强：

- 包含“课程论文要求”“作业要求”“字数 + 格式”“引用 + 参考资料”时，优先判为 `ASSIGNMENT_REQUIREMENT`。
- 包含“参考文献”“文献摘录”“已有研究”“研究指出”时，优先判为 `REFERENCE_MATERIAL`。
- 包含“研究结果”“调研”“数据显示”“%”“样本”时，优先判为 `RESEARCH_RESULT`。

### 4.5 AI 解析质量清单

入口：

- `ParseQualityService`
- `MaterialResponse.parseQuality`
- `ParsingStatusPage.jsx`

这是 v1.5 的核心增强。

后端会实时派生 `ParseQualityReport`：

- `status`
  - `READY`
  - `NEEDS_CONFIRMATION`
  - `NEEDS_SUPPLEMENT`
  - `FAILED`
- `score`
- `issues`
- `completeness`
- `nextAction`

检查项包括：

- AI 解析失败。
- 缺少可解析内容。
- 未完成 AI 解析。
- 置信度低于 0.55。
- 材料角色不确定。
- 摘要或主题关系缺失。
- 参考文献信息缺作者 / 年份 / 题名。
- 老师要求材料未解析出明确要求。
- 研究结果 / 图表数据缺少研究结论。
- 研究结果 / 图表数据缺少证据说明。

前端已经把它做成可操作体验：

- 解析质量徽标。
- 质量分。
- 问题清单。
- 一键把 `supplementPrompt` 填入补充说明。
- 可填写页码。
- 补充后重新预处理和 AI 解析。
- 可人工纠正材料角色。
- 关键材料若 `NEEDS_SUPPLEMENT` 或 `FAILED`，不能进入材料检查。

### 4.6 Requirement Snapshot

入口：

- `RequirementController`
- `RequirementApplicationService`

已实现：

- 创建要求快照。
- 获取最新要求快照。
- 字段包括：
  - `topic`
  - `wordCount`
  - `deadline`
  - `citationStyle`
  - `specialRequirements`
  - `version`

当前前端 `MaterialGatePage` 如果读取不到快照，会自动创建一个默认快照：

```text
topic = workspace.title
wordCount = 3000
citationStyle = APA
specialRequirements.minReferences = 5
```

### 4.7 材料充足性检查

入口：

- `SufficiencyController`
- `SufficiencyApplicationService`
- `MaterialGatePage.jsx`

已实现硬规则：

- 至少有一份关键材料完成 `AI_PARSED`。
- 已解析材料中要有老师要求 / 作业说明。
- 已解析材料中要有参考资料。
- 已解析材料中要有研究结果、用户草稿或图表数据。

缺失时返回：

- `missingItems`
- `recommendedSupplements`
- `reason`
- `isGenerationEligible = false`

前端行为：

- 材料不足时展示缺失原因、下一步动作、建议补充数量。
- 缺少参考资料时可调用 `LiteratureSearchController` 检索 Crossref / OpenAlex / Semantic Scholar 公开元数据。
- 候选结果会显示质量评分、匹配理由和缺失元数据，并可加入待下载清单。
- 用户下载原文后可在上传页关联候选；候选本身不会直接进入生成、知识库或可信链。
- 不会兜底生成。
- 只有 `isGenerationEligible = true` 时才触发初稿生成。

### 4.8 初稿生成与版本管理

入口：

- `DraftController`
- `DraftApplicationService`
- `OpenAiDraftGenerationService`

生成前置条件：

- workspace 存在。
- requirement snapshot 存在。
- 最近一次材料充足性检查存在且 `isGenerationEligible = true`。

AI 初稿生成：

- 使用 requirement context。
- 使用所有已解析材料的结构化 context。
- 支持模式：
  - `stable`
  - `quick`
  - `academic`
- 要求 AI 仅基于提供材料写作。
- 要求中文输出。
- 要求生成：
  - `titleSuggestion`
  - `outline`
  - `paragraphSkeletons`
  - `draftText`
  - `sourceTraceMap`

生成后：

- 创建新的 `DraftVersionEntity`。
- `generationStatus = SUCCESS`。
- `createdBy = system-ai`。
- 自动调用 `reviewApplicationService.refreshForDraft(draft)`。
- 自动调用 `evidenceBindingApplicationService.rebuild(draftId)`。
- 更新 workspace 当前草稿版本。

版本能力：

- 查询草稿详情。
- 保存手动编辑。
- 恢复历史版本。
- 获取 workspace 下草稿版本列表。

前端底部 `WorkspaceVersionPanel` 提供版本安全网。

### 4.9 项目知识库

入口：

- `KnowledgeBaseController`
- `KnowledgeBaseApplicationService`
- `KnowledgeBasePage.jsx`
- 工作台内知识库标签

这是 v1.3 MVP，并在后续做过可读性优化。

当前知识库不是 embedding / 向量检索，而是关键词与词法检索。

构建规则：

- 只纳入 `AI_PARSED` 的材料。
- 必须有可用文本或 AI 解析信号。
- 每个 workspace 最多 80 个 chunk。
- 原文 chunk 大小约 900 字，重叠 120 字。
- 优先构建 AI 信号片段：
  - 材料文件
  - 材料角色
  - 解析摘要
  - 主题关系
  - 观点
  - 证据
  - 老师要求
  - 文献信息
- 再补充原文 / 补充说明分块。

可读性清洗：

- Markdown 图片、链接、标题、引用、列表、代码标记清洗。
- Markdown 表格分隔符清洗。
- OCR 噪声、异常符号串过滤。
- 中文字符间多余空格处理。
- 过短或意义字符比例过低的 chunk 不入库。

检索逻辑：

- 查询为空时返回 400。
- limit 限制在 1 到 20。
- 通过全文包含、关键词匹配、chunk keyword 匹配计算 score。
- score 上限 1.0。

前端能力：

- 构建 / 重建知识库。
- 查看入库片段、关联材料、关键词。
- 输入问题检索。
- 工作台内可用选中文本或标题主题检索证据。
- 可把检索片段追加为证据说明，或插入引用。

### 4.10 材料可信链

入口：

- `EvidenceBindingController`
- `EvidenceBindingApplicationService`
- `EvidenceBindingRebuildJobService`
- `WorkspaceEditorPanel.jsx`

这是 v1.4 和 v1.6 的重点能力。

可信链重建逻辑：

- 根据 `draft.draftText` 切分段落，生成 `p1`, `p2`, ...
- 从 `sourceTraceMapJson` 中收集每段材料 ID。
- 根据材料 ID 查找材料、AI 解析结果、知识库 chunk。
- 对每段文本和 chunk 做词法匹配，选择 best chunk。
- 若没有来源，创建 `MISSING` binding。
- 若有材料且有强解析证据或知识库 chunk，标记 `CONFIRMED`。
- 若只有 source trace，标记 `WEAK`。
- 生成：
  - `claimText`
  - `sourceExcerpt`
  - `targetRange`
  - `confidenceScore`
  - `supportType`
  - `bindingStatus`
  - `citationText`

v1.6 返回增强：

- `EvidenceCoverageReport`
  - 总段落数
  - 已确认段落
  - 弱绑定段落
  - 缺来源段落
  - 覆盖率
  - 确认率
  - 健康标签
  - 建议
- `CitationConsistencyReport`
  - 引用标记数量
  - 已绑定材料数量
  - 缺引用段落数量
  - 孤儿引用数量
  - 文献信息不完整数量
  - 风险状态
  - 问题清单
- `sourceLocation`
  - 缺来源
  - 页码线索
  - 知识库片段线索
  - 正文范围线索
  - 材料预览 URL

前端展示：

- `材料可信链` 标签。
- 可信链覆盖率、确认率、缺来源段落、未使用材料。
- 引用一致性状态。
- 段落 -> 证据 -> 材料 -> 文献 -> 引用 五步链路。
- 原始材料位置线索。
- 证据强弱解释。
- 打开原始材料。
- 定位正文范围。
- 插入引用。
- 将弱绑定标记为用户确认。
- 将绑定标记为需补充。

可信链异步重建：

- `POST /drafts/{id}/evidence-bindings/rebuild` 返回 job。
- 后端用 `TaskExecutor` 后台执行。
- 前端每 1200ms 轮询 job，最多 90 次。
- 成功后刷新可信链。

### 4.11 原始材料预览

入口：

- `GET /materials/{id}/preview`
- `GET /materials/{id}/file`

已实现：

- `preview` 返回：
  - `previewType`
  - `previewText`
  - `downloadUrl`
  - `externalLink`
- 有本地文件则给 `/api/v1/materials/{id}/file`。
- 无本地文件但有外链则打开外链。
- 纯文本材料则展示文本摘要。

近期修复：

- Markdown / TXT / CSV / JSON / HTML 明确返回 UTF-8 charset。
- `Content-Disposition` 使用 UTF-8 inline filename，支持中文文件名预览。

### 4.12 AI 共写

入口：

- `CoWriteController`
- `DraftApplicationService.coWrite`
- `CoWritePreviewApplicationService`
- `OpenAiCoWriteService`
- `WorkspaceAiPanel.jsx`
- `WorkspaceCoWritePreviewDrawer.jsx`

共写包含两种路径：

1. 旧兼容路径：`POST /workspaces/{id}/co-write`
   - 直接调用 AI 共写。
   - 直接创建新 draft version。
   - 保留兼容。

2. 默认推荐路径：`POST /workspaces/{id}/co-write/preview`
   - 只生成候选正文，不创建新版本。
   - 用户确认后 `POST /co-write-previews/{id}/apply`。
   - 用户可 `POST /co-write-previews/{id}/discard`。

共写支持：

- 全文改写。
- 选区改写。
- 审查项定位改写。
- 操作类型：
  - rewrite selection
  - add evidence
  - adjust structure
  - reduce repetition
  - improve expression
  - expand argument
  - shorten text
- 修改力度：
  - light
  - balanced
  - deep
- 保留约束：
  - `keepCitations`
  - `keepData`
  - `noNewSources`
  - `keepStudentVoice`

AI prompt 中已经明确：

- 保留引用。
- 不修改数字、姓名、地点、日期、研究发现。
- 不新增来源。
- 保留学生表达。
- 减少 AI 味表达。
- 不编造引用或数据。

共写预览后端生成：

- 候选标题。
- 候选正文。
- 候选 source trace。
- diff summary。
- paragraph diffs。
- guardrails。
- conflict warnings。
- recheck suggestion。
- 可能关联的审查项落库到 `cowrite_preview_review_links`。

冲突提示包括：

- 引用可能丢失。
- 可能新增来源。
- 数字可能变化。
- 无明显冲突。

前端预览抽屉支持：

- 当前正文 vs AI 修改后高亮对照。
- 修改动作、长度变化、处理范围。
- 保留约束检查。
- 冲突提示。
- 可能关联的审查项。
- 差异摘要。
- 逐句差异预览。
- 逐段接受。
- 局部接受。
- 放弃预览。
- 应用选中差异到编辑区。
- 应用选中段落到编辑区。
- 整版应用为新版本。

应用整版预览后：

- 后端创建新 draft version。
- 刷新审查项。
- 重建可信链。
- 更新 workspace 当前版本。
- preview 状态改为 `APPLIED`。

局部接受 / 逐段接受的当前实现：

- 前端先把选中的差异应用到编辑区。
- 用户保存正文后重建可信链。
- 这不是后端直接创建新版本的路径。

### 4.13 审查、申诉与复查

入口：

- `ReviewController`
- `ReviewApplicationService`
- `OpenAiReviewService`
- `WorkspaceReviewSidebar.jsx`
- `WorkspaceReviewDrawer.jsx`
- `WorkspaceAppealModal.jsx`

审查生成逻辑：

- 初稿生成或共写应用后，后端会刷新审查项。
- 调用 AI 生成最多 5 条审查项。
- 支持影响等级：
  - `NOTICE`
  - `LOCAL_FIX`
  - `MUST_CONFIRM`
- 支持审查类型：
  - missing evidence
  - requirement conflict
  - repetition issue
  - logic gap
  - factual risk
  - citation missing
  - citation format mismatch
  - reference orphan
  - reference not cited
  - reference metadata incomplete
  - aigc style risk
  - generic unsupported claim
  - original evidence missing

v1.8 原创实证补强：

- `WritingRiskApplicationService` 按段落派生空泛论证、模板表达、证据缺失和原创实证不足风险。
- `GET /api/v1/drafts/{id}/writing-risks` 返回风险分、触发信号、补充提示和共写指令。
- 工作台可定位风险段落，并基于已上传材料生成“补原创实证”共写预览。
- 材料不足时只返回需要补充的案例、数据、访谈、问卷、实验或文献清单，不编造实证。
- 该能力用于提升内容质量，不承诺规避 AI 检测或论文查重。

本地引用规则叠加：

- APA / GB/T 7714 引用格式错配。
- 有 source trace 但正文无引用标记。
- 正文有引用但没有材料来源追溯。
- GB/T 编号引用数量超过可追溯材料数量。
- 参考文献区有条目但正文无引用。
- 已引用材料缺作者 / 年份 / 题名。

审查项操作：

- 打开详情抽屉。
- 定位正文范围。
- 按建议触发 AI 共写。
- 标记已解决。
- 忽略。
- 重新打开。
- 发起申诉。
- 手动复查单条审查项。

申诉逻辑：

- 用户提交理由和证据。
- AI 复审可能：
  - maintained
  - downgraded_to_notice
  - downgraded_to_local_fix
  - withdrawn
- 若撤销，审查项变为 `RESOLVED`。
- 若降级，更新影响等级。
- 申诉记录写入 `appeal_cases`。

手动复查逻辑：

- 只复查单条审查项，不全篇重审。
- AI 返回：
  - `RESOLVED`
  - `STILL_OPEN`
  - `DOWNGRADED`
  - `NEEDS_MORE_EVIDENCE`
- 更新审查状态、影响等级、`lastRecheckedAt`、`recheckNote`。
- 写入 `review_recheck_logs`，保存：
  - 结果
  - 前后状态
  - 前后影响等级
  - 说明
  - 依据快照

前端审查抽屉展示：

- 最近复查。
- 复查历史。
- 判断依据。
- 复查依据清单。
- 问题说明。
- 建议修正。
- 影响范围。
- 处理建议。
- 可申诉边界。

### 4.14 导出

入口：

- `ExportController`
- `ExportApplicationService`
- `ExportPage.jsx`

已实现：

- 支持 `docx` 与 `pdf`。
- 导出 job 创建成功后写入 `outputRef`。
- 下载地址为 `/api/v1/exports/{jobId}/download`。
- DOCX 使用 Apache POI。
- PDF 使用 PDFBox。
- 导出文件保存在 `backend/generated-exports/`。
- 根据 draft 的 `sourceTraceMapJson` 收集已使用材料。
- 根据 AI 解析出来的文献元数据生成参考文献草案。
- 支持 APA 与 GB/T 7714 两类格式化。

前端导出页已完成 v1.6 交付增强：

- 选择导出格式。
- 选择参考文献格式。
- 是否包含批注。
- 发起导出。
- 查询 job 输出。
- 下载导出文件。
- 参考文献草案预览。
- 编辑文献元数据。
- 交付确认面板。

交付确认检查：

- 是否有参考文献。
- 文献信息是否完整。
- 可信链覆盖率是否足够。
- 引用一致性是否 ready。
- 是否存在明显模板化 AI 表达。
- 是否存在过长段落。
- 导出格式是否可用。

当前 `includeComments` 已在前端请求里传递，但后端导出逻辑没有真正生成批注版本，这是后续增强点。

### 4.15 Job 与异步任务

入口：

- `JobApplicationService`
- `JobController`
- `EvidenceBindingRebuildJobService`

当前 job 特点：

- 内存存储：`ConcurrentHashMap`。
- 字段包括：
  - `id`
  - `workspaceId`
  - `jobType`
  - `status`
  - `progress`
  - `inputRef`
  - `outputRef`
  - `errorMessage`
  - `createdAt`
  - `updatedAt`
- 支持：
  - 创建 job
  - 查询 job
  - 附加输出
  - 更新进度
  - 标记 running / success / failed

当前真正后台异步的是可信链重建；其他很多 job 是同步完成后返回 `success` 状态。

边界：

- 不是生产级队列。
- 服务重启后 job 会丢。
- 没有重试、死信、持久化进度。

### 4.16 错误处理

入口：

- `BusinessException`
- `ErrorCode`
- `GlobalExceptionHandler`
- `ApiResponse`
- `ApiError`

已实现：

- 统一响应结构：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "meta": {
    "requestId": "..."
  }
}
```

- 业务异常返回对应 HTTP status 和 error code。
- 参数校验异常返回 `INVALID_REQUEST_BODY`。
- query 参数异常返回 `INVALID_QUERY_PARAM`。
- 未捕获异常返回 `INTERNAL_ERROR`。
- requestId 优先读取 `X-Request-Id`，否则生成 UUID。

前端 API 封装会把 502 / 503 / 504 转成更友好的 AI 服务提示。

## 5. 前端核心逻辑

### 5.1 API 封装

文件：`frontend/src/services/api.js`

已封装：

- workspace
- material upload/list/preview/preprocess/ai-parse/supplement/category/metadata
- requirement snapshot
- material sufficiency
- draft generation/detail/list/update/restore
- knowledge base build/list/search
- review list/status/appeal/recheck
- evidence binding get/rebuild/update status
- co-write direct/preview/apply/discard
- export
- job

特点：

- 自动给 JSON 请求设置 `Content-Type`。
- FormData 请求不设置 JSON 头。
- 统一解析 `payload.data`。
- 根据后端 `success=false` 抛出错误。

### 5.2 App 与路由保护

文件：`frontend/src/App.jsx`

已实现：

- 左侧步骤导航。
- 页面标题根据路由变化。
- workspace/draft localStorage 持久化。
- 路由保护：
  - 没有 workspace 不能进上传后的页面。
  - 没有 draft 不能进工作台和导出。
- 全局错误 banner。

### 5.3 上传页

文件：`frontend/src/pages/UploadPage.jsx`

已实现：

- 文件、文本、链接三种输入。
- 拖拽上传。
- 多文件队列。
- 每个文件可标记关键材料。
- 文本和链接也可标记关键材料。
- 提交时按文本、链接、文件依次上传。
- 上传成功后进入解析页。

### 5.4 解析质量检查页

文件：`frontend/src/pages/ParsingStatusPage.jsx`

已实现：

- 每 4 秒轮询材料列表。
- 显示总材料数、已解析数、关键材料可用度、需补充 / 失败数。
- 执行预处理 + AI 语义解析。
- 单项重试解析。
- 显示 AI 摘要、主题关系、文献信息。
- 可纠正材料角色。
- 展示 parse quality 状态、质量分、问题清单。
- 一键填入补充说明。
- 可填写页码。
- 补充说明后重新解析。
- 关键材料未达到 `READY` 或 `NEEDS_CONFIRMATION` 时阻断继续。

### 5.5 材料检查页

文件：`frontend/src/pages/MaterialGatePage.jsx`

已实现：

- 自动准备 Requirement Snapshot。
- 支持选择初稿生成模式。
- 调用材料充足性检查。
- 合格后调用初稿生成，拉取最新 draft，进入知识库页。
- 不合格时展示缺失项和补充建议。

### 5.6 知识库页

文件：`frontend/src/pages/KnowledgeBasePage.jsx`

已实现：

- 展示入库片段、关联材料、预览片段。
- 构建 / 重建项目知识库。
- 显示构建 summary。
- 输入问题或关键词检索。
- 展示匹配度、来源材料、关键词。

### 5.7 工作台状态 Hook

文件：`frontend/src/hooks/useWorkspace.js`

这是前端最核心的状态编排文件。

它负责：

- draft 正文状态。
- 审查项加载。
- 版本列表加载。
- 材料列表加载。
- Requirement Snapshot 引用格式加载。
- 可信链加载。
- 可信链重建 job 轮询。
- AI 共写预览。
- 应用 / 放弃共写预览。
- 应用选中句级 diff。
- 应用选中段落 diff。
- 保存正文。
- 插入引用。
- 定位证据。
- 打开原始材料。
- 工作台内知识库检索。
- 恢复历史版本。
- 提交申诉。
- 手动复查审查项。
- 审查项状态更新。

关键产品逻辑：

- 正文太长且没有选区时，提示先选中段落再共写。
- 如果用户没有手动选区，会优先定位到第一条有 target range 的审查项。
- 保存正文后会触发可信链后台重建。
- 应用整版共写预览后会加载新版本、审查项和可信链。
- 局部 / 逐段接受只是先应用到编辑区，用户仍需保存。

### 5.8 工作台 UI 组件

核心组件：

- `WorkspaceHeader.jsx`
- `WorkspaceReviewSidebar.jsx`
- `WorkspaceEditorPanel.jsx`
- `WorkspaceAiPanel.jsx`
- `WorkspaceCoWritePreviewDrawer.jsx`
- `WorkspaceReviewDrawer.jsx`
- `WorkspaceAppealModal.jsx`
- `WorkspaceVersionPanel.jsx`
- `ReviewItem.jsx`
- `workspaceUtils.js`
- `constants.js`

已实现区域：

- 顶部项目状态、版本、导出入口。
- 左侧审查 / 推荐任务。
- 中间正文编辑。
- 中间辅助标签：
  - 可信链
  - 知识库
  - 引用
- 右侧 AI 共写控制。
- 共写预览抽屉。
- 审查详情抽屉。
- 申诉弹层。
- 底部版本管理。

`workspaceUtils.js` 包含大量前端纯逻辑：

- 选区描述与 range clamp。
- co-write target range 构建。
- source trace 解析。
- APA / GB/T 引用文本与参考文献文本生成。
- 材料分类展示。
- citation suggestions。
- 可信链摘要。
- 审查项指令构建。
- 审查依据和处理建议。
- 版本 diff 摘要。
- 句级 / 段落级 diff。
- 局部接受 diff。
- 高亮 diff block。
- 共写修改解释。
- 预览关联审查项前端推断。
- source location 格式化。
- guardrail check。
- evidence strength。

### 5.9 导出页

文件：`frontend/src/pages/ExportPage.jsx`

已实现：

- 读取材料列表。
- 读取 requirement citation style。
- 读取可信链 summary。
- 收集参考文献材料。
- 构建导出 readiness。
- 文献元数据编辑。
- 发起导出并读取 job output。
- 下载文件。
- 展示交付确认。

导出 readiness 包括：

- 参考文献数量。
- 文献信息完整度。
- 可信链覆盖率。
- 引用一致性。
- AI 写作味风险。
- 学术表达结构风险。
- 导出格式。

## 6. API 端点清单

下表以当前后端 Controller 实现为准。`openapi_contract_draft.md` 与 `api_field_spec.md` 仍保留 v1.6 历史规格，尚未完整覆盖 v1.8 / v1.9 新接口。

| 方法 | 路径 | 用途 |
| --- | --- | --- |
| `POST` | `/api/v1/workspaces` | 创建项目 |
| `GET` | `/api/v1/workspaces` | 项目列表 |
| `GET` | `/api/v1/workspaces/{id}` | 项目详情 |
| `POST` | `/api/v1/workspaces/{id}/materials` | 上传或创建材料 |
| `GET` | `/api/v1/workspaces/{id}/materials` | 材料列表，含 parseQuality |
| `GET` | `/api/v1/materials/{id}/preview` | 原始材料预览元信息 |
| `GET` | `/api/v1/materials/{id}/file` | 原始材料文件流 |
| `POST` | `/api/v1/materials/{id}/preprocess` | 预处理 |
| `POST` | `/api/v1/materials/{id}/ai-parse` | AI 语义解析 |
| `POST` | `/api/v1/materials/{id}/supplement` | 补充说明 |
| `PATCH` | `/api/v1/materials/{id}/category` | 材料角色纠正 |
| `PATCH` | `/api/v1/materials/{id}/bibliographic-metadata` | 文献元数据编辑 |
| `POST` | `/api/v1/workspaces/{id}/requirement-snapshot` | 创建要求快照 |
| `GET` | `/api/v1/workspaces/{id}/requirement-snapshot` | 获取最新要求快照 |
| `POST` | `/api/v1/workspaces/{id}/material-sufficiency-check` | 材料充足性检查 |
| `POST` | `/api/v1/workspaces/{id}/literature-search` | 多源文献元数据检索 |
| `POST` | `/api/v1/workspaces/{id}/literature-candidates` | 保存候选到待下载清单 |
| `GET` | `/api/v1/workspaces/{id}/literature-candidates` | 获取候选文献清单 |
| `POST` | `/api/v1/workspaces/{id}/generate-draft` | 初稿生成 |
| `GET` | `/api/v1/workspaces/{id}/drafts` | 草稿版本列表 |
| `GET` | `/api/v1/drafts/{id}` | 草稿详情 |
| `PATCH` | `/api/v1/drafts/{id}` | 保存正文 |
| `POST` | `/api/v1/drafts/{id}/restore` | 恢复版本 |
| `POST` | `/api/v1/workspaces/{id}/co-write` | 兼容旧版直接共写 |
| `POST` | `/api/v1/workspaces/{id}/co-write/preview` | 生成共写预览 |
| `POST` | `/api/v1/co-write-previews/{id}/apply` | 应用共写预览 |
| `POST` | `/api/v1/co-write-previews/{id}/discard` | 放弃共写预览 |
| `GET` | `/api/v1/drafts/{id}/evidence-bindings` | 获取可信链 |
| `POST` | `/api/v1/drafts/{id}/evidence-bindings/rebuild` | 异步重建可信链 |
| `PATCH` | `/api/v1/evidence-bindings/{id}/status` | 更新证据绑定状态 |
| `GET` | `/api/v1/drafts/{id}/writing-risks` | 获取原创实证与 AI 写作味风险 |
| `GET` | `/api/v1/drafts/{id}/review-items` | 审查项列表 |
| `PATCH` | `/api/v1/review-items/{id}/status` | 更新审查项状态 |
| `POST` | `/api/v1/review-items/{id}/appeal` | 发起申诉 |
| `POST` | `/api/v1/review-items/{id}/recheck` | 手动复查 |
| `GET` | `/api/v1/appeals/{id}` | 获取申诉结果 |
| `POST` | `/api/v1/workspaces/{id}/knowledge-base/build` | 构建知识库 |
| `GET` | `/api/v1/workspaces/{id}/knowledge-base/chunks` | 知识库片段列表 |
| `POST` | `/api/v1/workspaces/{id}/knowledge-base/search` | 知识库检索 |
| `POST` | `/api/v1/drafts/{id}/export` | 导出 |
| `GET` | `/api/v1/exports/{jobId}/download` | 下载导出文件 |
| `GET` | `/api/v1/jobs/{id}` | 查询任务状态 |

## 7. 测试与验证

### 7.1 后端测试

测试目录：

- `backend/src/test/java/com/aipm/cowriting/application/service/`
- `backend/src/test/java/com/aipm/cowriting/interfaces/rest/`

覆盖内容包括：

- Workspace service / controller。
- Material controller，包括 UTF-8 文件预览用例。
- Parse quality service。
- Sufficiency service。
- Draft service / controller。
- Knowledge base service / controller。
- Evidence binding controller。
- CoWrite controller。
- CoWrite preview service。
- Review service / controller。
- Writing risk service / controller。
- Literature search service / controller，包括多 Provider 降级、质量评分和去重。
- Export controller。
- Job controller。

当前后端测试报告为 `62` 个测试通过，`0` 失败、`0` 错误、`0` 跳过。文档中出现的 `37 / 49 / 51 / 55 / 60` 是对应历史阶段的当时验证数量，不代表当前测试总数。

运行命令：

```bash
cd backend
mvn clean test
```

### 7.2 前端验证

前端没有组件级测试，当前验证为生产构建 + smoke token 检查。

运行命令：

```bash
cd frontend
npm run test
```

执行内容：

```bash
npm run build
node scripts/mvp-smoke.mjs
```

Smoke check 检查：

- 主路由存在。
- 核心 API 方法存在。
- 导出页包含交付确认、参考文献、可信链覆盖率、引用一致性。
- 共写预览抽屉包含冲突提示、逐段接受、应用选中段落。
- 工作台编辑器包含材料可信链、原始材料入口。
- 解析页包含解析质量清单和补充说明。
- dist 构建产物存在。

### 7.3 Demo 验证

Demo 指南在：

- `docs/guides/DEMO_GUIDE.md`

推荐演示路径：

1. 新建项目。
2. 上传文本 / 文件 / 图片。
3. 执行预处理与 AI 解析。
4. 查看解析质量清单并补充材料。
5. 执行材料充足性检查。
6. 生成初稿。
7. 构建知识库。
8. 进入工作台查看可信链、覆盖率、引用一致性、原始材料。
9. 生成共写预览，逐段接受或应用为新版本。
10. 查看审查项、申诉或复查。
11. 导出 DOCX / PDF。

## 8. 本地运行与配置

### 8.1 后端启动

普通启动：

```bash
cd backend
mvn spring-boot:run
```

热部署开发模式：

```bash
cd backend
./scripts/dev-hot.sh
```

你已经补了：

- Spring Boot DevTools 依赖。
- `application.yml` 中 devtools restart 配置。
- `backend/scripts/dev-hot.sh` 监听源码变更、编译并触发重启。

### 8.2 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认地址：

- 前端：`http://localhost:5173`
- 后端：`http://localhost:8080`
- Vite 代理：`/api -> http://localhost:8080`

### 8.3 Supabase 配置

配置指南：

- `docs/guides/supabase-setup.md`

后端通过 `backend/.env` 读取：

```bash
SUPABASE_DB_HOST=...
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=...
SUPABASE_DB_PASSWORD=...
OPENAI_API_KEY=...
OPENAI_BASE_URL=...
OPENAI_MODEL=...
OPENAI_TIMEOUT_SECONDS=...
```

`application.yml` 关键点：

- 通过 `optional:file:.env[.properties]` 加载环境变量。
- JDBC 使用 Supabase Session pooler。
- SSL mode require。
- Hikari 最大连接池设为 5，避免 Supabase session pooler 连接耗尽。
- `ddl-auto: update` 用于开发阶段自动建表。

### 8.4 OpenAI 兼容配置

配置类：

- `OpenAiProperties.java`

所有 OpenAI 服务会规范化 base url：

- base url 为空时用 `https://api.openai.com/v1`
- 不以 `/v1` 结尾时自动补 `/v1`

当前 AI 任务：

- 语义解析。
- 图片 OCR。
- 初稿生成。
- 共写改写。
- 审查生成。
- 申诉复审。
- 单条审查项复查。

## 9. 文档体系

你已经建立了完整交付文档体系。

| 文档 | 内容 |
| --- | --- |
| `README.md` | 项目总入口、当前能力、启动方式、验证方式、边界 |
| `docs/README.md` | 文档目录索引 |
| `docs/product/PRD.md` | 产品与开发总规格 |
| `docs/design/frontend_page_spec.md` | 页面字段清单与组件树 |
| `docs/design/P7_workspace_design_review.md` | 共写工作台设计评审 |
| `docs/design/P7_workspace_visual_spec.md` | 共写工作台视觉规范 |
| `docs/engineering/backend_service_spec.md` | 后端服务拆分与状态机 |
| `docs/engineering/api_field_spec.md` | 接口字段级定义 |
| `docs/engineering/openapi_contract_draft.md` | OpenAPI 风格接口契约 |
| `docs/engineering/engineering_tasks.md` | 前后端任务拆分 |
| `docs/guides/DEMO_GUIDE.md` | Demo 启动与演示说明 |
| `docs/guides/supabase-setup.md` | Supabase 配置说明 |
| `docs/project/FINAL_DELIVERY_CHECKLIST.md` | 最终交付清单 |
| `docs/project/PRODUCT_COMPLETION_STATUS-6-16.md` | 当前产品完成度快照 |
| `docs/personal/resume_project_experience.md` | 简历项目经历描述 |

本文件属于新的工程汇总文档，用于把“实际代码逻辑 + 历史完成事项 + 当前边界”集中到一处。

## 10. 当前边界与未完成项

这些内容不属于当前 MVP 已完成范围：

- 用户登录。
- 租户隔离。
- 权限体系。
- Supabase RLS 系统化策略。
- 生产级对象存储。
- 生产级异步队列。
- job 持久化、失败重试、死信机制。
- AI 调用日志、成本统计、监控告警。
- Playwright / Cypress E2E。
- 前端组件测试。
- 正式上线部署流水线。
- PDF 页码级精准跳转。
- 原文截图定位。
- 更细粒度词级 diff。
- 复杂冲突合并。
- 应用预览后的明确复查任务列表。
- 更正式的论文排版。
- 真实批注导出。
- 更细的原创材料采集模板和段落补强任务。
- 候选文献阅读任务与中文学术元数据增强。

当前实现中也有一些设计与代码的差异：

- SQL 草案里有 `generation_jobs`，但当前 job 是内存实现。
- SQL 草案里有 `preprocessed_contents`，但当前预处理文本存在 `materials` 表字段。
- `api_field_spec.md` 和 `openapi_contract_draft.md` 标题及主体仍是 v1.6 历史规格，未完整同步 v1.8 写作风险和 v1.9 文献候选接口。
- 历史文档保留了 `37 / 49 / 51 / 55 / 60` 等阶段测试数量；当前统一验证口径是 `62` 个测试通过。
- v1.9 迁移 SQL 已交付且 JPA 可自动建表，但尚无手动执行正式 Supabase 迁移的文档记录。
- 前端依赖里 `react-router-dom` 标注 `^7.16.0`，实际 API 仍按 BrowserRouter / Route / Routes 常规方式使用。

## 11. 下一阶段建议

从当前产品状态看，不建议继续无限扩功能。更合理的下一阶段是围绕“可信、可控、可交付”继续打磨。

P0：

- PDF 页码级精准跳转与原文截图定位。
- 共写词级 diff 与复杂冲突合并。
- 应用预览后的复查任务列表。
- 任务状态统一化，把解析、生成、可信链、导出都统一成可持久查询的 job。

P1：

- 建立问卷、访谈、实验、案例、数据和观察记录等原创材料采集模板。
- 增加候选文献阅读任务，并继续增强中文学术元数据来源。
- 强化 APA / GB/T 7714 格式化与正文引用一致性。
- 优化 DOCX / PDF 正式论文排版。

P2：

- Playwright / Cypress E2E。
- 生产级异步队列。
- AI 调用日志与成本统计。
- 用户体系与权限隔离。
- 云对象存储。

## 12. 最终状态判断

当前项目已经具备以下真实能力：

- 创建论文项目。
- 上传文本、文件、图片、链接。
- 文件文本抽取。
- 图片 OCR。
- AI 语义解析。
- 解析质量清单。
- 材料补充与重新解析。
- 材料角色纠正。
- 文献元数据补全。
- Requirement Snapshot。
- 材料充足性检查。
- 材料不足阻断。
- 多源文献元数据检索与失败降级。
- 候选文献质量评分、去重和待下载清单。
- 上传原文时关联候选文献。
- 真实初稿生成。
- 版本管理。
- 项目知识库。
- 知识库检索。
- 工作台内证据检索。
- 段落级可信链。
- 可信链覆盖率。
- 引用一致性检查。
- 原始材料预览。
- AI 共写预览。
- 共写约束控制。
- 逐句 / 逐段 / 局部接受。
- 冲突提示。
- 共写关联审查项落库。
- 审查项生成。
- 申诉复审。
- 单条手动复查。
- 复查历史。
- 段落级原创实证不足、空泛论证和 AI 写作味风险提示。
- 基于已有材料生成原创实证补强预览。
- DOCX / PDF 导出。
- 导出前交付确认。
- 前端构建与 smoke 验证。
- 后端 service / controller 测试体系。
- Supabase 本地开发配置。
- 后端热部署开发脚本。

最终判断：

`这已经是一套能演示、能联调、能从材料到定稿跑通的 AI 论文共写 MVP，并完成 v1.8 原创实证补强与 v1.9 文献补充增强首轮建设。它最有价值的部分不是“能生成论文”，而是围绕 AI 输出可信度形成了完整产品设计：材料不足时帮助用户寻找真实文献但不越权生成、解析质量可补、证据链可追溯、原创实证可补强、共写先预览、审查可申诉、复查可留痕、导出前有风险提示。当前产品适合完整 Demo 和用户验证，但尚未达到生产上线标准。`
