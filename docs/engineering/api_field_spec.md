# AI 论文共写工作台 v1 接口字段级定义

## 1. 设计约定

- 所有响应默认使用 `application/json`
- 时间字段统一使用 `ISO 8601 UTC`
- 主键统一使用 `UUID`
- 列表接口默认返回 `items` + `pagination`
- 异步任务接口默认返回 `jobId`

---

## 2. Workspace 接口

### 2.1 `POST /workspaces`

请求体：

```json
{
  "title": "人工智能对大学生学习方式的影响"
}
```

字段说明：
- `title`: string, 必填, 项目标题

响应体：

```json
{
  "id": "uuid",
  "title": "人工智能对大学生学习方式的影响",
  "status": "draft",
  "createdAt": "2026-05-28T08:00:00Z",
  "updatedAt": "2026-05-28T08:00:00Z"
}
```

### 2.2 `GET /workspaces`

查询参数：
- `page`: integer, 可选
- `pageSize`: integer, 可选

响应体：

```json
{
  "items": [
    {
      "id": "uuid",
      "title": "项目标题",
      "status": "ready",
      "currentDraftVersionId": "uuid",
      "updatedAt": "2026-05-28T08:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "pageSize": 20,
    "total": 1
  }
}
```

### 2.3 `GET /workspaces/{id}`

响应体：

```json
{
  "id": "uuid",
  "title": "项目标题",
  "status": "processing",
  "currentDraftVersionId": "uuid",
  "requirementSnapshotId": "uuid",
  "createdAt": "2026-05-28T08:00:00Z",
  "updatedAt": "2026-05-28T08:10:00Z"
}
```

---

## 3. Materials 接口

### 3.1 `POST /workspaces/{id}/materials`

表单字段：
- `files[]`: binary[], 可选
- `plainText`: string, 可选
- `externalLink`: string, 可选
- `sourceType`: string, 必填
- `isKeyMaterial`: boolean, 可选

说明：
- 三者 `files[] / plainText / externalLink` 至少传一个
- `sourceType` 示例：
  - `upload`
  - `pasted_text`
  - `external_link`

响应体：

```json
{
  "items": [
    {
      "id": "uuid",
      "filename": "assignment.pdf",
      "fileType": "pdf",
      "sourceType": "upload",
      "isKeyMaterial": true,
      "parseStage": "preprocessed",
      "createdAt": "2026-05-28T08:00:00Z"
    }
  ]
}
```

### 3.2 `GET /workspaces/{id}/materials`

响应体：

```json
{
  "items": [
    {
      "id": "uuid",
      "filename": "assignment.pdf",
      "fileType": "pdf",
      "sourceType": "upload",
      "isKeyMaterial": true,
      "parseStage": "ai_parsed",
      "confidenceScore": 0.94,
      "createdAt": "2026-05-28T08:00:00Z"
    }
  ]
}
```

### 3.3 `POST /materials/{id}/preprocess`

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

### 3.4 `POST /materials/{id}/ai-parse`

请求体：

```json
{
  "forceRetry": false
}
```

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

### 3.5 `POST /materials/{id}/supplement`

请求体：

```json
{
  "supplementText": "第3页图表中显示样本量为200",
  "pageRef": 3
}
```

字段说明：
- `supplementText`: string, 必填
- `pageRef`: integer, 可选

响应体：

```json
{
  "id": "uuid",
  "materialId": "uuid",
  "accepted": true
}
```

---

## 4. Requirement Snapshot 接口

### 4.1 `POST /workspaces/{id}/requirement-snapshot`

请求体：

```json
{
  "topic": "人工智能对大学生学习方式的影响",
  "wordCount": 3000,
  "deadline": "2026-06-10T12:00:00Z",
  "citationStyle": "APA",
  "specialRequirements": {
    "language": "zh-CN",
    "minReferences": 5
  }
}
```

响应体：

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "topic": "人工智能对大学生学习方式的影响",
  "wordCount": 3000,
  "deadline": "2026-06-10T12:00:00Z",
  "citationStyle": "APA",
  "specialRequirements": {
    "language": "zh-CN",
    "minReferences": 5
  },
  "version": 1,
  "createdAt": "2026-05-28T08:00:00Z"
}
```

### 4.2 `GET /workspaces/{id}/requirement-snapshot`

响应体同上。

---

## 5. Material Sufficiency Check 接口

### 5.1 `POST /workspaces/{id}/material-sufficiency-check`

请求体：

```json
{
  "requirementSnapshotId": "uuid"
}
```

响应体：

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "isGenerationEligible": false,
  "missingItems": [
    {
      "type": "reference_material",
      "message": "缺少可引用核心文献"
    }
  ],
  "recommendedSupplements": [
    {
      "type": "reference_material",
      "suggestedCount": "3-5",
      "message": "建议补充3-5篇核心参考文献"
    }
  ],
  "reason": "当前材料不足以支撑正文生成"
}
```

---

## 6. Draft 接口

### 6.1 `POST /workspaces/{id}/generate-draft`

请求体：

```json
{
  "requirementSnapshotId": "uuid",
  "mode": "default"
}
```

字段说明：
- `mode`: string, 可选, `default | quick`

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

### 6.2 `POST /workspaces/{id}/co-write`

请求体：

```json
{
  "draftVersionId": "uuid",
  "action": "rewrite_selection",
  "targetRange": {
    "start": 120,
    "end": 260
  },
  "instruction": "请使这段表达更具体，并补充证据支撑"
}
```

字段说明：
- `draftVersionId`: UUID, 必填
- `action`: string, 必填
  - `rewrite_selection`
  - `add_evidence`
  - `adjust_structure`
  - `reduce_repetition`
  - `improve_expression`
- `targetRange`: object, 可选
- `instruction`: string, 可选

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

### 6.3 `POST /drafts/{id}/rewrite`

请求体：

```json
{
  "targetRange": {
    "start": 120,
    "end": 260
  },
  "instruction": "更学术化，但不要空泛"
}
```

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

### 6.4 `POST /drafts/{id}/add-evidence`

请求体：

```json
{
  "targetRange": {
    "start": 300,
    "end": 420
  },
  "claim": "人工智能提升了学习效率"
}
```

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

### 6.5 `POST /drafts/{id}/reduce-repetition`

请求体：

```json
{
  "targetRange": {
    "start": 500,
    "end": 900
  }
}
```

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

---

## 7. Draft Version 查询结构

建议新增读取接口：

### 7.1 `GET /drafts/{id}`

响应体：

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "versionNo": 3,
  "titleSuggestion": "人工智能对大学生学习方式的影响研究",
  "outline": {
    "sections": [
      {
        "title": "引言",
        "purpose": "说明研究背景与问题"
      }
    ]
  },
  "paragraphSkeletons": [
    {
      "paragraphId": "p1",
      "goal": "说明研究背景",
      "evidenceHints": ["material-1", "material-3"]
    }
  ],
  "draftText": "正文全文...",
  "sourceTraceMap": {
    "p1": ["material-1"]
  },
  "generationStatus": "success",
  "createdBy": "system",
  "createdAt": "2026-05-28T08:20:00Z"
}
```

---

## 8. Review 接口

### 8.1 `GET /drafts/{id}/review-items`

响应体：

```json
{
  "items": [
    {
      "id": "uuid",
      "reviewType": "missing_evidence",
      "reviewImpactLevel": "local_fix",
      "targetRange": {
        "start": 320,
        "end": 360
      },
      "message": "该段关键论点缺少来源支撑",
      "suggestedFix": "请补充至少1条可引用来源",
      "canBypass": true,
      "createdAt": "2026-05-28T08:30:00Z"
    }
  ]
}
```

字段说明：
- `reviewType`: string
  - `missing_evidence`
  - `requirement_conflict`
  - `repetition_issue`
  - `logic_gap`
  - `factual_risk`
- `reviewImpactLevel`: `notice | local_fix | must_confirm`

---

## 9. Appeal 接口

### 9.1 `POST /review-items/{id}/appeal`

请求体：

```json
{
  "userReason": "这段内容已在课程讲义第2页给出支持，不应判定为缺来源",
  "evidence": {
    "materialIds": ["uuid-1"],
    "note": "请参考课程讲义第2页"
  }
}
```

响应体：

```json
{
  "id": "uuid",
  "reviewItemId": "uuid",
  "userReason": "这段内容已在课程讲义第2页给出支持，不应判定为缺来源",
  "reviewOutcome": null,
  "createdAt": "2026-05-28T08:40:00Z"
}
```

### 9.2 `GET /appeals/{id}`

响应体：

```json
{
  "id": "uuid",
  "reviewItemId": "uuid",
  "userReason": "申诉理由",
  "evidence": {
    "materialIds": ["uuid-1"]
  },
  "reviewOutcome": "downgraded_to_notice",
  "resolvedAt": "2026-05-28T08:50:00Z",
  "createdAt": "2026-05-28T08:40:00Z"
}
```

---

## 10. Export 接口

### 10.1 `POST /drafts/{id}/export`

请求体：

```json
{
  "format": "docx",
  "includeComments": false
}
```

字段说明：
- `format`: `docx | pdf`
- `includeComments`: boolean, 可选

响应体：

```json
{
  "jobId": "uuid",
  "status": "queued"
}
```

---

## 11. Generation Job 查询结构

建议新增读取接口：

### 11.1 `GET /jobs/{id}`

响应体：

```json
{
  "id": "uuid",
  "workspaceId": "uuid",
  "jobType": "draft_generate",
  "status": "running",
  "inputRef": {
    "requirementSnapshotId": "uuid"
  },
  "outputRef": {},
  "errorMessage": null,
  "createdAt": "2026-05-28T08:00:00Z",
  "updatedAt": "2026-05-28T08:01:00Z"
}
```

---

## 12. 错误码建议

- `WORKSPACE_NOT_FOUND`
- `MATERIAL_NOT_FOUND`
- `MATERIAL_PARSE_FAILED`
- `MATERIAL_PARSE_INCOMPLETE`
- `INSUFFICIENT_MATERIAL`
- `REQUIREMENT_SNAPSHOT_MISSING`
- `AI_SERVICE_UNAVAILABLE`
- `DRAFT_NOT_FOUND`
- `REVIEW_ITEM_NOT_FOUND`
- `APPEAL_NOT_FOUND`
- `EXPORT_FAILED`

---

## 13. 前后端联调重点

### 13.1 状态流转必须对齐
- `parseStage`
- `generationStatus`
- `reviewImpactLevel`

### 13.2 关键拦截点必须一致
- 材料不足不得生成
- 关键材料未完成 AI 解析不得生成
- AI 调用失败不得伪造成功结果

### 13.3 页面端必须能消费这些结构
- 缺失项列表
- 建议补充数量
- 审查定位范围
- 版本差异
- 复审结果
