# AI 论文共写工作台 v2.0.1

当前项目基线：`v1.x MVP 能力完整保留 + v2.0 全学术人群升级 + v2.0.1 学术文档统一工作台首轮收口`。

产品定位：`面向本科生、硕士生、博士生与科研人员的证据驱动型研究共创平台`。AI 可以基于真实材料生成和修改学术文档，但必须保留材料范围、来源追溯、章节版本、人工确认和 AI 使用记录；产品不承诺规避 AI 检测或论文查重。

当前正式状态以 [PRODUCT_COMPLETION_STATUS-6-16.md](docs/project/PRODUCT_COMPLETION_STATUS-6-16.md) 为准，产品边界见 [V2_ACADEMIC_UPGRADE.md](docs/product/V2_ACADEMIC_UPGRADE.md)，最新接口与统一工作台契约见 [v2_academic_workspace_api.md](docs/engineering/v2_academic_workspace_api.md)。

## v2.0.1 已完成

- 学术项目画像：本科、硕士、博士、科研人员；学科方向、研究范式、语言、引用格式和 AI 使用策略。
- 多文档研究项目：同一项目共享材料和知识库，可创建开题、学位论文、期刊稿、综述、研究报告等独立文档。
- 章节树写作：支持拖拽整理章节顺序，以及章节级正文、状态、目标篇幅、版本恢复、AI 生成、共写预览、组装与导出。
- 动态 readiness：按文档类型、研究范式和章节类型判断材料门槛；研究计划不要求已有结果，系统综述不要求实验数据。
- 文档材料隔离：每个文档可选择自己的材料集，知识检索、章节生成和共写不会串用其他文档材料。
- 通用学术要求：要求优先级为 `用户确认的学校/导师/课程/期刊要求 > 文档设置 > 研究范式规则 > 平台默认`。
- AI 使用留痕：记录操作类型、目标章节、材料依据、模型、采纳状态与披露要求。
- 统一正文源：`DocumentSection` 是唯一可编辑正文；整篇视图只负责组装预览、全篇检查、审查和导出。
- 统一分析作用域：`ContentScope` 同时支持章节、整篇文档和旧草稿，可信链、原创风险、审查与复查不再绑定单一 draft 模型。
- 章节可信审查闭环：章节保存后异步重建可信链，本地风险实时派生；AI 深度审查由用户手动发起，旧结果会按章节版本标记为 `STALE`。
- 可控章节共写：支持选区共写、整版/逐段/差异行应用、引用与数据冲突提示；基准版本变化时返回 `409`，不会覆盖用户新修改。
- 整篇只读交付：聚合章节准备度、证据覆盖率、原创实证风险、审查状态和引用一致性，并直接按章节组装导出，不创建可编辑旧 draft。
- 旧入口退场：导航已隐藏“整篇共写（兼容）”；旧 `/workspace`、`/export` 路由和旧 API 仅保留历史回归兼容。
- 旧稿可选拆分：可识别标题并预览，用户确认后才生成章节，拆分前正文保留在版本历史。
- Supabase 正式迁移：v2.0 基础迁移、章节版本修复和 v2.0.1 作用域迁移均已在真实数据库执行。

## 保留能力

v1.1-v1.9 的上传与 OCR、AI 语义解析、解析质量清单、材料不足文献检索、项目知识库、材料可信链、原创实证补强、审查复查和 DOCX/PDF 导出继续可用。旧整篇共写路由和接口保留兼容，但不再作为默认产品入口。

## 当前主流程

```text
创建研究项目与学术画像
-> 上传并 AI 解析真实材料
-> 按当前文档执行动态 readiness
-> 材料不足时检索和补传真实文献/研究成果
-> 构建项目知识库
-> 创建或切换学术文档
-> 按章节生成、编辑和共写预览
-> 选择性应用为章节新版本
-> 查看本章可信链、原创风险、审查与复查
-> 在整篇检查中聚合质量问题并跳回对应章节
-> 按章节组装并导出
```

旧版“整篇初稿 -> 整篇共写”流程只保留直接路由和 API 兼容，导航已隐藏。学术文档工作台是唯一主入口，整篇内容不可直接编辑。

## 目录

| 路径 | 用途 |
| --- | --- |
| [backend](backend) | Spring Boot 3 / Java 17 REST 后端 |
| [frontend](frontend) | React / Vite 多页面前端 |
| [supabase/migrations/20260711031857_v2_academic_workspace.sql](supabase/migrations/20260711031857_v2_academic_workspace.sql) | v2.0 正式 Supabase 迁移 |
| [supabase/migrations/20260711065932_repair_document_section_versions.sql](supabase/migrations/20260711065932_repair_document_section_versions.sql) | 章节历史版本一致性修复迁移 |
| [supabase/migrations/20260711092831_academic_document_quality_scopes.sql](supabase/migrations/20260711092831_academic_document_quality_scopes.sql) | v2.0.1 章节/文档可信链、审查和选择性共写作用域迁移 |
| [docs/README.md](docs/README.md) | 文档总索引 |
| [docs/product/PRD.md](docs/product/PRD.md) | 产品总规格与历史兼容规格 |
| [docs/product/V2_ACADEMIC_UPGRADE.md](docs/product/V2_ACADEMIC_UPGRADE.md) | v2.0 当前产品规格 |
| [docs/engineering/v2_academic_workspace_api.md](docs/engineering/v2_academic_workspace_api.md) | v2.0 数据与接口契约 |
| [docs/guides/DEMO_GUIDE.md](docs/guides/DEMO_GUIDE.md) | 启动与演示说明 |
| [docs/project/FINAL_DELIVERY_CHECKLIST.md](docs/project/FINAL_DELIVERY_CHECKLIST.md) | 当前交付清单 |

## 启动

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

默认地址：后端 `http://localhost:8080`，前端 `http://localhost:5173`。可通过 `VITE_API_PROXY_TARGET` 覆盖前端代理目标。

数据库结构只通过版本化 SQL 迁移维护，运行时默认 `HIBERNATE_DDL_AUTO=none`，避免 Hibernate 与 PostgreSQL 生成列或索引冲突。

## 验证

```bash
cd backend && mvn test
cd frontend && npm run test
cd frontend && npm run test:e2e
```

当前验证结果：

- 后端 `78` 个 service / controller 测试通过，无失败或错误。
- 前端生产构建与 smoke test 通过。
- 前端 Playwright E2E `2` 个 Chrome 用户流程通过，覆盖统一学术文档工作台、章节共写、审查复查与导出。
- `npm audit` 与 `npm audit --omit=dev` 均为 `0 vulnerabilities`。
- 真实 Supabase 已完成 v2.0.1 迁移和 API 冒烟：章节可信链异步重建、整篇质量聚合、AI 审查与单项复查、基准版本冲突、逐段应用、章节组装和 DOCX 导出均通过。

## 当前边界

- 尚未实现登录、租户隔离、协作者权限和导师批注。
- 尚未接入对象存储、生产级任务队列和监控告警；前端组件级测试与更多异常路径 E2E 仍可继续扩充。
- v2.0 使用 PostgreSQL 全文检索、元数据和结构化筛选；混合向量检索留到 v2.1 以后。
- CSL、RIS/BibTeX、LaTeX 和正式学位论文模板仍属于下一阶段。
