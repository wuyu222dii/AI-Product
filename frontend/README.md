# AI 论文共写工作台 v2.2.1 前端说明

React 前端已经完成 Supabase Auth、产品化 App Shell、新用户研究导航、多文档章节工作台与可信交付主链路。

## 当前入口

公开页面：

- `/`、`/about`
- `/sign-in`、`/auth/callback`
- `/privacy`、`/terms`

登录后页面：

- `/app/onboarding`：首次 6 步研究导航；`?mode=tour` 为随时可重开的使用指南。
- `/app/projects`：项目 Dashboard。
- `/app/projects/{workspaceId}`：动态项目路线与项目画像。
- `/app/projects/{workspaceId}/upload`：研究输入。
- `/app/projects/{workspaceId}/parsing`：解析质量与补充重解析。
- `/app/projects/{workspaceId}/materials`：动态 readiness 与文献补充。
- `/app/projects/{workspaceId}/knowledge`：知识库。
- `/app/projects/{workspaceId}/documents/{documentId?}`：学术文档统一工作台。

## 技术栈

- React 18 + React Router
- Vite 6
- Supabase JS 2.110.7
- Lucide React
- dnd-kit
- Vitest + Testing Library
- Playwright + axe-core
- 自托管 Noto Sans SC / Noto Sans Mono / Noto Serif SC

## 代码结构

```text
src/
├── auth/                 Supabase 会话、受保护路由与 OAuth 回调
├── components/
│   ├── academic/         学术文档编辑、章节树、检查和共写预览
│   ├── guide/            路线选项与状态文案
│   └── workspace/        v1.x 兼容工作台组件
├── pages/
│   ├── OnboardingPage.jsx
│   ├── ProjectListPage.jsx
│   ├── ProjectOverviewPage.jsx
│   ├── UploadPage.jsx
│   ├── ParsingStatusPage.jsx
│   ├── MaterialGatePage.jsx
│   ├── KnowledgeBasePage.jsx
│   └── AcademicDocumentsPage.jsx
├── services/api.js       带 Bearer Token 的统一 API 客户端
└── styles/
    ├── tokens.css
    ├── base.css
    ├── public.css
    ├── app-shell.css
    ├── workflow.css
    ├── academic-workspace.css
    └── onboarding.css
```

## v2.2.1 导航能力

- `GET /api/v1/me` 读取 `onboardingStatus`。
- `PATCH /api/v1/me/onboarding` 保存完成或跳过状态。
- `POST /api/v1/onboarding/complete` 事务创建首个项目。
- `GET/PATCH /api/v1/workspaces/{id}/guide` 读取或调整动态路线。
- 新用户无项目时自动进入向导；已有用户不会被强制跳转。
- 项目路线来自后端真实材料、解析、准备度、知识库、章节和审查状态，不由前端自行猜测。
- 路线只推荐下一步，不锁定页面；知识库明确为推荐步骤。

## 本地开发

```bash
cp .env.example .env.local
npm install
npm run dev
```

默认前端地址：`http://localhost:5173`。Vite 将 `/api` 代理到 `VITE_API_PROXY_TARGET`，默认后端为 `http://localhost:8080`。

## 自动化验证

```bash
npm run test:all
npm audit
```

当前结果：

- Vitest：`11` 个通过。
- 生产构建与 MVP smoke：通过。
- Playwright：`17` 个通过，覆盖向导创建、跳过、重开、认证、核心学术流程、桌面/平板/手机布局和基础无障碍。
- npm audit：`0 vulnerabilities`。

完整启动与演示说明见 [DEMO_GUIDE.md](../docs/guides/DEMO_GUIDE.md)，当前状态以 [PRODUCT_COMPLETION_STATUS-6-16.md](../docs/project/PRODUCT_COMPLETION_STATUS-6-16.md) 为准。
