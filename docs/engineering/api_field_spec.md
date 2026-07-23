# AI 论文共写工作台 v2.2.1 接口字段级定义

> 本文档保留 v1.x 字段定义。v2.0 当前新增字段与接口见 [v2_academic_workspace_api.md](v2_academic_workspace_api.md)。

本文档描述当前代码已经实现的 REST API 字段。历史设计中未落地的独立接口不再写入本文件，避免前后端按旧契约开发。

## 1. 通用约定

- API 前缀：`/api/v1`
- 所有业务接口必须携带 Supabase access token；资源按 JWT `sub` 隔离，访问他人资源统一返回 `404`。
- 实际响应统一由 `ApiResponse` 包裹：`{ "success": true, "data": {}, "meta": {} }`。
- 下文的响应体默认只展示 `data` 字段内容。
- 时间字段使用 ISO 8601。
- 主键字段使用 UUID 字符串。
- 枚举值按当前 Java enum 输出，通常为大写。

## 2. Workspace

### `POST /workspaces`

请求体：

```json
{
  "title": "基于物联网与机器学习的高校教室能源智能管理系统设计与实现"
}
```

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 项目 ID |
| `title` | string | 项目标题 |
| `status` | string | 项目状态 |
| `currentDraftVersionId` | UUID/null | 当前草稿版本 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

### `GET /workspaces`

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `items` | array | 项目列表 |
| `pagination.page` | number | 当前页 |
| `pagination.pageSize` | number | 每页数量 |
| `pagination.total` | number | 总数 |

### `GET /workspaces/{id}`

返回单个 `WorkspaceResponse`。

## 3. Materials

### `POST /workspaces/{id}/materials`

请求类型：`multipart/form-data`

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `files` | file[] | 否 | 上传文件；前端可队列提交，后端按材料创建记录 |
| `plainText` | string | 否 | 粘贴文本 |
| `externalLink` | string | 否 | 外部链接 |
| `sourceType` | string | 是 | `upload / pasted_text / external_link` 等 |
| `isKeyMaterial` | boolean | 否 | 是否关键材料 |

约束：`files / plainText / externalLink` 至少提供一种。

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `items` | MaterialResponse[] | 本次创建的材料 |

### `GET /workspaces/{id}/materials`

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `items` | MaterialResponse[] | 项目材料列表 |
| `pagination` | object | 分页信息 |

`MaterialResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 材料 ID |
| `filename` | string | 文件名或材料标题 |
| `fileType` | string | 文件类型 |
| `sourceType` | string | 来源类型 |
| `isKeyMaterial` | boolean | 是否关键材料 |
| `parseStage` | string | `UPLOADED / PREPROCESSED / AI_PARSED / AI_FAILED` 等 |
| `confidenceScore` | number/null | AI 解析置信度 |
| `aiMaterialCategory` | string/null | AI 判断的材料类型 |
| `effectiveMaterialCategory` | string/null | 当前生效材料类型 |
| `categoryOverridden` | boolean | 是否用户手动纠正类型 |
| `summary` | string/null | 摘要 |
| `topicRelation` | string/null | 与论文主题关系 |
| `detectedClaims` | string[] | 识别出的观点 / 结论 |
| `detectedEvidence` | string[] | 识别出的证据 |
| `detectedRequirements` | string[] | 识别出的老师要求 |
| `bibliographicMetadata` | object/null | 作者、年份、题名、期刊/出版社、链接等 |
| `parseQuality` | ParseQualityReport | v1.5 解析质量报告 |

`parseQuality` 示例：

```json
{
  "status": "NEEDS_SUPPLEMENT",
  "score": 0.48,
  "nextAction": "请补充材料背景或重新上传更清晰文件后再继续。",
  "completeness": {
    "summary": false,
    "topicRelation": true,
    "category": true,
    "claims": false,
    "evidence": false,
    "requirements": false,
    "bibliographicMetadata": true
  },
  "issues": [
    {
      "code": "RESEARCH_EVIDENCE_MISSING",
      "level": "BLOCKING",
      "label": "缺少研究证据",
      "message": "该关键材料缺少可支撑正文的证据或数据说明。",
      "suggestedAction": "补充研究结论、数据来源、样本或图表含义。",
      "supplementPrompt": "这份材料可以支撑的研究证据是："
    }
  ]
}
```

`parseQuality.status`：

| 状态 | 含义 | 是否阻断继续 |
| --- | --- | --- |
| `READY` | 可用于后续生成 | 否 |
| `NEEDS_CONFIRMATION` | 建议用户确认 | 否 |
| `NEEDS_SUPPLEMENT` | 需要补充信息 | 关键材料阻断 |
| `FAILED` | 解析失败或无可用文本 | 关键材料阻断 |

### `POST /materials/{id}/preprocess`

执行文件文本抽取、图片 OCR 或基础预处理。

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `jobId` | string | 任务 ID |
| `status` | string | 当前实现通常返回 `success` |

### `POST /materials/{id}/ai-parse`

请求体可为空。前端保留兼容字段：

```json
{
  "forceRetry": true
}
```

响应字段同 `preprocess`。

### `GET /materials/{id}/preview`

返回原始材料预览入口。文本 / 链接材料返回可直接展示的信息，文件材料返回后端下载地址。

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 材料 ID |
| `filename` | string | 材料文件名或标题 |
| `fileType` | string/null | 文件类型 |
| `previewType` | string | `file / external_link / text` |
| `previewText` | string/null | 文本预览，最多截取一段 |
| `downloadUrl` | string/null | 文件下载 / 预览地址 |
| `externalLink` | string/null | 外部链接 |

### `GET /materials/{id}/file`

返回材料原始文件流。当前用于工作台证据卡的“打开原始材料”入口。

### `POST /materials/{id}/supplement`

请求类型：`multipart/form-data`

字段：

| 字段 | 类型 | 必填 | 说明 |
| --- | --- | --- | --- |
| `supplementText` | string | 是 | 用户补充说明 |
| `pageRef` | number | 否 | 页码或位置线索 |

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 补充记录 ID |
| `materialId` | UUID | 材料 ID |
| `accepted` | boolean | 是否接受 |
| `pageRef` | number/null | 位置线索 |

### `PATCH /materials/{id}/category`

请求体：

```json
{
  "category": "REFERENCE_MATERIAL"
}
```

用于用户纠正材料类型。

### `PATCH /materials/{id}/bibliographic-metadata`

请求体：

```json
{
  "authors": ["Smith, J."],
  "year": "2024",
  "title": "AI-supported academic writing",
  "venue": "Journal of Learning Technology",
  "publisher": "Example Press",
  "link": "https://example.com/paper"
}
```

用于补全文献信息，支撑 APA / GB/T 7714 引用格式。

## 4. Requirement Snapshot

### `POST /workspaces/{id}/requirement-snapshot`

请求体：

```json
{
  "topic": "人工智能对大学生学习方式的影响",
  "wordCount": 3000,
  "deadline": "2026-06-30T12:00:00Z",
  "citationStyle": "APA",
  "specialRequirements": {
    "minReferences": 5,
    "language": "zh-CN"
  }
}
```

### `GET /workspaces/{id}/requirement-snapshot`

返回当前项目的老师要求快照。

## 5. Material Sufficiency

### `POST /workspaces/{id}/material-sufficiency-check`

请求体：

```json
{
  "requirementSnapshotId": "uuid"
}
```

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 检查记录 ID |
| `workspaceId` | UUID | 项目 ID |
| `isGenerationEligible` | boolean | 是否允许生成 |
| `missingItems` | object[] | 缺失项 |
| `recommendedSupplements` | object[] | 建议补充内容 |
| `reason` | string | 总体原因 |

## 6. Draft

### `POST /workspaces/{id}/generate-draft`

请求体：

```json
{
  "requirementSnapshotId": "uuid",
  "mode": "default"
}
```

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `jobId` | string | 任务 ID |
| `status` | string | 当前生成状态 |

### `GET /workspaces/{id}/drafts`

返回项目草稿版本列表。

### `GET /drafts/{id}`

`DraftResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 草稿版本 ID |
| `workspaceId` | UUID | 项目 ID |
| `versionNo` | number | 版本号 |
| `titleSuggestion` | string | 标题建议 |
| `outline` | object | 正文结构 |
| `paragraphSkeletons` | object/array | 段落骨架 |
| `draftText` | string | 正文 |
| `sourceTraceMap` | object | 段落到材料的追溯图 |
| `generationStatus` | string | `SUCCESS / FAILED` 等 |
| `createdBy` | string | 创建来源 |
| `createdAt` | datetime | 创建时间 |

### `PATCH /drafts/{id}`

请求体：

```json
{
  "draftText": "用户手动修改后的正文"
}
```

### `POST /drafts/{id}/restore`

恢复指定历史版本为当前版本。

## 7. Co-write

### `POST /workspaces/{id}/co-write`

兼容旧流程：直接生成新版本。前端默认不再走该接口。

### `POST /workspaces/{id}/co-write/preview`

默认共写入口：只生成预览，不创建新版本。

请求体：

```json
{
  "draftVersionId": "uuid",
  "action": "rewrite_selection",
  "targetRange": {
    "start": 120,
    "end": 260
  },
  "instruction": "请让这一段更具体，但保留原有引用。",
  "controls": {
    "rewriteDepth": "medium",
    "keepCitations": true,
    "keepData": true,
    "noNewSources": true,
    "keepStudentVoice": true
  }
}
```

`CoWritePreviewResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 预览 ID |
| `workspaceId` | UUID | 项目 ID |
| `draftVersionId` | UUID | 原草稿版本 |
| `action` | string | 共写动作 |
| `targetRange` | object/null | 目标范围 |
| `instruction` | string | 用户指令 |
| `controls` | object | 共写约束 |
| `candidateTitleSuggestion` | string | 候选标题 |
| `candidateDraftText` | string | 候选正文 |
| `candidateSourceTraceMap` | object | 候选来源追溯 |
| `diffSummary` | object | 差异摘要、修改理由、关联审查项等 |
| `status` | string | `READY / APPLIED / DISCARDED` 等 |
| `createdAt` | datetime | 创建时间 |
| `appliedAt` | datetime/null | 应用时间 |

`diffSummary` v1.6 关键字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `paragraphDiffs` | object[] | 按段落拆分的候选修改，支持逐段接受 |
| `paragraphDiffs[].paragraphId` | string | 段落编号，如 `p1` |
| `paragraphDiffs[].changeType` | string | `added / removed / modified` |
| `paragraphDiffs[].originalText` | string | 原段落 |
| `paragraphDiffs[].candidateText` | string | AI 候选段落 |
| `paragraphDiffs[].intentLabel` | string | 修改意图，如表达优化、补充论证、补充引用 |
| `paragraphDiffs[].selectedByDefault` | boolean | 前端是否默认选中 |
| `conflictWarnings` | object[] | 引用、数据、来源相关冲突提示 |
| `recheckSuggestion` | object | 应用后建议复查的审查项统计与关联审查项 |
| `recheckSuggestion.relatedReviewItems` | object[] | 可能被本次共写影响的审查项 ID 和关联原因 |

### `POST /co-write-previews/{id}/apply`

应用预览并创建新的 draft version。

响应：`DraftResponse`

### `POST /co-write-previews/{id}/discard`

放弃预览，不影响当前草稿。

响应：`CoWritePreviewResponse`

## 8. Evidence Bindings

### `GET /drafts/{id}/evidence-bindings`

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `draftVersionId` | UUID | 草稿版本 |
| `paragraphs` | EvidenceParagraphResponse[] | 按段落分组的证据链 |
| `missingParagraphIds` | string[] | 缺来源段落 |
| `usedMaterials` | EvidenceMaterialResponse[] | 已使用材料 |
| `unusedMaterials` | EvidenceMaterialResponse[] | 未使用材料 |
| `coverage` | EvidenceCoverageReport | v1.6 材料覆盖率评分 |
| `citationConsistency` | CitationConsistencyReport | v1.6 引用一致性检查 |

`EvidenceCoverageReport`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `totalParagraphs` | number | 段落总数 |
| `confirmedParagraphs` | number | 已确认或强绑定段落数 |
| `weakParagraphs` | number | 弱绑定段落数 |
| `missingParagraphs` | number | 缺来源段落数 |
| `coverageRatio` | number | 有来源段落占比，0-100 |
| `confirmedRatio` | number | 强确认段落占比，0-100 |
| `healthLabel` | string | 覆盖率状态说明 |
| `recommendations` | string[] | 后续补证据建议 |

`CitationConsistencyReport`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `status` | string | `READY / NEEDS_REVIEW / NEEDS_FIX` |
| `detectedCitationCount` | number | 正文检测到的引用标记数 |
| `linkedMaterialCount` | number | 可信链已使用材料数 |
| `missingCitationParagraphCount` | number | 缺引用或缺来源段落数 |
| `orphanCitationCount` | number | 正文引用多于可信链材料的估算数量 |
| `incompleteReferenceCount` | number | 文献信息不完整数量 |
| `issues` | string[] | 引用一致性问题 |

`EvidenceParagraphResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `paragraphId` | string | 段落 ID |
| `paragraphText` | string | 段落文本 |
| `bindingStatus` | string | 段落整体绑定状态 |
| `bindings` | EvidenceBindingItemResponse[] | 证据绑定 |

`EvidenceBindingItemResponse` 关键字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 绑定 ID |
| `materialId` | UUID/null | 材料 ID |
| `knowledgeChunkId` | UUID/null | 知识库片段 ID |
| `materialTitle` | string | 材料标题 |
| `claimText` | string | 被支撑的正文观点 |
| `sourceExcerpt` | string | 原始证据片段 |
| `sourceLocation` | object | 页码、位置、材料线索 |
| `confidenceScore` | number | 绑定置信度 |
| `supportType` | string | 支撑类型 |
| `bindingStatus` | string | `CONFIRMED / WEAK / MISSING / USER_CONFIRMED` |
| `citationText` | string | 可插入引用文本 |
| `bibliographicMetadata` | object | 文献信息 |

`sourceLocation.previewUrl` 会返回 `/api/v1/materials/{id}/preview`，前端可据此打开原始材料预览入口。

### `POST /drafts/{id}/evidence-bindings/rebuild`

异步重建当前草稿可信链。

响应：

```json
{
  "jobId": "uuid",
  "status": "running"
}
```

### `PATCH /evidence-bindings/{id}/status`

请求体：

```json
{
  "status": "USER_CONFIRMED"
}
```

用于用户把弱绑定标记为可信或继续保留为待补充状态。

## 9. Review

### `GET /drafts/{id}/review-items`

返回审查项列表。

`ReviewItemResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | 审查项 ID |
| `reviewType` | string | 审查类型 |
| `reviewImpactLevel` | string | 影响等级 |
| `targetRange` | object | 指向正文范围 |
| `message` | string | 问题描述 |
| `suggestedFix` | string | 修改建议 |
| `canBypass` | boolean | 是否允许跳过 |
| `reviewStatus` | string | `OPEN / RESOLVED / IGNORED` |
| `resolutionNote` | string/null | 处理说明 |
| `resolvedAt` | datetime/null | 解决时间 |
| `lastRecheckedAt` | datetime/null | 最近复查时间 |
| `recheckNote` | string/null | 最近复查说明 |
| `recheckHistory` | array | 复查历史 |
| `createdAt` | datetime | 创建时间 |

### `PATCH /review-items/{id}/status`

请求体：

```json
{
  "status": "RESOLVED",
  "resolutionNote": "已根据建议补充材料来源。"
}
```

### `POST /review-items/{id}/appeal`

请求体：

```json
{
  "reason": "老师要求允许这部分重复作为章节小结。",
  "studentEvidence": "课程说明第 3 条要求每章有小结。"
}
```

### `POST /review-items/{id}/recheck`

手动复查单条审查项。

规则：

- `RESOLVED`：自动关闭审查项。
- `STILL_OPEN` 或 `NEEDS_MORE_EVIDENCE`：保持 `OPEN`。
- `DOWNGRADED`：降低影响等级，并按 AI 结果决定是否仍为 `OPEN`。

### `GET /appeals/{id}`

获取申诉 / 复审结果。

## 10. Knowledge Base

### `POST /workspaces/{id}/knowledge-base/build`

构建或重建项目知识库。

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `workspaceId` | UUID | 项目 ID |
| `createdCount` | number | 新增片段数 |
| `skippedCount` | number | 跳过片段数 |
| `message` | string | 结果说明 |

### `GET /workspaces/{id}/knowledge-base/chunks`

返回知识库片段列表。

### `POST /workspaces/{id}/knowledge-base/search`

请求体：

```json
{
  "query": "传感器数据噪声对预测精度的影响",
  "limit": 5
}
```

响应字段：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `query` | string | 检索问题 |
| `items` | KnowledgeChunkResponse[] | 匹配片段 |

## 11. Export

### `POST /drafts/{id}/export`

请求体：

```json
{
  "format": "docx",
  "includeReferences": true,
  "citationStyle": "APA"
}
```

响应：

```json
{
  "jobId": "uuid",
  "status": "success"
}
```

### `GET /exports/{jobId}/download`

获取导出文件下载信息。

## 12. Jobs

### `GET /jobs/{id}`

`JobDetailResponse`：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `id` | UUID | job ID |
| `workspaceId` | UUID/null | 所属项目 |
| `jobType` | string | 任务类型 |
| `status` | string | `QUEUED / RUNNING / SUCCESS / FAILED` 等 |
| `progress` | number | 进度 |
| `inputRef` | object/null | 输入引用 |
| `outputRef` | object/null | 输出引用 |
| `errorMessage` | string/null | 错误信息 |
| `createdAt` | datetime | 创建时间 |
| `updatedAt` | datetime | 更新时间 |

## 13. Onboarding And Project Guide

### `GET /me`

在用户资料基础上增加：

| 字段 | 类型 | 说明 |
| --- | --- | --- |
| `onboardingStatus` | string | `NOT_STARTED / COMPLETED / SKIPPED` |
| `onboardingVersion` | string/null | 最近完成或跳过的向导版本 |
| `onboardingCompletedAt` | datetime/null | 完成或跳过时间 |

### `PATCH /me/onboarding`

```json
{
  "status": "SKIPPED",
  "onboardingVersion": "v1"
}
```

`status` 只允许 `COMPLETED / SKIPPED`，不能由客户端重置为 `NOT_STARTED`。

### `POST /onboarding/complete`

请求体包含 `workspace` 和 `onboardingVersion`。`workspace` 在普通创建字段外增加：

```json
{
  "guideProfile": {
    "currentProgress": "MATERIALS_COLLECTING",
    "availableMaterials": ["REFERENCES", "NOTES_DRAFT"],
    "targetDeadline": "2026-12-20",
    "preferredMode": "GUIDED"
  }
}
```

响应返回 `user / workspace / guide`。创建与用户状态更新在同一事务内完成，重复完成请求返回已有首个项目。

### `GET /workspaces/{id}/guide`

返回 guide 配置以及派生字段：`overallProgress / currentTaskId / tasks`。每项任务包含 `id / phase / title / description / reason / expectedOutcome / status / targetPath / blocking / progressLabel`。

任务状态固定为：`COMPLETED / CURRENT / IN_PROGRESS / UPCOMING / OPTIONAL / NEEDS_ATTENTION`。

### `PATCH /workspaces/{id}/guide`

请求字段：`currentProgress / availableMaterials / targetDeadline / preferredMode`。修改 guide 只重新计算路线，不改变材料、正文或审查数据。
