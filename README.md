# AI 论文共写工作台 v1 研发交付目录

## 1. 项目说明

本目录用于沉淀 `AI 论文共写工作台 v1` 的研发交付文档。  
当前所有文件均以同一版产品目标为基线：

- 面向 `本科课程论文`
- AI 角色是 `创作者 / 共写者`
- 用户上传 `待解析的写作输入`
- 系统在 `AI 语义解析 + 材料充足性检查` 通过后生成正文框架与初稿
- v1.3 新增 `项目知识库 MVP`：把已解析材料转成可检索证据片段
- 推荐任务与审查是 `可选辅助层`
- 材料不足时 `禁止兜底生成`

这套文档可以直接用于：
- 产品评审
- 技术方案评审
- 前后端拆活
- AI 编排实现
- 数据库建模

> 所有 Markdown 文档已归类至 [docs/](docs/README.md) 目录，下文链接均指向新路径。

---

## 2. 文档总览

### [DEMO_GUIDE.md](docs/guides/DEMO_GUIDE.md)
面向演示与体验验证的总使用说明。  
适合产品演示、路演、内部汇报或交给他人快速跑通 Demo。

内容包括：
- 启动方式
- 演示路径
- 页面重点
- 已接真能力说明
- 常见问题排查

### [PRD.md](docs/product/PRD.md)
产品与开发总规格文档。  
适合产品、设计、前后端、AI 工程共同对齐。

内容包括：
- 产品目标与边界
- 核心流程
- 页面规格
- 推荐任务与审查机制
- 材料充足性规则
- 生成与共写规则
- 数据对象与状态定义

### [frontend_page_spec.md](docs/design/frontend_page_spec.md)
前端页面字段清单与组件树。  
适合前端、设计、产品联动使用。

内容包括：
- 各页面字段清单
- 页面组件树
- 全局共享组件
- 状态管理建议
- 前端联调重点

### [backend_service_spec.md](docs/engineering/backend_service_spec.md)
后端服务拆分与状态机时序图。  
适合后端、架构、AI 平台工程使用。

内容包括：
- 服务边界
- 核心状态机
- 关键时序图
- 异步任务编排
- 失败兜底与恢复机制

### [api_field_spec.md](docs/engineering/api_field_spec.md)
接口字段级定义。  
适合前后端联调与后端 API 实现。

内容包括：
- 请求 / 响应字段
- 字段类型与说明
- 状态字段约束
- 错误码建议

### [postgresql_schema.sql](postgresql_schema.sql)
PostgreSQL 建表 SQL 草案。  
适合后端、数据库工程、架构评审。

内容包括：
- 枚举定义
- 主表结构
- 外键关系
- 索引建议

### [engineering_tasks.md](docs/engineering/engineering_tasks.md)
前后端任务拆分清单。  
适合研发排期、任务分派、项目管理。

内容包括：
- 前端任务拆分
- 后端任务拆分
- 联调顺序
- 角色分工建议

### 更多文档

完整索引见 [docs/README.md](docs/README.md)，包括设计评审、视觉规范、交付清单、样例材料等。

---

## 3. 推荐阅读顺序

### 对产品 / 设计
1. [PRD.md](docs/product/PRD.md)
2. [frontend_page_spec.md](docs/design/frontend_page_spec.md)
3. [engineering_tasks.md](docs/engineering/engineering_tasks.md)

### 对前端
1. [PRD.md](docs/product/PRD.md)
2. [frontend_page_spec.md](docs/design/frontend_page_spec.md)
3. [api_field_spec.md](docs/engineering/api_field_spec.md)
4. [engineering_tasks.md](docs/engineering/engineering_tasks.md)

### 对后端
1. [PRD.md](docs/product/PRD.md)
2. [backend_service_spec.md](docs/engineering/backend_service_spec.md)
3. [api_field_spec.md](docs/engineering/api_field_spec.md)
4. [postgresql_schema.sql](postgresql_schema.sql)
5. [engineering_tasks.md](docs/engineering/engineering_tasks.md)

### 对 AI / 编排工程
1. [PRD.md](docs/product/PRD.md)
2. [backend_service_spec.md](docs/engineering/backend_service_spec.md)
3. [api_field_spec.md](docs/engineering/api_field_spec.md)

---

## 4. 当前已锁定的开发基线

### 主路径
`上传待解析的写作输入 -> 本地预处理 -> AI 语义解析 -> 材料充足性检查并生成初稿 -> 项目知识库 -> 用户修改 -> AI 继续共写 -> 导出`

### 关键约束
- 关键材料未完成 `AI 语义解析`，不得生成
- 材料不足时，必须 `阻断生成`
- AI 失败时，必须 `明确报错 + 保留内容 + 支持重试`
- 知识库只纳入 `已完成 AI 解析` 的材料，避免未解析内容污染证据链
- 审查是辅助层，不是唯一裁决
- 会影响准确性或引发大范围返工的问题，升级为 `必须确认`

### v1.3 知识库 MVP
- 后端新增 `knowledge_chunks` 表，用于保存项目级材料片段、关键词和来源信息。
- 后端新增接口：`POST /api/v1/workspaces/{id}/knowledge-base/build`、`GET /api/v1/workspaces/{id}/knowledge-base/chunks`、`POST /api/v1/workspaces/{id}/knowledge-base/search`。
- 前端新增 `知识库` 页面，可构建/重建知识库、预览入库片段，并按问题检索相关证据。
- 当前知识库检索采用关键词匹配，不依赖额外检索网关，降低演示和开发环境的不稳定性。

### 审查分级
- `仅提示`
- `局部修正`
- `必须确认`

---

## 5. 建议开发顺序

1. 上传与材料入库
2. 本地预处理
3. AI 语义解析
4. Requirement Snapshot
5. 材料充足性检查
6. 项目知识库
7. 初稿生成
8. 共写工作台
9. 审查与推荐任务
10. 申诉复审
11. 导出
12. 错误兜底与监控

---

## 6. 交付使用建议

### 用于启动开发时
- 先阅读 [PRD.md](docs/product/PRD.md)
- 再按角色阅读对应专项文档
- 最后用 [engineering_tasks.md](docs/engineering/engineering_tasks.md) 拆活

### 用于方案评审时
- 产品评审重点看：`PRD + 前端页面规格`
- 技术评审重点看：`后端服务规格 + API + SQL`

### 用于联调时
- 以前后端共同确认 [api_field_spec.md](docs/engineering/api_field_spec.md) 为准
- 以前端确认页面字段和状态展示，以 [frontend_page_spec.md](docs/design/frontend_page_spec.md) 为准
- 以后端确认状态流转与异步链路，以 [backend_service_spec.md](docs/engineering/backend_service_spec.md) 为准

---

## 7. 文档目录结构

```
docs/
├── README.md              # 文档索引
├── product/               # 产品规格
├── design/                # 页面与视觉设计
├── engineering/           # 接口、服务、任务拆分
├── guides/                # Demo 与部署指南
├── project/               # 进度与交付
├── personal/              # 个人材料
└── samples/               # Demo 样例上传材料
```

根目录保留 [README.md](README.md)；模块说明见 [frontend/README.md](frontend/README.md)。

---

## 8. 下一步建议

如果继续往实现推进，建议优先补以下内容之一：

1. `前端页面原型 / 低保真线框图`
2. `后端任务状态机字段与错误码常量`
3. `AI Prompt 与工作流编排文档`
4. `MVP 迭代排期表`
