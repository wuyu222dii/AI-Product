# AI 论文共写工作台 v2.2

面向本科生、硕士生、博士生与科研人员的证据驱动型研究共创平台。系统以真实材料、章节版本、来源追溯和人工确认为基础，支持从研究输入到学术文档交付的完整流程；不承诺规避 AI 检测或论文查重。

当前基线：`v1.x 可信写作能力 + v2.0 全学术人群升级 + v2.0.1 学术文档统一工作台 + v2.1 用户隔离 + v2.2 产品化界面`。

当前状态唯一事实来源：[PRODUCT_COMPLETION_STATUS-6-16.md](docs/project/PRODUCT_COMPLETION_STATUS-6-16.md)。Supabase Auth 控制台配置见 [AUTH_SETUP.md](docs/guides/AUTH_SETUP.md)。

## 最新交付

### v2.1 用户隔离

- Supabase Auth：Google OAuth、6 位邮箱 OTP、PKCE 会话恢复与当前设备退出。
- Spring Security：通过 Supabase JWKS 校验 ES256 JWT，同时校验 issuer、`aud=authenticated`、有效期与 UUID `sub`。
- API 保护：`/api/v1/**` 必须携带 Bearer Token；未登录返回 `401`，访问他人资源统一返回 `404`。
- 资源隔离：workspace、material、document、section、version、draft、review、evidence、preview、job 与 export 均沿工作区归属校验。
- 文件隔离：上传与导出路径包含 `{userId}/{workspaceId}`，并执行规范化路径检查；预览和下载均使用带 Token 的 Blob 请求。
- 数据迁移：新增 `user_profiles`，35 个历史 Demo 项目已转为未归属且对普通用户不可见，业务表不向 `anon/authenticated` 开放 Data API 权限。
- 用户接口：`GET /api/v1/me`、`PATCH /api/v1/me`。

迁移文件：[20260721002107_user_auth_and_workspace_isolation.sql](supabase/migrations/20260721002107_user_auth_and_workspace_isolation.sql)。迁移已在真实 Supabase 执行并登记到 migration history。

### v2.2 产品化界面

- 新增品牌首页、产品理念、登录、OAuth 回调、隐私与条款页面。
- 登录后统一为 `/app/projects` 信息架构，旧路径保留重定向或隐藏兼容入口。
- 项目首页升级为 Dashboard，App Shell 使用紧凑侧边栏、项目导航与用户菜单。
- 上传、解析、材料检查、知识库和学术文档使用统一任务视觉系统。
- 学术文档保持章节树、正文编辑器、按需检查抽屉的三栏工作台；整篇检查只读。
- 设计系统改为青绿主色、琥珀强调、6/8px 圆角、细边框和浅色学术阅读界面。
- 自托管 Noto Sans SC、Noto Sans Mono、Noto Serif SC；CSS 已拆为 tokens、base、public、app-shell、workflow 和 academic-workspace。
- 已完成桌面、平板、手机响应式检查、基础无障碍检查和真实产品截图。

## 产品主流程

```text
登录
-> 创建研究项目与学术画像
-> 上传并 AI 解析真实材料
-> 查看解析质量和动态 readiness
-> 材料不足时检索并补传真实文献或研究成果
-> 构建项目知识库
-> 创建或切换学术文档
-> 按章节生成、编辑和可控共写
-> 查看可信链、原创风险、审查与复查
-> 整篇质量检查
-> 按章节组装并导出
```

`DocumentSection` 是唯一可编辑正文源。整篇视图只负责组装、检查、审查与导出，旧整篇工作台仅保留兼容。

## 目录

| 路径 | 用途 |
| --- | --- |
| [backend](backend) | Spring Boot 3 / Java 17 REST 后端 |
| [frontend](frontend) | React / Vite 前端与 Playwright E2E |
| [supabase/migrations](supabase/migrations) | Supabase 版本化迁移 |
| [docs/README.md](docs/README.md) | 研发文档索引 |
| [docs/guides/AUTH_SETUP.md](docs/guides/AUTH_SETUP.md) | Google、Resend、Supabase Auth 配置 |
| [docs/guides/DEMO_GUIDE.md](docs/guides/DEMO_GUIDE.md) | 启动与演示路径 |
| [docs/project/FINAL_DELIVERY_CHECKLIST.md](docs/project/FINAL_DELIVERY_CHECKLIST.md) | 当前交付清单 |

## 配置与启动

复制示例变量后填写本机私密值，不要提交真实密钥：

```bash
cp backend/.env.example backend/.env
cp frontend/.env.example frontend/.env.local
```

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

默认地址：前端 `http://localhost:5173`，后端 `http://localhost:8080`。数据库结构仅通过迁移维护，运行时保持 `HIBERNATE_DDL_AUTO=none`。

## 验证

```bash
cd backend && mvn test
cd frontend && npm run test:all
```

最近一次验证（2026-07-21）：

- 后端 `92` 个测试通过，`0` 失败、`0` 错误、`0` 跳过。
- 前端 `6` 个 Vitest 测试、生产构建和 smoke test 通过。
- Playwright `10` 个场景通过，覆盖认证、核心流程、`1440x900 / 1024x768 / 390x844` 与基础无障碍检查。
- `npm audit` 为 `0 vulnerabilities`。
- Spring Boot 已连接真实 Supabase；JWKS 返回 `200`，未登录业务 API 返回结构化 `401`。
- 真实数据库中 35 个历史工作区均为未归属；RLS 开启，`anon/authenticated` 对业务表授权数为 0。
- `supabase db advisors` 返回 `No issues found`。

## 当前边界

- Google Client ID/Secret、Resend SMTP 和发送域名仍需在对应控制台配置；仓库不包含这些私密值，因此真实 Google/OTP 双用户冒烟尚待环境配置后执行。
- 当前文件仍保存在本机，job 仍为内存实现；对象存储、持久化队列、限流、监控和账号删除不属于本轮。
- 项目暂不支持分享、导师协作和管理员后台。
- CSL、RIS/BibTeX、LaTeX 与正式学位论文模板仍属于后续交付质量增强。
