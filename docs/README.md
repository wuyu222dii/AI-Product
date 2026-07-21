# 文档目录索引

本目录集中存放项目 Markdown 文档，按用途分类。根目录 [README.md](../README.md) 是项目总入口。

当前正式口径：

- 当前完成度（唯一当前状态来源）：[PRODUCT_COMPLETION_STATUS-6-16.md](project/PRODUCT_COMPLETION_STATUS-6-16.md)
- v2.0 产品规格：[V2_ACADEMIC_UPGRADE.md](product/V2_ACADEMIC_UPGRADE.md)
- v2.0.1 数据与接口：[v2_academic_workspace_api.md](engineering/v2_academic_workspace_api.md)
- v2.1 Auth 配置：[AUTH_SETUP.md](guides/AUTH_SETUP.md)
- 当前交付清单：[FINAL_DELIVERY_CHECKLIST.md](project/FINAL_DELIVERY_CHECKLIST.md)
- Demo 跑通指南：[DEMO_GUIDE.md](guides/DEMO_GUIDE.md)

历史规格文档用于解释设计与接口演进；当历史版本号、测试数量与当前完成度文档不一致时，以当前完成度文档为准。

## 产品规格

| 文档 | 说明 |
| --- | --- |
| [PRD.md](product/PRD.md) | 产品与开发总规格 |
| [V2_ACADEMIC_UPGRADE.md](product/V2_ACADEMIC_UPGRADE.md) | v2.0 全学术人群当前规格 |

## 设计与交互

| 文档 | 说明 |
| --- | --- |
| [frontend_page_spec.md](design/frontend_page_spec.md) | 前端页面字段与组件树 |
| [P7_workspace_design_review.md](design/P7_workspace_design_review.md) | 共写工作台设计评审 |
| [P7_workspace_visual_spec.md](design/P7_workspace_visual_spec.md) | 共写工作台视觉规范 |

## 工程与接口

| 文档 | 说明 |
| --- | --- |
| [backend_service_spec.md](engineering/backend_service_spec.md) | 后端服务与状态机 |
| [api_field_spec.md](engineering/api_field_spec.md) | 当前实现的接口字段定义 |
| [openapi_contract_draft.md](engineering/openapi_contract_draft.md) | OpenAPI 风格后端接口契约 |
| [engineering_tasks.md](engineering/engineering_tasks.md) | 前后端任务拆分 |
| [ai-cowriting-workbench-development-doc.md](engineering/ai-cowriting-workbench-development-doc.md) | 当前工程实现与版本演进汇总 |
| [v2_academic_workspace_api.md](engineering/v2_academic_workspace_api.md) | v2.0.1 学术画像、多文档、章节、统一可信审查与选择性共写接口 |
| [postgresql_schema.sql](../postgresql_schema.sql) | PostgreSQL 基础建表草案 |
| [v1_4_trust_chain.sql](../backend/db/v1_4_trust_chain.sql) | v1.4 可信链迁移 |
| [v1_6_trust_delivery_enhancement.sql](../backend/db/v1_6_trust_delivery_enhancement.sql) | v1.6 共写审查关联迁移 |
| [v1_9_literature_candidates.sql](../backend/db/v1_9_literature_candidates.sql) | v1.9 候选文献清单迁移 SQL |
| [20260711031857_v2_academic_workspace.sql](../supabase/migrations/20260711031857_v2_academic_workspace.sql) | v2.0 正式 Supabase 迁移、旧项目回填与索引 |
| [20260711065932_repair_document_section_versions.sql](../supabase/migrations/20260711065932_repair_document_section_versions.sql) | 修复章节当前版本与历史最大版本不一致 |
| [20260711092831_academic_document_quality_scopes.sql](../supabase/migrations/20260711092831_academic_document_quality_scopes.sql) | v2.0.1 章节/文档分析作用域、审查生命周期和选择性共写迁移 |
| [20260721002107_user_auth_and_workspace_isolation.sql](../supabase/migrations/20260721002107_user_auth_and_workspace_isolation.sql) | v2.1 Supabase Auth 用户资料、工作区归属、RLS 与权限收紧迁移 |

## 使用与部署

| 文档 | 说明 |
| --- | --- |
| [DEMO_GUIDE.md](guides/DEMO_GUIDE.md) | Demo 启动与演示路径 |
| [supabase-setup.md](guides/supabase-setup.md) | Supabase / Postgres 配置 |
| [AUTH_SETUP.md](guides/AUTH_SETUP.md) | Google OAuth、Resend 邮箱 OTP、JWT 与双用户隔离验收 |
| [aws-ec2-deploy.md](guides/aws-ec2-deploy.md) | AWS EC2 部署零基础手册（同域 Nginx + HTTPS，不改仓库代码） |
| [vercel-deploy.md](guides/vercel-deploy.md) | Vercel 前端部署零基础手册（团队 harry-w-projects，API rewrite 到外部后端） |
| [frontend/README.md](../frontend/README.md) | 前端模块说明 |

## 项目进度与交付

| 文档 | 说明 |
| --- | --- |
| [PRODUCT_COMPLETION_STATUS-6-16.md](project/PRODUCT_COMPLETION_STATUS-6-16.md) | 产品完成度快照 |
| [FINAL_DELIVERY_CHECKLIST.md](project/FINAL_DELIVERY_CHECKLIST.md) | 最终交付清单 |

## 个人与样例

| 文档 | 说明 |
| --- | --- |
| [resume_project_experience.md](personal/resume_project_experience.md) | 简历项目经历描述 |
| [samples/](samples/) | Demo 上传用的样例材料 |

## 推荐阅读顺序

**产品 / 设计：** V2_ACADEMIC_UPGRADE -> PRD -> frontend_page_spec -> P7 工作台历史设计/视觉稿

**前端：** AUTH_SETUP -> V2_ACADEMIC_UPGRADE -> v2_academic_workspace_api -> DEMO_GUIDE

**后端：** AUTH_SETUP -> v2_academic_workspace_api -> Supabase v2/v2.1 migrations -> backend_service_spec

**快速跑 Demo：** DEMO_GUIDE -> FINAL_DELIVERY_CHECKLIST
