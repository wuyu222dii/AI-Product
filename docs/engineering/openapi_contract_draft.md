# AI 论文共写工作台 v1 OpenAPI 风格后端接口契约补完稿

## 1. 文档目标

本文档用于在已有接口字段说明基础上，补齐工程实现层真正需要的接口契约约束。  
重点补齐以下内容：

- 统一响应协议
- 鉴权与权限要求
- 字段校验规则
- 错误码规范
- 异步任务接口约束
- 幂等与时序限制

上位文档：

- [PRD.md](../product/PRD.md)
- [api_field_spec.md](api_field_spec.md)
- [backend_service_spec.md](backend_service_spec.md)

---

## 2. OpenAPI 风格约定

### 2.1 基础信息

- `Base URL`: `/api/v1`
- `Content-Type`: `application/json`
- 文件上传接口使用：`multipart/form-data`
- 时间字段：`ISO 8601 UTC`
- ID 字段：`UUID`

### 2.2 鉴权方式

默认所有接口都要求登录态，采用：

- `Authorization: Bearer <token>`

例外情况：

- 无公开接口
- 导出文件下载链接可使用短期签名 URL

### 2.3 权限规则

每个 `workspace` 都必须做归属校验：

- 只能访问自己的 `workspace`
- 只能访问属于该 workspace 的：
  - materials
  - drafts
  - review items
  - appeals
  - exports

若不满足权限：

- 返回 `403 FORBIDDEN`

---

## 3. 统一响应协议

## 3.1 成功响应

统一格式：

```json
{
  "success": true,
  "data": {},
  "meta": {
    "requestId": "uuid"
  }
}
```

说明：
- `success`: boolean
- `data`: 业务数据
- `meta.requestId`: 用于追踪日志和排查问题

## 3.2 列表响应

统一格式：

```json
{
  "success": true,
  "data": {
    "items": [],
    "pagination": {
      "page": 1,
      "pageSize": 20,
      "total": 100
    }
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

## 3.3 异步任务响应

统一格式：

```json
{
  "success": true,
  "data": {
    "jobId": "uuid",
    "status": "queued"
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

## 3.4 错误响应

统一格式：

```json
{
  "success": false,
  "error": {
    "code": "INSUFFICIENT_MATERIAL",
    "message": "根据当前上传内容，暂时无法生成正文框架或初稿。",
    "details": {
      "missingItems": [
        {
          "type": "reference_material",
          "message": "缺少 3-5 篇核心参考文献"
        }
      ]
    }
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

说明：
- `error.code`: 机器可读错误码
- `error.message`: 面向前端展示的简明说明
- `error.details`: 结构化错误上下文

---

## 4. HTTP 状态码约定

- `200 OK`
  成功读取、成功执行同步操作
- `201 CREATED`
  成功创建资源
- `202 ACCEPTED`
  异步任务已入队
- `400 BAD REQUEST`
  请求体不合法、参数校验失败
- `401 UNAUTHORIZED`
  未登录或 token 无效
- `403 FORBIDDEN`
  无权限访问资源
- `404 NOT FOUND`
  资源不存在
- `409 CONFLICT`
  资源状态冲突、版本冲突、幂等冲突
- `422 UNPROCESSABLE ENTITY`
  业务条件不满足，如材料不足、关键材料未解析完成
- `429 TOO MANY REQUESTS`
  频率限制
- `500 INTERNAL SERVER ERROR`
  服务内部错误
- `503 SERVICE UNAVAILABLE`
  AI 服务暂时不可用

---

## 5. 统一字段约束

## 5.1 Workspace 字段

- `title`
  - type: string
  - required: true
  - minLength: 1
  - maxLength: 120
  - trim: true

## 5.2 Material 上传字段

- `files[]`
  - type: binary[]
  - required: false
- `plainText`
  - type: string
  - required: false
  - maxLength: 50000
- `externalLink`
  - type: string
  - required: false
  - maxLength: 2000
  - format: uri
- `sourceType`
  - type: enum
  - required: true
  - allowed:
    - `upload`
    - `pasted_text`
    - `external_link`
- `isKeyMaterial`
  - type: boolean
  - required: false
  - default: false

约束：
- `files[] / plainText / externalLink` 至少传一个

## 5.3 Requirement Snapshot 字段

- `topic`
  - type: string
  - required: false
  - maxLength: 300
- `wordCount`
  - type: integer
  - required: false
  - minimum: 100
  - maximum: 50000
- `deadline`
  - type: string
  - required: false
  - format: date-time
- `citationStyle`
  - type: string
  - required: false
  - maxLength: 50
- `specialRequirements`
  - type: object
  - required: false

## 5.4 共写接口字段

- `action`
  - type: enum
  - required: true
  - allowed:
    - `rewrite_selection`
    - `add_evidence`
    - `adjust_structure`
    - `reduce_repetition`
    - `improve_expression`
- `targetRange`
  - type: object
  - required: false
  - fields:
    - `start`: integer >= 0
    - `end`: integer > start
- `instruction`
  - type: string
  - required: false
  - maxLength: 2000

## 5.5 Appeal 字段

- `userReason`
  - type: string
  - required: true
  - minLength: 1
  - maxLength: 2000
- `evidence`
  - type: object
  - required: false

---

## 6. 错误码字典

## 6.1 通用错误

- `INVALID_REQUEST_BODY`
- `INVALID_QUERY_PARAM`
- `UNAUTHORIZED`
- `FORBIDDEN`
- `RESOURCE_NOT_FOUND`
- `INTERNAL_ERROR`
- `RATE_LIMITED`

## 6.2 Workspace 错误

- `WORKSPACE_NOT_FOUND`
- `WORKSPACE_TITLE_INVALID`
- `WORKSPACE_STATUS_CONFLICT`

## 6.3 Material 错误

- `MATERIAL_NOT_FOUND`
- `MATERIAL_UPLOAD_EMPTY`
- `MATERIAL_FORMAT_UNSUPPORTED`
- `MATERIAL_PARSE_FAILED`
- `MATERIAL_PARSE_INCOMPLETE`
- `KEY_MATERIAL_NOT_PARSED`

## 6.4 Requirement 错误

- `REQUIREMENT_SNAPSHOT_MISSING`
- `REQUIREMENT_CONFLICT`
- `REQUIREMENT_INVALID`

## 6.5 生成错误

- `INSUFFICIENT_MATERIAL`
- `GENERATION_NOT_ELIGIBLE`
- `DRAFT_NOT_FOUND`
- `AI_SERVICE_UNAVAILABLE`
- `GENERATION_FAILED`
- `PARTIAL_GENERATION_ONLY`

## 6.6 审查与申诉错误

- `REVIEW_ITEM_NOT_FOUND`
- `APPEAL_NOT_FOUND`
- `APPEAL_ALREADY_RESOLVED`

## 6.7 导出错误

- `EXPORT_FAILED`
- `EXPORT_FORMAT_UNSUPPORTED`

---

## 7. 幂等与调用约束

## 7.1 幂等要求

以下接口建议支持幂等：

- `POST /workspaces`
  - 可通过前端传 `Idempotency-Key`
- `POST /workspaces/{id}/materials`
  - 同一提交批次建议带 `Idempotency-Key`
- `POST /workspaces/{id}/generate-draft`
  - 同一版本上下文下重复请求不应重复生成多份相同草稿
- `POST /drafts/{id}/export`
  - 同一参数下重复调用可复用已有导出任务

## 7.2 不可幂等接口

- `POST /review-items/{id}/appeal`
  - 默认一个审查项允许多次申诉历史记录，但同一未关闭申诉不能重复创建

## 7.3 状态前置约束

### 生成草稿前
必须满足：
- 关键材料已 `ai_parsed`
- Requirement Snapshot 存在
- 材料充足性检查通过

否则返回：
- `422`
- `GENERATION_NOT_ELIGIBLE` 或相关业务错误

### 共写前
必须满足：
- 对应 `draftVersion` 存在
- draft 状态可编辑

### 导出前
必须满足：
- draft 存在
- draft 不是空文本

---

## 8. 异步任务接口约束

## 8.1 统一 job 查询接口

建议补充：

- `GET /jobs/{id}`

响应字段：

```json
{
  "success": true,
  "data": {
    "id": "uuid",
    "workspaceId": "uuid",
    "jobType": "draft_generate",
    "status": "running",
    "progress": 60,
    "inputRef": {},
    "outputRef": {},
    "errorMessage": null,
    "createdAt": "2026-05-28T08:00:00Z",
    "updatedAt": "2026-05-28T08:01:00Z"
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

字段说明：
- `progress`: integer, 0-100
- `status`: `queued | running | success | partial | failed`

## 8.2 前端轮询建议

- 解析类任务：`2s - 3s`
- 生成类任务：`2s`
- 导出类任务：`3s`

停止轮询条件：
- `status = success`
- `status = partial`
- `status = failed`

## 8.3 partial 约束

当任务结果为 `partial`：
- 前端必须明确展示“部分完成”
- 后端不得将其视为 `success`
- 返回结果中必须说明哪些部分已完成、哪些未完成

建议 `error.details` 或 `outputRef` 中包含：

```json
{
  "completedParts": ["titleSuggestion", "outline"],
  "missingParts": ["draftText"]
}
```

---

## 9. OpenAPI 风格接口补完

## 9.1 `POST /api/v1/workspaces`

鉴权：
- Bearer Token

请求头：
- `Authorization`
- `Idempotency-Key` 可选

请求体：

```json
{
  "title": "人工智能对大学生学习方式的影响"
}
```

成功响应：
- `201 CREATED`

错误响应：
- `400 INVALID_REQUEST_BODY`
- `401 UNAUTHORIZED`

---

## 9.2 `POST /api/v1/workspaces/{id}/materials`

鉴权：
- Bearer Token

请求类型：
- `multipart/form-data`

请求字段：
- `files[]`
- `plainText`
- `externalLink`
- `sourceType`
- `isKeyMaterial`

成功响应：
- `201 CREATED`

错误响应：
- `400 MATERIAL_UPLOAD_EMPTY`
- `400 MATERIAL_FORMAT_UNSUPPORTED`
- `401 UNAUTHORIZED`
- `403 FORBIDDEN`
- `404 WORKSPACE_NOT_FOUND`

---

## 9.3 `POST /api/v1/materials/{id}/ai-parse`

鉴权：
- Bearer Token

请求体：

```json
{
  "forceRetry": false
}
```

成功响应：
- `202 ACCEPTED`

错误响应：
- `401 UNAUTHORIZED`
- `403 FORBIDDEN`
- `404 MATERIAL_NOT_FOUND`

---

## 9.4 `POST /api/v1/workspaces/{id}/material-sufficiency-check`

鉴权：
- Bearer Token

请求体：

```json
{
  "requirementSnapshotId": "uuid"
}
```

成功响应：
- `200 OK`

错误响应：
- `404 WORKSPACE_NOT_FOUND`
- `404 REQUIREMENT_SNAPSHOT_MISSING`
- `422 KEY_MATERIAL_NOT_PARSED`

---

## 9.5 `POST /api/v1/workspaces/{id}/generate-draft`

鉴权：
- Bearer Token

请求头：
- `Idempotency-Key` 推荐

请求体：

```json
{
  "requirementSnapshotId": "uuid",
  "mode": "default"
}
```

成功响应：
- `202 ACCEPTED`

错误响应：
- `404 WORKSPACE_NOT_FOUND`
- `404 REQUIREMENT_SNAPSHOT_MISSING`
- `422 INSUFFICIENT_MATERIAL`
- `422 KEY_MATERIAL_NOT_PARSED`
- `503 AI_SERVICE_UNAVAILABLE`

---

## 9.6 `POST /api/v1/workspaces/{id}/co-write`

鉴权：
- Bearer Token

请求体：

```json
{
  "draftVersionId": "uuid",
  "action": "rewrite_selection",
  "targetRange": {
    "start": 120,
    "end": 260
  },
  "instruction": "请把这一段写得更具体，并补充来源。"
}
```

成功响应：
- `202 ACCEPTED`

错误响应：
- `404 DRAFT_NOT_FOUND`
- `409 WORKSPACE_STATUS_CONFLICT`
- `503 AI_SERVICE_UNAVAILABLE`

---

## 9.7 `GET /api/v1/drafts/{id}/review-items`

鉴权：
- Bearer Token

成功响应：
- `200 OK`

错误响应：
- `404 DRAFT_NOT_FOUND`

---

## 9.8 `POST /api/v1/review-items/{id}/appeal`

鉴权：
- Bearer Token

请求体：

```json
{
  "userReason": "这段内容已在课程讲义第2页给出支持。",
  "evidence": {
    "materialIds": ["uuid-1"],
    "note": "请查看课程讲义第2页"
  }
}
```

成功响应：
- `201 CREATED`

错误响应：
- `404 REVIEW_ITEM_NOT_FOUND`
- `409 APPEAL_ALREADY_RESOLVED`

---

## 9.9 `POST /api/v1/drafts/{id}/export`

鉴权：
- Bearer Token

请求头：
- `Idempotency-Key` 推荐

请求体：

```json
{
  "format": "docx",
  "includeComments": false
}
```

成功响应：
- `202 ACCEPTED`

错误响应：
- `404 DRAFT_NOT_FOUND`
- `400 EXPORT_FORMAT_UNSUPPORTED`
- `503 AI_SERVICE_UNAVAILABLE`

---

## 10. 推荐后续动作

如果要把这份契约进一步变成真正可导入工具链的文件，下一步建议：

1. 把本文转成 `openapi.yaml`
2. 补齐：
   - `components.schemas`
   - `components.responses`
   - `components.parameters`
   - `securitySchemes`
3. 用它生成：
   - 前端类型
   - 后端接口 stub
   - API Mock
