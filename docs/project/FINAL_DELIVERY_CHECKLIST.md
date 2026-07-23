# AI 论文共写工作台 v2.2.1 当前交付清单

更新时间：`2026-07-22`

当前交付结论：`v2.1 用户隔离 + v2.2 产品化界面 + v2.2.1 新用户研究导航与动态路线已完成`。

Google/Resend 私密凭据不进入仓库，因此真实 Provider 与双用户登录验收单独列为环境待办，不计入代码缺陷。

## 1. 文档交付

- [x] [README.md](../../README.md)
- [x] [PRODUCT_COMPLETION_STATUS-6-16.md](PRODUCT_COMPLETION_STATUS-6-16.md)
- [x] [AUTH_SETUP.md](../guides/AUTH_SETUP.md)
- [x] [DEMO_GUIDE.md](../guides/DEMO_GUIDE.md)
- [x] [V2_ACADEMIC_UPGRADE.md](../product/V2_ACADEMIC_UPGRADE.md)
- [x] [v2_academic_workspace_api.md](../engineering/v2_academic_workspace_api.md)
- [x] [openapi_contract_draft.md](../engineering/openapi_contract_draft.md)
- [x] [20260721002107_user_auth_and_workspace_isolation.sql](../../supabase/migrations/20260721002107_user_auth_and_workspace_isolation.sql)
- [x] [20260722103000_user_onboarding_and_project_guides.sql](../../supabase/migrations/20260722103000_user_onboarding_and_project_guides.sql)

## 2. v2.1 前端认证

- [x] 固定版本 `@supabase/supabase-js` 已安装并锁定。
- [x] `AuthProvider` 负责初始化、恢复、刷新和监听会话。
- [x] `ProtectedRoute` 保护全部 `/app` 路由。
- [x] Google OAuth 使用 `signInWithOAuth` 和 `/auth/callback`。
- [x] 邮箱使用 6 位 OTP，支持数字过滤、粘贴、重发倒计时和错误提示。
- [x] OAuth 回调恢复站内 `returnTo`。
- [x] `returnTo` 阻止站外跳转、`//` 和反斜杠路径。
- [x] API 客户端自动附加当前 access token。
- [x] `401` 清理会话并返回登录页。
- [x] 退出只清理当前设备会话和用户临时状态。
- [x] 材料预览与导出使用 Bearer Token 获取 Blob，不暴露裸下载链接。
- [x] 业务项目状态迁移到 URL，用户偏好按 UUID 隔离。

关键文件：

- [supabaseClient.js](../../frontend/src/auth/supabaseClient.js)
- [AuthProvider.jsx](../../frontend/src/auth/AuthProvider.jsx)
- [ProtectedRoute.jsx](../../frontend/src/auth/ProtectedRoute.jsx)
- [SignInPage.jsx](../../frontend/src/pages/SignInPage.jsx)
- [AuthCallbackPage.jsx](../../frontend/src/pages/AuthCallbackPage.jsx)
- [api.js](../../frontend/src/services/api.js)

## 3. v2.1 后端隔离

- [x] Spring Security、OAuth2 Resource Server 和 security test 依赖已接入。
- [x] Supabase JWKS 验证 ES256 JWT。
- [x] issuer、audience、expiration 和 UUID subject 校验已启用。
- [x] `/api/v1/**` 默认要求 Bearer Token。
- [x] 未登录返回 `401`，他人资源返回 `404`。
- [x] 工作区创建直接使用 JWT `sub`，列表只查询当前用户。
- [x] workspace、material、document、section、version、draft、review、appeal、evidence、preview、job 和 export 归属检查已覆盖。
- [x] 要求、材料、候选文献、版本和共写请求体跨资源关联会验证同一 workspace。
- [x] 候选文献在文件落盘前验证，避免失败请求留下材料或文件。
- [x] job 保存 ownerUserId，查询和下载均验证 owner。
- [x] 文件路径包含 userId/workspaceId 并执行根目录规范化检查。
- [x] `GET/PATCH /api/v1/me` 已完成，授权不依赖可编辑 metadata。
- [x] 开发环境旧 Demo owner 配置在 production 自动失效。

关键文件：

- [SecurityConfig.java](../../backend/src/main/java/com/aipm/cowriting/config/SecurityConfig.java)
- [OwnershipWebMvcConfig.java](../../backend/src/main/java/com/aipm/cowriting/config/OwnershipWebMvcConfig.java)
- [CurrentUserService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/CurrentUserService.java)
- [ResourceOwnershipService.java](../../backend/src/main/java/com/aipm/cowriting/application/service/ResourceOwnershipService.java)
- [CurrentUserController.java](../../backend/src/main/java/com/aipm/cowriting/interfaces/rest/CurrentUserController.java)

## 4. v2.1 数据库

- [x] `user_profiles` 已创建并引用 `auth.users(id)`。
- [x] Auth 用户创建触发器已创建，函数权限已收紧。
- [x] `workspaces.user_id` 外键已指向 `auth.users(id)`。
- [x] `legacy_unowned` 与 owner 状态约束已生效。
- [x] `(user_id, updated_at desc)` 部分索引已创建。
- [x] 35 个旧 Demo workspace 已迁移为未归属。
- [x] 普通用户无法看到未归属 workspace。
- [x] `anon/authenticated` 对 public 业务表授权已撤销。
- [x] user_profiles 和 workspaces RLS 已核验开启。
- [x] 迁移已在真实 Supabase 执行并登记 history。
- [x] `supabase db advisors` 无 warn/error。
- [x] 管理员历史项目归属 SQL 已写入 Auth 指南。

## 5. v2.2 页面与设计

- [x] 公开首页以产品名称和真实工作台截图为首屏信号。
- [x] About 页面说明产品理念、可信流程和 AI 边界。
- [x] 登录、OAuth 回调、隐私、条款页面已完成。
- [x] `/app/projects` Dashboard 支持统计、搜索、新建和最近项目。
- [x] App Shell 使用紧凑左侧导航、顶部项目上下文和用户菜单。
- [x] 上传、解析、材料检查和知识库页面已统一视觉层级。
- [x] 学术文档保持章节树、正文、检查抽屉三栏工作台。
- [x] 移动端导航、抽屉和正文优先布局已完成。
- [x] Loading、Empty、Toast、错误和按钮层级已统一。
- [x] Noto 字体已自托管，颜色全部由 CSS variables 管理。
- [x] 原 5381 行全局 CSS 已拆为 7 个职责文件并删除旧文件。
- [x] React、Supabase、拖拽和图标依赖已拆包，无 500 kB chunk 告警。
- [x] 真实工作台截图已生成到 `frontend/public/assets/workspace-product.png`。

## 6. v2.2.1 新用户研究导航

- [x] 新用户无项目时自动进入 `/app/onboarding`，已有用户不被强制跳转。
- [x] 6 步向导覆盖平台主线、项目画像、研究方式、当前进度、已有内容与路线预览。
- [x] “稍后再说”持久化为 `SKIPPED`，跳过后不重复自动弹出。
- [x] 顶栏“使用指南”可随时重新查看系统三阶段与任务主线。
- [x] 首次完成接口以事务创建 workspace、学术画像、首个文档与 guide，并更新用户状态。
- [x] 重复完成请求返回已有首个项目，不创建第二份数据。
- [x] 项目概览的 7 项路线由后端真实状态派生，知识库保持推荐步骤。
- [x] 项目概览可修改进度、已有内容、截止日期和引导模式。
- [x] guide 访问沿 workspace 做用户隔离，他人资源统一返回 `404`。
- [x] 真实 Supabase 已完成 guide 回填：`38/38`，缺失 `0`。
- [x] `project_guides` RLS 已开启，`anon/authenticated` 授权数为 `0`。
- [x] 桌面与手机引导流程、跳过、重开和动态路线已纳入 Playwright。

样式文件：

- [tokens.css](../../frontend/src/styles/tokens.css)
- [base.css](../../frontend/src/styles/base.css)
- [public.css](../../frontend/src/styles/public.css)
- [app-shell.css](../../frontend/src/styles/app-shell.css)
- [workflow.css](../../frontend/src/styles/workflow.css)
- [academic-workspace.css](../../frontend/src/styles/academic-workspace.css)
- [onboarding.css](../../frontend/src/styles/onboarding.css)

## 7. 自动化与真实环境验证

- [x] 后端 `mvn test`：`103` 个通过，0 失败、0 错误、0 跳过。
- [x] Controller 测试统一使用 mock JWT。
- [x] 跨租户 workspace、material、document、section、draft、review、evidence 与 job 行为已测试。
- [x] 前端 Vitest：`11` 个通过。
- [x] 前端生产构建与 MVP smoke 通过。
- [x] Playwright：`17` 个通过。
- [x] 视口：`1440x900`、`1024x768`、`390x844` 无横向溢出。
- [x] 登录页基础 WCAG serious/critical 检查通过。
- [x] `npm audit`：0 vulnerabilities。
- [x] Spring Boot 真实 Supabase 启动成功。
- [x] Supabase JWKS HTTP 200。
- [x] 未登录业务 API 返回结构化 401。
- [x] 真实数据库 owner/RLS/grants/migration history 已核验。

## 8. 需要项目所有者完成

- [ ] 提供 Google OAuth Client ID/Secret 并启用 Supabase Provider。
- [ ] 提供 Resend API Key、验证发送域名并启用 Supabase Custom SMTP。
- [ ] 将邮件模板改为显示 `{{ .Token }}`。
- [ ] 在 `frontend/.env.local` 填入 Supabase URL 与 publishable key。
- [ ] 配置本地和生产 Redirect URLs。
- [ ] 使用同一邮箱验证 Google/OTP 身份关联。
- [ ] 使用用户 A/B 验证项目、文件、章节、job 和导出隔离。

完成方法：[AUTH_SETUP.md](../guides/AUTH_SETUP.md)。

## 9. 非本轮范围

- [ ] Supabase Storage 或其他私有对象存储。
- [ ] 持久化异步队列、重试和死信。
- [ ] 项目分享、导师协作和细粒度角色。
- [ ] 账号删除、数据导出和管理员后台。
- [ ] 生产限流、反滥用、成本监控和告警。
- [ ] CSL、RIS/BibTeX、LaTeX 和正式学位论文模板。

## 10. 交付判断

`代码、数据库迁移、自动化测试和产品化界面已经交付。完成 Google/Resend 控制台配置与真实双用户验收后，可进入封闭内测；当前仍不应描述为生产上线完成。`
