# AI 论文共写工作台当前产品完成情况

更新时间：`2026-07-22`

## 1. 当前结论

当前项目已完成：

`v1.x MVP 主流程 + v2.0 全学术人群升级 + v2.0.1 学术文档统一工作台 + v2.1 用户隔离工程交付 + v2.2 产品化界面升级 + v2.2.1 新用户研究导航`。

更准确的阶段判断是：

`产品化内测准备阶段`。

核心业务、认证与隔离代码、真实数据库迁移和产品界面均已完成。Google OAuth 与 Resend OTP 的真实用户验收需要项目所有者在外部控制台填入私密凭据后执行，因此当前不能描述为“生产上线完成”。

`MVP 100%` 只表示原 PRD 的 MVP 范围已经完成，不表示生产运维、合规、协作和商业化能力达到 100%。

## 2. 版本状态

| 版本 | 状态 | 结论 |
| --- | --- | --- |
| v1.x | 已完成 | 上传、解析、材料检查、文献补充、知识库、可信链、原创补强、共写、审查与导出完整保留 |
| v2.0 | 已完成 | 支持本科、硕士、博士、科研人员、多文档、研究范式和动态 readiness |
| v2.0.1 | 已完成 | `DocumentSection` 成为唯一正文源，可信审查、可控共写和整篇交付进入统一工作台 |
| v2.1 | 工程与数据库已完成 | Supabase Auth、JWT、用户数据隔离、受保护文件与用户资料 API 已落地；真实 Google/Resend 双用户验收待凭据 |
| v2.2 | 已完成第二轮 | 公共官网、登录页、Dashboard、项目概览、App Shell、工作流页面、响应式与无障碍检查已完成 |
| v2.2.1 | 已完成 | 6 步首次向导、可跳过状态、顶栏指南、动态项目路线、路线调整和真实 Supabase guide 回填已完成 |

## 3. 当前用户主流程

```text
Google / 邮箱 OTP 登录
-> 新用户研究导航（可跳过）
-> 研究项目 Dashboard
-> 创建项目与学术画像
-> 项目概览与建议下一步
-> 上传文件、文本或链接
-> 预处理、OCR 与 AI 语义解析
-> 解析质量清单与材料补全
-> 动态 readiness
-> 材料不足时检索并补传真实文献
-> 项目知识库
-> 多学术文档与章节树
-> 章节生成、编辑与共写预览
-> 选择性应用为章节新版本
-> 可信链、原创风险、审查、申诉与复查
-> 整篇质量聚合
-> DOCX / PDF 导出
```

## 4. v2.1 用户隔离完成项

### 身份认证

- 前端固定使用 `@supabase/supabase-js 2.110.7`。
- Google 登录使用 `signInWithOAuth`，回调固定 `/auth/callback`。
- 邮箱登录使用 `signInWithOtp + verifyOtp`，支持 6 位数字、自动粘贴、重发倒计时和通用错误。
- `returnTo` 只允许站内 `/app` 路径，防止开放重定向。
- 登录为强制要求，不提供游客写作模式。

### 后端安全

- Spring Security Resource Server 通过官方 JWKS 校验 Supabase ES256 JWT。
- 校验 issuer、audience、有效期和 UUID subject。
- `/api/v1/**` 默认必须登录；未登录返回结构化 `401`。
- workspace、material、document、section、version、draft、review、appeal、evidence、preview、job 和 export 均执行归属检查。
- 他人资源与不存在资源统一返回 `404`。
- 请求体跨资源关联会验证位于同一 workspace；候选文献会在文件落盘前校验。
- 展示名称可编辑，但授权始终只使用 JWT `sub`。

### 数据与文件

- 新增 `user_profiles`，通过 Auth 用户创建触发器初始化。
- `workspaces.user_id` 已关联 `auth.users(id)`。
- 35 个历史 Demo workspace 已迁移为未归属，普通用户不可见。
- 新 workspace 必须有 owner；生产环境忽略旧项目临时访问配置。
- 业务表不向 `anon/authenticated` 提供直接 Data API 权限。
- 上传路径为 `uploaded-materials/{userId}/{workspaceId}`。
- 导出路径为 `generated-exports/{userId}/{workspaceId}`。
- 原文件与导出均通过带 Bearer Token 的 Blob 请求访问。

### 用户接口

- `GET /api/v1/me`：获取当前账号和展示资料。
- `PATCH /api/v1/me`：更新展示名称。
- `PATCH /api/v1/me/onboarding`：保存完成或跳过状态。

## 5. v2.2.1 新用户研究导航完成项

- 无项目且 `onboardingStatus=NOT_STARTED` 的新用户自动进入 `/app/onboarding`。
- 固定 6 步收集项目名称、学术阶段、文档类型、学科、研究范式、当前进度、已有内容、截止日期、篇幅和引用格式。
- 完成接口在一个事务内创建 workspace、学术画像、初始文档、guide 并更新用户状态；重复提交返回既有首个项目。
- 跳过后进入项目列表且不再自动弹出；顶栏“使用指南”可随时重新查看三阶段主线。
- 动态路线固定覆盖建立项目、添加材料、确认解析、准备度、知识库、章节写作、审查交付。
- 当前任务优先级为 `NEEDS_ATTENTION > CURRENT > IN_PROGRESS > OPTIONAL`，知识库保持推荐而非强制。
- 项目概览允许调整进度、已有内容、截止日期和 `GUIDED / FLEXIBLE` 模式。
- 访问他人 guide 沿 workspace 归属统一返回 `404`。
- 真实 Supabase 已执行 `20260722103000_user_onboarding_and_project_guides.sql`；现有账号保持已完成，新账号默认未开始。
- 数据库验收结果：`38` 个 workspace、`38` 个 guide、`0` 个缺失 guide，RLS 开启，公开角色授权为 `0`。

## 6. v2.2 产品界面完成项

- 公开路由：`/`、`/about`、`/sign-in`、`/auth/callback`、`/privacy`、`/terms`。
- 登录路由：`/app/projects` 及项目上传、解析、材料、知识库、学术文档页面。
- Dashboard 展示最近项目、阶段、文档数、更新时间、搜索和新建项目。
- 已有项目默认进入项目概览，新建项目直接进入材料准备，避免重复上传入口。
- 项目概览聚合材料、解析、知识片段、文档、章节和关键问题，并给出可跳过的建议下一步。
- App Shell 使用紧凑侧边导航、项目面包屑和用户菜单；导航按“研究准备 / 研究资产 / 写作交付”分组。
- 上传、解析、材料检查使用统一任务主线和状态视觉。
- 材料不足页的文献入口已按“关键词检索 / 检索目的 / 高级筛选 / 上传回流 / 外部全文入口”重排，高级筛选默认收起并删除重复建议区块。
- 解析、材料、知识库和学术文档页面统一为单一一级标题；知识库返回动作已修正为材料检查。
- 旧版整篇初稿入口已从材料检查主操作降为折叠的兼容操作，新项目默认进入章节写作。
- 知识库使用检索、结果、来源的清晰布局。
- 学术文档保持章节树、正文编辑器、按需检查抽屉三栏结构。
- 移动端使用精简页面位置、顶部导航和抽屉；登录表单优先出现在首屏，页面仅保留一个一级标题。
- 全局 CSS 已拆为 7 个职责文件，新增独立 onboarding 样式并统一视觉变量。
- E2E 会生成真实工作台截图到 `frontend/public/assets/workspace-product.png`，供官网和演示材料使用。
- Vite 已对 React、Supabase、拖拽和图标库拆包，所有 JS chunk 均低于 500 kB。

## 7. 核心产品能力

| 模块 | 当前状态 |
| --- | --- |
| 多格式上传、拖拽与原文件预览 | 已完成 |
| 文件抽取、图片 OCR、真实 AI 解析 | 已完成 |
| 解析质量清单与补充重解析 | 已完成 |
| 动态材料 readiness 与阻断 | 已完成 |
| Crossref / OpenAlex / Semantic Scholar 文献候选 | 已完成 |
| 文献质量评分、待下载、上传关联 | 已完成 |
| 项目知识库与关键词检索 | 已完成 |
| 学术画像、多文档、章节树和版本 | 已完成 |
| 章节级可信链和整篇覆盖率 | 已完成 |
| 原创实证与 AI 写作味风险提示 | 已完成 |
| 共写预览、逐段/差异行应用与 409 冲突保护 | 已完成 |
| 审查、申诉、复查和时效状态 | 已完成 |
| 章节组装与 DOCX/PDF 导出 | 已完成 |
| Google + 邮箱 OTP 前端流程 | 已完成代码，待真实 Provider 验收 |
| JWT 与所有业务资源隔离 | 已完成并通过自动化/数据库冒烟 |
| 产品化公共站点和工作台 | 已完成首轮 |
| 新用户研究导航与动态项目路线 | 已完成并通过桌面/手机 E2E |

## 8. 最近验证结果

| 验证项 | 结果 |
| --- | --- |
| 后端测试 | `103` 个通过，0 失败、0 错误、0 跳过 |
| 前端单元测试 | `11` 个通过，覆盖安全返回路径、受保护路由、会话异常恢复、OTP 与 Google OAuth 参数 |
| 前端构建与 smoke | 通过，稳定依赖已拆包，无大 chunk 告警 |
| Playwright | `17` 个通过，新增首次引导、完成创建、跳过、重开指南和动态路线覆盖 |
| npm audit | `0 vulnerabilities` |
| 真实 Spring 启动 | 通过，可连接真实 Supabase PostgreSQL |
| Supabase JWKS | `200` |
| 未登录 API | `/api/v1/workspaces` 返回结构化 `401` |
| 工作区与 guide | workspace 共 38 个，其中 35 个历史未归属、3 个归属用户；38 个 guide，无缺失 |
| RLS / grants | workspaces 与 user_profiles RLS 开启；业务表公开角色授权数为 0 |
| v2.1 migration history | `20260721002107:user_auth_and_workspace_isolation` 已登记 |
| v2.2.1 migration history | `20260722103000:user_onboarding_and_project_guides` 已登记，远端已是最新 |
| Supabase advisors | `No issues found` |

## 9. 尚待外部环境验收

- [ ] 在 Google Auth Platform 创建 OAuth Web Client。
- [ ] 在 Supabase 启用 Google Provider 并配置 Redirect URLs。
- [ ] 在 Resend 验证域名并创建 SMTP API Key。
- [ ] 在 Supabase 配置 Resend Custom SMTP 和 `{{ .Token }}` 模板。
- [ ] 填写 `frontend/.env.local` 的 Supabase URL 与 publishable key。
- [ ] 使用两个真实账号完成 Google、OTP、同邮箱身份关联和跨用户资源冒烟。

操作说明：[AUTH_SETUP.md](../guides/AUTH_SETUP.md)。

## 10. 下一阶段优先级

P0：

- 完成真实 Provider 双用户验收并补 Auth Playwright storage state。
- 将本地上传/导出迁移到私有对象存储。
- 将内存 job 改为持久化队列，保留 ownerUserId、重试和失败原因。
- 增加登录与 AI 调用限流、滥用保护和可观测性。

P1：

- 账号删除、数据导出和隐私请求流程。
- PDF 页码级来源定位与原文截图。
- CSL、RIS/BibTeX、正式学位论文排版和 LaTeX。
- 共写词级 diff 与修改后的复查任务列表。

P2：

- 导师/合作者批注、分享与角色权限。
- 管理员后台、配额、成本统计与监控告警。

## 11. 最终判断

`项目已从可演示学术写作 MVP 升级为具备账号边界和统一产品界面的内测版本。代码、迁移和自动化验收已完成；完成 Google/Resend 私密配置与真实双用户验证后，可进入小规模封闭用户测试。`
