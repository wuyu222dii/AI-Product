# AI 论文共写工作台 v2.0 前端说明

## 1. 当前状态

当前前端已经基于现有产品文档、页面原型和后端真实接口，完成了主链路页面实现：

- 项目首页
- 上传页
- 解析状态页
- 材料充足性检查页
- 项目知识库页
- 学术多文档与章节工作台
- 共写工作台主页面
- 导出页

当前目标是：

`保留 v1.x 可信共写能力，并提供面向本科、硕士、博士与科研人员的多文档章节级研究工作台。`

---

## 2. 技术栈

- React 18
- Vite 6
- Lucide React
- Playwright
- 原生 CSS
- fetch API

---

## 3. 目录结构

```text
frontend/
├── index.html
├── package.json
├── playwright.config.js
├── e2e
│   └── v2-academic-flow.spec.js
├── vite.config.js
└── src
    ├── App.jsx
    ├── main.jsx
    ├── components
    │   ├── ErrorBanner.jsx
    │   └── StatusBadge.jsx
    │   ├── academic
    │   │   ├── AcademicDocumentSwitcher.jsx
    │   │   ├── AcademicSectionNavigator.jsx
    │   │   ├── AcademicSectionEditor.jsx
    │   │   └── AcademicReadinessPanel.jsx
    │   └── workspace
    │       ├── WorkspaceReviewSidebar.jsx
    │       ├── WorkspaceReviewDrawer.jsx
    │       ├── WorkspaceAppealModal.jsx
    │       ├── WorkspaceEditorPanel.jsx
    │       ├── WorkspaceAiPanel.jsx
    │       └── WorkspaceVersionPanel.jsx
    ├── pages
    │   ├── ProjectListPage.jsx
    │   ├── UploadPage.jsx
    │   ├── ParsingStatusPage.jsx
    │   ├── MaterialGatePage.jsx
    │   ├── KnowledgeBasePage.jsx
    │   ├── AcademicDocumentsPage.jsx
    │   ├── WorkspacePage.jsx
    │   └── ExportPage.jsx
    ├── services
    │   └── api.js
    └── styles
        └── global.css
```

---

## 4. 当前已接后端接口

已按当前后端骨架接入：

- `GET /api/v1/workspaces`
- `POST /api/v1/workspaces`
- `POST /api/v1/workspaces/{id}/materials`
- `GET /api/v1/workspaces/{id}/materials`
- `POST /api/v1/workspaces/{id}/requirement-snapshot`
- `GET /api/v1/workspaces/{id}/requirement-snapshot`
- `POST /api/v1/workspaces/{id}/material-sufficiency-check`
- `POST /api/v1/workspaces/{id}/generate-draft`
- `POST /api/v1/workspaces/{id}/knowledge-base/build`
- `GET /api/v1/workspaces/{id}/knowledge-base/chunks`
- `POST /api/v1/workspaces/{id}/knowledge-base/search`
- `GET /api/v1/workspaces/{id}/drafts`
- `GET /api/v1/drafts/{id}`
- `GET /api/v1/drafts/{id}/review-items`
- `POST /api/v1/workspaces/{id}/co-write`
- `POST /api/v1/review-items/{id}/appeal`
- `POST /api/v1/drafts/{id}/export`
- `GET/PATCH /api/v1/workspaces/{id}/academic-profile`
- `POST/GET /api/v1/workspaces/{id}/documents`
- `GET/PATCH /api/v1/documents/{id}`
- `POST/GET /api/v1/documents/{id}/sections`
- `POST /api/v1/documents/{id}/readiness-check`
- `POST /api/v1/sections/{id}/readiness-check`
- `POST /api/v1/sections/{id}/generate`
- `POST /api/v1/sections/{id}/co-write/preview`
- `POST /api/v1/section-co-write-previews/{id}/apply`
- `POST /api/v1/documents/{id}/assemble`
- `GET /api/v1/documents/{id}/ai-actions`

---

## 5. 当前已接真的后端能力

前端当前已连接的真实能力包括：

- 真实数据库
- 真实 AI 语义解析
- 真实材料充足性检查
- 真实初稿生成
- 真实共写
- 真实审查 / 复审
- 真实导出
- 真实文件解析与图片 OCR
- 真实学术画像、多文档、可拖拽章节树和章节版本
- 真实文档 / 章节动态 readiness
- 真实文档材料范围与 AI 使用记录

当前重点不再是“接通接口”，而是：

`把这些真实后端能力组织成更完整、更顺滑的演示前端。`

---

## 6. 本地开发

### 前端启动

```bash
cd frontend
npm install
npm run dev
```

默认端口：

- `http://localhost:5173`

### 后端代理

Vite 已配置：

- `/api` -> `http://localhost:8080`
- 可通过 `VITE_API_PROXY_TARGET=http://127.0.0.1:8082` 临时覆盖代理目标

所以前端开发时无需额外改 API base URL。

---

## 7. 联调建议顺序

1. 先确认后端能启动
2. 前端起 Vite
3. 在项目首页创建 workspace
4. 上传文本 / 文件 / 图片材料进入解析页
5. 进入材料检查页，执行当前文档动态 readiness
6. 进入知识库页，构建并检索项目证据片段
7. 进入学术文档页，切换多文档、编辑章节、生成共写预览并组装导出
8. 需要时进入兼容整篇工作台触发可信链、审查、申诉和导出链路

---

## 8. 自动化验证

```bash
npm run test
npm run test:e2e
npm run test:all
```

- `npm run test`：生产构建 + MVP smoke。
- `npm run test:e2e`：使用本机 Chrome 跑 Playwright 用户流程，API 使用稳定 mock。
- `npm run test:all`：依次执行两类前端验证。
- 当前 E2E 覆盖学术画像、文字材料上传、多文档、章节拖拽排序、章节生成、共写预览与应用、AI 留痕、审查复查和导出。

---

## 9. 下一步建议

建议继续推进这几件事：

1. 引入 React Query 或等价数据层
2. 补充更完整的 loading / empty / success / error 收口
3. 把更多状态通过 query params 或 route state 显式化
4. 增加 Demo 首页引导与演示脚本入口
