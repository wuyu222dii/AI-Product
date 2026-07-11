# AI 论文共写工作台 v1.6 OpenAPI 风格接口契约

> 本文档保留 v1.x 兼容接口。v2.0 学术画像、多文档、章节、readiness 和 AI 留痕接口见 [v2_academic_workspace_api.md](v2_academic_workspace_api.md)。

本文档是当前实现的 OpenAPI 风格契约说明，不是可直接导入的 YAML 文件。字段级细节以 [api_field_spec.md](api_field_spec.md) 为准。

## 1. 基础信息

- Base URL：`http://localhost:8080/api/v1`
- 当前认证：MVP 暂无登录态、令牌鉴权、权限隔离和 RLS 强制策略。
- 后续生产化：需要补用户体系、资源归属校验、Supabase RLS、审计日志和限流。
- 响应封装：所有成功响应统一为 `ApiResponse<T>`。

成功响应：

```json
{
  "success": true,
  "data": {},
  "meta": {
    "path": "/api/v1/workspaces",
    "method": "POST",
    "timestamp": "2026-06-20T00:00:00Z"
  }
}
```

失败响应：

```json
{
  "success": false,
  "error": {
    "code": "VALIDATION_ERROR",
    "message": "title 不能为空"
  },
  "meta": {}
}
```

## 2. 当前端点总览

### Workspace

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces` | `201` | 创建项目 |
| `GET` | `/workspaces` | `200` | 获取项目列表 |
| `GET` | `/workspaces/{id}` | `200` | 获取项目详情 |

### Materials

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces/{id}/materials` | `201` | 上传或创建材料 |
| `GET` | `/workspaces/{id}/materials` | `200` | 获取材料列表，包含 `parseQuality` |
| `POST` | `/materials/{id}/preprocess` | `202` | 预处理材料 |
| `POST` | `/materials/{id}/ai-parse` | `202` | AI 语义解析 |
| `GET` | `/materials/{id}/preview` | `200` | 获取原始材料预览入口 |
| `GET` | `/materials/{id}/file` | `200` | 获取原始文件流 |
| `POST` | `/materials/{id}/supplement` | `200` | 补充说明并重新解析 |
| `PATCH` | `/materials/{id}/category` | `200` | 用户纠正材料类型 |
| `PATCH` | `/materials/{id}/bibliographic-metadata` | `200` | 补全文献信息 |

### Requirement

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces/{id}/requirement-snapshot` | `201` | 创建或更新老师要求快照 |
| `GET` | `/workspaces/{id}/requirement-snapshot` | `200` | 获取老师要求快照 |

### Material Sufficiency

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces/{id}/material-sufficiency-check` | `201` | 检查材料是否足够生成 |

### Draft

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces/{id}/generate-draft` | `202` | 生成初稿 |
| `GET` | `/workspaces/{id}/drafts` | `200` | 获取草稿版本列表 |
| `GET` | `/drafts/{id}` | `200` | 获取草稿详情 |
| `PATCH` | `/drafts/{id}` | `200` | 保存用户手动编辑 |
| `POST` | `/drafts/{id}/restore` | `200` | 恢复历史版本 |

### Co-write

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces/{id}/co-write` | `202` | 兼容旧流程：直接共写并创建新版本 |
| `POST` | `/workspaces/{id}/co-write/preview` | `201` | 生成共写预览，不创建新版本 |
| `POST` | `/co-write-previews/{id}/apply` | `200` | 应用预览并创建新版本 |
| `POST` | `/co-write-previews/{id}/discard` | `200` | 放弃预览 |

### Evidence Bindings

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/drafts/{id}/evidence-bindings` | `200` | 获取段落级可信链 |
| `POST` | `/drafts/{id}/evidence-bindings/rebuild` | `202` | 异步重建可信链 |
| `PATCH` | `/evidence-bindings/{id}/status` | `200` | 用户确认或调整绑定状态 |

### Review

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/drafts/{id}/review-items` | `200` | 获取审查项 |
| `PATCH` | `/review-items/{id}/status` | `200` | 更新审查项状态 |
| `POST` | `/review-items/{id}/appeal` | `201` | 发起申诉 / 复审 |
| `POST` | `/review-items/{id}/recheck` | `200` | 手动复查单条审查项 |
| `GET` | `/appeals/{id}` | `200` | 获取申诉结果 |

### Knowledge Base

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/workspaces/{id}/knowledge-base/build` | `201` | 构建或重建项目知识库 |
| `GET` | `/workspaces/{id}/knowledge-base/chunks` | `200` | 获取知识库片段 |
| `POST` | `/workspaces/{id}/knowledge-base/search` | `200` | 关键词检索知识片段 |

### Export

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `POST` | `/drafts/{id}/export` | `202` | 导出 docx / pdf |
| `GET` | `/exports/{jobId}/download` | `200` | 获取导出下载信息 |

### Jobs

| Method | Path | 状态码 | 说明 |
| --- | --- | --- | --- |
| `GET` | `/jobs/{id}` | `200` | 获取任务状态 |

## 3. 关键请求契约

### 创建项目

`POST /workspaces`

```json
{
  "title": "论文题目"
}
```

### 上传材料

`POST /workspaces/{id}/materials`

请求类型：`multipart/form-data`

```text
files=<binary>
plainText=课程论文要求...
externalLink=https://example.com
sourceType=pasted_text
isKeyMaterial=true
```

### AI 解析

`POST /materials/{id}/ai-parse`

请求体可为空；前端可保留兼容字段：

```json
{
  "forceRetry": true
}
```

### 材料预览

`GET /materials/{id}/preview`

无需请求体。文件材料返回 `downloadUrl`，文本材料返回 `previewText`，链接材料返回 `externalLink`。

### 补充说明

`POST /materials/{id}/supplement`

请求类型：`multipart/form-data`

```text
supplementText=这份材料的样本量为 200，数据来自 2025 年问卷。
pageRef=3
```

### 生成初稿

`POST /workspaces/{id}/generate-draft`

```json
{
  "requirementSnapshotId": "uuid",
  "mode": "default"
}
```

### 共写预览

`POST /workspaces/{id}/co-write/preview`

```json
{
  "draftVersionId": "uuid",
  "action": "rewrite_selection",
  "targetRange": {
    "start": 120,
    "end": 260
  },
  "instruction": "请增强论证，但不要新增来源。",
  "controls": {
    "rewriteDepth": "medium",
    "keepCitations": true,
    "keepData": true,
    "noNewSources": true,
    "keepStudentVoice": true
  }
}
```

### 更新证据绑定状态

`PATCH /evidence-bindings/{id}/status`

```json
{
  "status": "USER_CONFIRMED"
}
```

### 手动复查审查项

`POST /review-items/{id}/recheck`

请求体为空。

### 知识库检索

`POST /workspaces/{id}/knowledge-base/search`

```json
{
  "query": "预测精度受哪些因素影响",
  "limit": 5
}
```

### 导出

`POST /drafts/{id}/export`

```json
{
  "format": "docx",
  "includeReferences": true,
  "citationStyle": "APA"
}
```

## 4. 关键响应契约

### MaterialResponse 追加 v1.5 解析质量

`GET /workspaces/{id}/materials` 的每个材料包含：

```json
{
  "id": "uuid",
  "filename": "论文共写平台测试.md",
  "parseStage": "AI_PARSED",
  "effectiveMaterialCategory": "REFERENCE_MATERIAL",
  "summary": "材料摘要",
  "bibliographicMetadata": {
    "authors": ["Smith, J."],
    "year": "2024",
    "title": "AI-supported writing",
    "venue": "Journal",
    "publisher": "Example Press",
    "link": "https://example.com"
  },
  "parseQuality": {
    "status": "READY",
    "score": 0.86,
    "nextAction": "当前解析结果可用于后续材料检查。",
    "completeness": {
      "summary": true,
      "topicRelation": true,
      "category": true,
      "claims": true,
      "evidence": true,
      "requirements": false,
      "bibliographicMetadata": true
    },
    "issues": []
  }
}
```

### MaterialPreviewResponse

```json
{
  "id": "uuid",
  "filename": "论文共写平台测试.md",
  "fileType": "md",
  "previewType": "text",
  "previewText": "材料预览内容...",
  "downloadUrl": null,
  "externalLink": null
}
```

### EvidenceBindingSummaryResponse

```json
{
  "draftVersionId": "uuid",
  "paragraphs": [
    {
      "paragraphId": "p1",
      "paragraphText": "正文段落...",
      "bindingStatus": "WEAK",
      "bindings": [
        {
          "id": "uuid",
          "materialId": "uuid",
          "knowledgeChunkId": "uuid",
          "materialTitle": "参考资料",
          "claimText": "正文观点",
          "sourceExcerpt": "原始证据片段",
          "sourceLocation": {
            "page": 3,
            "hint": "用户补充说明",
            "previewUrl": "/api/v1/materials/uuid/preview"
          },
          "confidenceScore": 0.72,
          "supportType": "DIRECT",
          "bindingStatus": "WEAK",
          "citationText": "(Smith, 2024)"
        }
      ]
    }
  ],
  "missingParagraphIds": ["p3"],
  "usedMaterials": [],
  "unusedMaterials": [],
  "coverage": {
    "totalParagraphs": 5,
    "confirmedParagraphs": 3,
    "weakParagraphs": 1,
    "missingParagraphs": 1,
    "coverageRatio": 80,
    "confirmedRatio": 60,
    "healthLabel": "整体可用，仍建议补强弱绑定段落。",
    "recommendations": ["优先补充缺来源段落。"]
  },
  "citationConsistency": {
    "status": "NEEDS_REVIEW",
    "detectedCitationCount": 4,
    "linkedMaterialCount": 3,
    "missingCitationParagraphCount": 1,
    "orphanCitationCount": 1,
    "incompleteReferenceCount": 0,
    "issues": ["正文引用数量多于可信链已绑定材料，建议人工核对。"]
  }
}
```

### CoWritePreviewResponse

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "draftVersionId": "uuid",
  "action": "rewrite_selection",
  "candidateDraftText": "AI 修改后的候选正文",
  "candidateSourceTraceMap": {},
  "diffSummary": {
    "paragraphDiffs": [
      {
        "paragraphId": "p1",
        "changeType": "modified",
        "originalText": "原段落",
        "candidateText": "AI 修改后的段落",
        "intentLabel": "补充论证",
        "selectedByDefault": true
      }
    ],
    "conflictWarnings": [
      {
        "code": "NO_MAJOR_CONFLICT",
        "title": "未发现明显冲突",
        "message": "引用、数字和来源数量未发现明显异常。",
        "level": "LOW"
      }
    ],
    "recheckSuggestion": {
      "shouldRecheck": true,
      "reviewItemCount": 1,
      "relatedReviewItems": [
        {
          "reviewItemId": "uuid",
          "relationType": "TARGET_RANGE_OVERLAP",
          "reason": "本次共写范围与该审查项定位范围重叠。"
        }
      ]
    }
  },
  "status": "READY",
  "createdAt": "2026-06-20T00:00:00Z",
  "appliedAt": null
}
```

### ReviewItemResponse 追加 v1.4 复查字段

```json
{
  "id": "uuid",
  "reviewType": "EVIDENCE_WEAK",
  "reviewImpactLevel": "MUST_CONFIRM",
  "message": "该段证据支撑偏弱",
  "suggestedFix": "补充更直接的材料来源",
  "reviewStatus": "OPEN",
  "lastRecheckedAt": "2026-06-20T00:00:00Z",
  "recheckNote": "复查结果：仍需补充证据",
  "recheckHistory": []
}
```

## 5. 状态约束

### 解析质量状态

| 状态 | 含义 | 前端处理 |
| --- | --- | --- |
| `READY` | 可用于生成 | 允许继续 |
| `NEEDS_CONFIRMATION` | 建议确认 | 允许继续但提示 |
| `NEEDS_SUPPLEMENT` | 需要补充 | 关键材料阻断 |
| `FAILED` | 解析失败 | 关键材料阻断 |

### 证据绑定状态

| 状态 | 含义 |
| --- | --- |
| `CONFIRMED` | 系统认为可信 |
| `WEAK` | 弱绑定，需要用户注意 |
| `MISSING` | 缺来源 |
| `USER_CONFIRMED` | 用户确认可信 |

### 审查项状态

| 状态 | 含义 |
| --- | --- |
| `OPEN` | 待处理 |
| `RESOLVED` | 已解决 |
| `IGNORED` | 用户忽略 |

### 复查结果

| 结果 | 处理 |
| --- | --- |
| `RESOLVED` | 自动关闭审查项 |
| `STILL_OPEN` | 保持待处理 |
| `DOWNGRADED` | 降低影响等级 |
| `NEEDS_MORE_EVIDENCE` | 保持待处理并提示补证据 |

## 6. 错误处理约定

| HTTP 状态 | 使用场景 |
| --- | --- |
| `400` | 参数错误、状态不允许 |
| `404` | 资源不存在 |
| `409` | 状态冲突，例如材料不足却生成 |
| `500` | 服务端异常 |
| `502` | 外部 AI 网关调用失败 |

前端提示原则：

- AI 调用失败：提示“AI 服务调用失败，请稍后重试”，不做本地伪解释。
- 材料不足：提示“根据目前内容无法生成”，并列出建议补充内容和数量。
- 解析质量不足：提示具体问题，并允许一键填入补充说明。

## 7. 当前不包含的契约

以下能力不是当前 API 已实现契约：

- 独立的旧版“改写 / 补证据 / 降重复”草稿端点；当前统一走 `co-write` 与 `co-write/preview`。
- 用户登录、注册、刷新令牌。
- 多用户权限隔离。
- 生产级异步队列回调。
