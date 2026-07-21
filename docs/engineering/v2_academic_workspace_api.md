# v2.0.1 学术文档统一工作台 API 契约

更新时间：`2026-07-21`

统一前缀：`/api/v1`。响应继续使用现有 `ApiResponse<T>` 包装。

## 鉴权前提

- 所有接口必须携带 `Authorization: Bearer <supabase-access-token>`。
- workspace 和全部子资源按 JWT `sub` 校验 owner。
- 未登录返回 `401`；访问他人资源统一返回 `404`。
- `GET/PATCH /me` 用于读取或更新当前用户展示资料。
- 文件预览、job 查询和导出下载同样需要 Bearer Token。

## 0. 正文与分析作用域

- `DocumentSection` 是唯一可编辑正文源。
- 文档整篇内容由章节按顺序组装，只用于预览、检查、审查和导出，不提供全文写接口。
- `ContentScope` 统一描述 `LEGACY_DRAFT / DOCUMENT_SECTION / ACADEMIC_DOCUMENT` 的正文、版本、材料范围、来源追溯和要求上下文。
- 章节级问题的 `targetRange` 均为章节内字符范围，并携带 `sectionId`。
- 章节版本变化后，旧可信链或审查结果返回 `analysisState=STALE`，前端不得把它展示为当前结论。
- 旧 draft、`/workspace` 和旧共写 API 继续兼容，但不是默认产品入口。

## 1. 创建研究项目

`POST /workspaces`

```json
{
  "title": "智能教室能源管理研究",
  "academicProfile": {
    "academicStage": "MASTER",
    "disciplineGroup": "STEM",
    "researchParadigm": "QUANTITATIVE",
    "primaryLanguage": "zh-CN",
    "defaultCitationStyle": "APA",
    "institution": "示例大学",
    "aiUsagePolicy": "EVIDENCE_GROUNDED_DRAFTING",
    "aiPolicy": {
      "humanReviewRequired": true,
      "disclosureRequired": true
    }
  },
  "initialDocument": {
    "title": "智能教室能源管理研究开题",
    "documentType": "RESEARCH_PROPOSAL",
    "targetInstitution": "示例大学",
    "targetVenue": null,
    "targetLength": 6000,
    "lengthUnit": "WORDS",
    "citationStyle": "APA",
    "requirementProfile": {},
    "primaryDocument": true
  }
}
```

旧请求 `{ "title": "..." }` 继续兼容，并自动创建 `UNDERGRADUATE + COURSE_PAPER`。

## 2. 学术画像

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/workspaces/{id}/academic-profile` | 获取画像；旧项目不存在时自动补默认画像 |
| PATCH | `/workspaces/{id}/academic-profile` | 更新阶段、学科、范式、语言、引用格式、机构和 AI 策略 |

## 3. 多文档

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/workspaces/{id}/documents` | 创建文档并生成默认章节树 |
| GET | `/workspaces/{id}/documents` | 获取项目全部文档 |
| GET | `/documents/{id}` | 获取单个文档 |
| PATCH | `/documents/{id}` | 更新标题、状态、机构、期刊、篇幅、引用格式和专属要求 |
| POST | `/documents/{id}/activate` | 设为工作区当前文档 |

`documentType`：`COURSE_PAPER / RESEARCH_PROPOSAL / UNDERGRADUATE_THESIS / MASTER_THESIS / DOCTORAL_DISSERTATION / JOURNAL_ARTICLE / CONFERENCE_PAPER / LITERATURE_REVIEW / RESEARCH_REPORT`。

## 4. 章节与版本

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/documents/{id}/sections` | 新增章节 |
| GET | `/documents/{id}/sections` | 按顺序获取章节树 |
| PATCH | `/sections/{id}` | 保存章节正文、状态、目标篇幅和来源追溯 |
| PATCH | `/documents/{id}/sections/order` | 一次提交当前文档全部章节 ID，保存拖拽后的顺序 |
| GET | `/sections/{id}/versions` | 获取章节历史版本 |
| POST | `/section-versions/{id}/restore` | 恢复指定章节版本并创建新恢复点 |

章节保存示例：

```json
{
  "title": "研究方法",
  "content": "...",
  "targetLength": 3000,
  "status": "DRAFTING",
  "sourceTraceMap": {
    "p1": { "materialIds": ["uuid"] }
  },
  "changeSummary": "补充样本与变量说明"
}
```

章节排序请求：

```json
{
  "sectionIds": ["section-2-uuid", "section-1-uuid", "section-3-uuid"]
}
```

请求必须包含当前文档的全部章节且不能重复。排序只更新 `sortOrder`，不会递增章节 `versionNo` 或创建 `document_section_versions` 记录。

## 5. Readiness

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/documents/{id}/readiness-check` | 获取文档整体材料和章节准备度 |
| POST | `/sections/{id}/readiness-check` | 获取当前章节准备度 |

```json
{
  "documentId": "uuid",
  "status": "READY | NEEDS_CONFIRMATION | NEEDS_SUPPLEMENT",
  "score": 87,
  "generationEligible": true,
  "issues": [
    {
      "code": "RESEARCH_ARTIFACT_MISSING",
      "level": "BLOCKING",
      "label": "缺少研究数据或分析依据",
      "message": "...",
      "suggestedAction": "...",
      "sectionId": "uuid"
    }
  ],
  "artifactCoverage": {
    "parsedKeyMaterial": true,
    "literature": true,
    "submissionRequirement": true,
    "researchArtifact": false,
    "authorDraft": true
  },
  "nextAction": "..."
}
```

## 6. 文档材料范围

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/documents/{id}/materials` | 新增或更新某材料在文档中的角色和 included 状态 |
| GET | `/documents/{id}/materials` | 获取文档材料选择记录 |

未建立任何 link 时默认使用项目全部材料；一旦建立 link，只使用 `included=true` 的材料。

知识库检索 `POST /workspaces/{id}/knowledge-base/search` 新增可选字段：

```json
{
  "query": "问卷调查与回归分析",
  "limit": 8,
  "documentId": "uuid",
  "materialIds": [],
  "tags": ["methodology"]
}
```

## 7. 章节 AI

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/sections/{id}/generate` | 基于当前文档选定材料生成当前章节 |
| POST | `/sections/{id}/co-write/preview` | 生成共写候选，不覆盖正文 |
| POST | `/section-co-write-previews/{id}/apply` | 应用候选并创建章节新版本 |
| POST | `/section-co-write-previews/{id}/discard` | 放弃候选 |

```json
{
  "action": "improve_expression",
  "instruction": "保持作者声音并明确研究边界",
  "controls": {
    "rewriteDepth": "balanced",
    "keepCitations": true,
    "keepData": true,
    "noNewSources": true,
    "keepStudentVoice": true
  },
  "targetRange": {
    "mode": "selection",
    "start": 120,
    "end": 286
  }
}
```

`targetRange` 省略时处理当前章节；选区范围使用章节内字符偏移。

预览响应额外包含：`baseContent / targetRange / diffRows / paragraphDiffRows / relatedReviewItemIds`。

应用请求：

```json
{
  "mode": "PARAGRAPHS",
  "selectedIds": ["p1", "p3"]
}
```

`mode` 固定为：

| 值 | 行为 |
| --- | --- |
| `ALL` | 应用全部候选正文 |
| `PARAGRAPHS` | 仅应用 `paragraphDiffRows` 中选定 ID |
| `DIFF_ROWS` | 仅应用 `diffRows` 中选定 ID |

应用成功只创建一个章节新版本。若当前章节版本不再等于预览的 `baseVersionNo`，返回 `409 WORKSPACE_STATUS_CONFLICT`，不会覆盖新正文。关联审查项会进入 `MODIFIED_PENDING_RECHECK`。

若画像的 `aiUsagePolicy=GUIDANCE_ONLY`，正文生成和共写返回禁止操作；材料不足时生成接口返回 `422`，不会编造内容。

章节生成与共写预览会在 AI 调用前记录基准版本，提交时按章节历史最大版本加行锁写入。如果 AI 处理期间章节已被用户或其他请求修改，返回 `409 WORKSPACE_STATUS_CONFLICT`，不会覆盖新内容。数据库唯一约束冲突统一返回 `409 DATA_INTEGRITY_CONFLICT`。

兼容要求快照读取支持 `GET /workspaces/{id}/requirement-snapshot?optional=true`。快照不存在时返回 `200` 且 `data=null`，默认强制读取仍保留 `404 REQUIREMENT_SNAPSHOT_MISSING`。

## 8. 章节与整篇质量接口

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET | `/sections/{id}/evidence-bindings` | 获取当前章节段落级可信链 |
| POST | `/sections/{id}/evidence-bindings/rebuild` | 异步重建当前章节可信链，返回 `202 + JobResponse` |
| GET | `/documents/{id}/evidence-summary` | 聚合整篇覆盖率、缺来源和引用一致性 |
| GET | `/sections/{id}/writing-risks` | 获取当前章节原创实证与写作风险 |
| GET | `/documents/{id}/writing-risks` | 聚合整篇写作风险 |
| GET | `/documents/{id}/quality-summary` | 获取整篇交付质量汇总和章节明细 |
| GET | `/documents/{id}/review-items` | 按章节、状态和作用域筛选文档审查项 |
| POST | `/sections/{id}/review-items/refresh` | 手动异步发起本章 AI 深度审查 |
| POST | `/documents/{id}/review-items/refresh` | 手动异步发起整篇 AI 深度审查 |

`GET /documents/{id}/review-items` 可选查询参数：`sectionId / status / scopeType`。

章节保存、AI 生成、版本恢复、共写应用和旧稿拆分只自动触发可信链异步重建；AI 深度审查不会因保存自动调用。异步任务继续通过 `GET /jobs/{jobId}` 轮询。

质量结果公共作用域字段：

```json
{
  "scopeType": "DOCUMENT_SECTION",
  "documentId": "uuid",
  "sectionId": "uuid",
  "sectionVersionNo": 5,
  "analysisState": "CURRENT"
}
```

`analysisState` 固定为 `CURRENT / STALE`。审查状态固定为 `OPEN / MODIFIED_PENDING_RECHECK / RESOLVED / IGNORED / SUPERSEDED`。旧 `PATCH /evidence-bindings/{id}/status`、申诉和 `POST /review-items/{id}/recheck` 接口继续用于用户确认、申诉和单项复查。

## 9. 组装、导出与 AI 留痕

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/documents/{id}/assemble` | 按章节顺序组装全文与来源追溯 |
| POST | `/documents/{id}/export` | 组装后复用现有异步 DOCX/PDF 导出 |
| GET | `/documents/{id}/ai-actions` | 获取文档 AI 操作和采纳记录 |

`/documents/{id}/assemble` 与 `/documents/{id}/export` 都读取章节组装快照。导出不会写入 `draft_versions`，因此不会产生第二份可编辑全文。

## 10. 旧稿预览拆分

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| POST | `/sections/{id}/split-preview` | 识别 `LEGACY_FULL_TEXT` 标题并返回拆分预览 |
| POST | `/sections/{id}/split` | 用户确认预览后创建章节 |

确认请求必须携带预览的 `baseVersionNo` 和用户确认后的章节数组：

```json
{
  "baseVersionNo": 3,
  "sections": [
    {
      "title": "绪论",
      "sectionType": "INTRODUCTION",
      "content": "..."
    }
  ]
}
```

拆分前全文保留在原章节历史中。基准版本不一致时拒绝应用，避免把预览覆盖到新修改上。

## 11. 数据库迁移

正式迁移文件：[20260711031857_v2_academic_workspace.sql](../../supabase/migrations/20260711031857_v2_academic_workspace.sql)。

章节版本一致性修复：[20260711065932_repair_document_section_versions.sql](../../supabase/migrations/20260711065932_repair_document_section_versions.sql)。

v2.0.1 统一质量作用域：[20260711092831_academic_document_quality_scopes.sql](../../supabase/migrations/20260711092831_academic_document_quality_scopes.sql)。

v2.1 用户隔离：[20260721002107_user_auth_and_workspace_isolation.sql](../../supabase/migrations/20260721002107_user_auth_and_workspace_isolation.sql)。

新增表：`academic_project_profiles / academic_documents / document_sections / document_section_versions / document_material_links / ai_action_logs / section_cowrite_previews`。

v2.0.1 新增 `section_cowrite_preview_review_links`，并扩展 `evidence_bindings / review_items / review_recheck_logs / section_cowrite_previews`。`draft_version_id` 在作用域表中改为可空，旧数据保留为 `LEGACY_DRAFT`。

运行时 `HIBERNATE_DDL_AUTO` 默认 `none`；禁止依赖 JPA 自动建表替代正式迁移。
