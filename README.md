# AI 论文共写工作台 v1 研发交付目录

当前项目基线：`MVP 阶段 100% 收口 + v1.6 可信交付增强版`。

本项目面向本科课程论文场景，核心定位是 `AI 强辅助共写平台`：用户上传作业要求、研究成果、参考资料、图片或文件后，系统完成 AI 语义解析、材料充足性检查、初稿生成、可信链追溯、可控共写、审查复查与导出。

当前正式状态以 [PRODUCT_COMPLETION_STATUS-6-16.md](docs/project/PRODUCT_COMPLETION_STATUS-6-16.md) 为准；交付检查以 [FINAL_DELIVERY_CHECKLIST.md](docs/project/FINAL_DELIVERY_CHECKLIST.md) 为准。

## 当前能力

- `v1.1` 文件解析补全：支持 PDF / DOCX / TXT / Markdown / 图片 / XLSX / CSV / PPTX / ZIP 等上传输入，并支持解析失败后的补充说明。
- `v1.2` 引用与格式增强：解析参考资料作者、年份、题名、期刊/出版社、链接，并支持 APA / GB/T 7714 格式切换。
- `v1.3` 项目知识库 MVP：把已完成 AI 解析的材料转成项目级证据片段，支持关键词检索和片段预览。
- `v1.4` 可信链与共写闭环：支持段落级证据绑定、异步可信链重建、共写预览后应用、审查项手动复查。
- `v1.5` AI 解析质量清单：材料响应新增 `parseQuality`，前端解析页展示质量徽标、问题清单、一键填入补充说明和关键材料阻断规则。
- `v1.6` 可信交付增强：支持原始材料预览入口、可信链覆盖率评分、引用一致性检查、共写逐段接受、冲突提示、共写与审查项关联落库，以及导出前交付风险检查。

## 主流程

```text
创建项目
-> 上传写作输入
-> 预处理
-> AI 语义解析
-> 解析质量检查
-> 材料充足性检查
-> 生成初稿
-> 项目知识库 / 材料可信链
-> 共写预览后应用
-> 审查与手动复查
-> 导出 docx / pdf
```

## 文档总览

| 文档 | 用途 |
| --- | --- |
| [docs/README.md](docs/README.md) | 全部文档索引 |
| [PRD.md](docs/product/PRD.md) | 产品与开发总规格 |
| [DEMO_GUIDE.md](docs/guides/DEMO_GUIDE.md) | Demo 启动、演示路径与排查 |
| [FINAL_DELIVERY_CHECKLIST.md](docs/project/FINAL_DELIVERY_CHECKLIST.md) | 当前交付检查清单 |
| [PRODUCT_COMPLETION_STATUS-6-16.md](docs/project/PRODUCT_COMPLETION_STATUS-6-16.md) | 当前产品完成度快照 |
| [frontend_page_spec.md](docs/design/frontend_page_spec.md) | 前端页面字段与组件树 |
| [backend_service_spec.md](docs/engineering/backend_service_spec.md) | 后端服务拆分与状态机 |
| [api_field_spec.md](docs/engineering/api_field_spec.md) | 当前实现的接口字段定义 |
| [openapi_contract_draft.md](docs/engineering/openapi_contract_draft.md) | OpenAPI 风格接口契约 |
| [engineering_tasks.md](docs/engineering/engineering_tasks.md) | 前后端任务拆分 |
| [postgresql_schema.sql](postgresql_schema.sql) | PostgreSQL 基础建表草案 |
| [backend/db/v1_4_trust_chain.sql](backend/db/v1_4_trust_chain.sql) | v1.4 可信链迁移 |
| [backend/db/v1_6_trust_delivery_enhancement.sql](backend/db/v1_6_trust_delivery_enhancement.sql) | v1.6 共写审查关联迁移 |

## 启动方式

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

## 验证方式

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

当前验证口径：

- 后端 `49` 个 service / controller 测试通过。
- 前端 `npm run test` 通过，包含生产构建与 MVP smoke check。

## 当前边界

以下内容不属于当前 MVP 已完成范围，应作为后续产品化增强处理：

- 用户登录、租户隔离、权限体系。
- 生产级对象存储。
- 生产级异步队列、失败重试和监控告警。
- Playwright / Cypress 端到端测试。
- PDF 页码级精准跳转、更细粒度证据可视化和正式论文级导出排版。

## 推荐阅读顺序

- 产品 / 设计：`PRD -> frontend_page_spec -> DEMO_GUIDE`
- 前端：`frontend_page_spec -> api_field_spec -> openapi_contract_draft`
- 后端：`backend_service_spec -> api_field_spec -> postgresql_schema.sql -> v1_4_trust_chain.sql -> v1_6_trust_delivery_enhancement.sql`
- 交付 / 演示：`FINAL_DELIVERY_CHECKLIST -> DEMO_GUIDE -> PRODUCT_COMPLETION_STATUS`
