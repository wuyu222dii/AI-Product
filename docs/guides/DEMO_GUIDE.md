# AI 论文共写工作台 v2.2 Demo 使用说明

更新时间：`2026-07-21`

## 1. Demo 范围

当前版本可演示：

1. Google 或 6 位邮箱 OTP 登录。
2. 用户独立的研究项目 Dashboard。
3. 学术画像、多文档和可拖拽章节树。
4. 多格式材料上传、预处理、OCR 与 AI 语义解析。
5. 解析质量清单、动态 readiness 和材料补全。
6. 材料不足时检索真实文献候选并回传原文。
7. 项目知识库和文档专属材料范围。
8. 章节生成、选区共写、预览和选择性应用。
9. 可信链、原创实证风险、审查、申诉与复查。
10. 整篇质量聚合和 DOCX/PDF 导出。

登录 Provider 未配置时仍可演示公开首页、About、登录界面和响应式设计，但不能进入真实业务工作台。完整 Auth 配置见 [AUTH_SETUP.md](AUTH_SETUP.md)。

## 2. 启动前配置

后端：

```bash
cp backend/.env.example backend/.env
```

至少填写数据库、OpenAI 和 Auth 项目地址：

```bash
SUPABASE_DB_HOST=...
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=...
SUPABASE_DB_PASSWORD=...
HIBERNATE_DDL_AUTO=none
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_JWT_AUDIENCE=authenticated
OPENAI_API_KEY=...
OPENAI_BASE_URL=...
OPENAI_MODEL=...
OPENAI_TIMEOUT_SECONDS=60
```

前端：

```bash
cp frontend/.env.example frontend/.env.local
```

```bash
VITE_SUPABASE_URL=https://<project-ref>.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=<publishable-key>
VITE_API_PROXY_TARGET=http://127.0.0.1:8080
```

不要把真实 `.env`、数据库密码、service role key、Google Client Secret 或 Resend API Key 提交到仓库。

## 3. 启动

后端：

```bash
cd backend
mvn spring-boot:run
```

热部署开发模式：

```bash
cd backend
./scripts/dev-hot.sh
```

前端：

```bash
cd frontend
npm install
npm run dev
```

默认地址：

- 公开首页：`http://localhost:5173/`
- 登录页：`http://localhost:5173/sign-in`
- 项目页：`http://localhost:5173/app/projects`
- 后端：`http://localhost:8080`

## 4. 首次登录验收

### Google

1. 打开 `/sign-in`。
2. 点击“使用 Google 登录”。
3. 完成 Google 授权。
4. 确认回到 `/auth/callback` 后进入 `/app/projects`。
5. 刷新页面，确认会话仍在。

### 邮箱 OTP

1. 输入邮箱并发送验证码。
2. 从邮件复制 6 位数字。
3. 粘贴到单一验证码输入框并登录。
4. 60 秒后确认可重新发送。

同一邮箱分别登录 Google 与 OTP 后，应在 Supabase Users 中显示相同 user ID。

## 5. 推荐业务演示

1. 在 Dashboard 新建研究项目。
2. 选择本科、硕士、博士或科研人员，并设置研究范式和首个文档。
3. 上传学校/导师/期刊要求、研究笔记、数据、已有草稿和参考文献。
4. 进入解析页，执行预处理和 AI 解析。
5. 查看“可用于生成 / 建议确认 / 需要补充 / 解析失败”质量状态。
6. 对问题使用“填入补充说明”并重新解析。
7. 进入材料检查；材料不足时打开文献补充入口。
8. 检索 Crossref / OpenAlex / Semantic Scholar，查看候选质量、匹配原因与元数据缺口。
9. 将候选加入待下载清单，自行获取原文后回到上传页关联候选。
10. 原文完成 AI 解析后重新执行 readiness。
11. 构建知识库并进入“学术文档”。
12. 切换文档、拖拽章节、设置文档材料范围。
13. 生成或编辑章节，选择一段正文生成共写预览。
14. 演示整版、逐段或差异行应用，以及基准版本变化后的 `409` 防覆盖。
15. 打开本章检查，查看可信链、原创补强和审查项。
16. 手动审查并复查单项问题。
17. 切换整篇检查，从问题跳回对应章节。
18. 组装并导出文档；整篇视图不直接编辑全文。

## 6. 用户隔离演示

需要两个真实 Auth 用户：

1. 用户 A 创建项目并上传一个文件，记录 workspace 与 material UUID。
2. 用户 A 退出，用户 B 登录。
3. 用户 B 项目列表中不能出现 A 的项目。
4. 用户 B 直接请求 A 的 workspace/material UUID，应返回 `404`。
5. 用户 B 不能轮询 A 的 job，也不能下载 A 的原文件或导出。
6. 用户 A 再次登录，仍可访问自己的项目和文件。

未登录请求：

```bash
curl -i http://localhost:8080/api/v1/workspaces
```

预期为 `401`，响应错误码 `UNAUTHORIZED`。

## 7. 自动化验证

```bash
cd backend
mvn test
```

```bash
cd frontend
npm run test:all
npm audit
```

当前结果：

- 后端 `92` 个测试通过。
- 前端 `6` 个 Vitest 测试通过。
- 前端构建与 smoke 通过。
- Playwright `10` 个场景通过。
- 三种验收视口无横向溢出。
- 登录页基础无障碍检查通过。
- npm audit 为 0 vulnerabilities。

## 8. 页面展示重点

### 公开站点

- 首屏直接展示产品名称和真实学术工作台。
- About 明确 AI 是证据驱动共创助手，不代替用户或导师判断。
- 登录页只保留 Google 和邮箱 OTP 两个清晰入口。

### Dashboard

- 最近项目、学术阶段、文档数和更新时间可扫描。
- 搜索和新建项目是主要操作。
- 移动端导航收进顶部菜单，不挤压正文。

### 工作流页面

- 上传、解析、材料检查和知识库共享同一任务视觉语言。
- 材料不足不会兜底生成，而是进入真实文献补充路径。
- AI 失败显示可重试错误，不做本地伪解释。

### 学术文档

- 章节是唯一正文源。
- 检查能力在抽屉中按需打开，不堆叠在正文周围。
- 共写默认先预览，用户决定应用范围。
- 整篇检查只读，问题可以定位回章节。

## 9. 常见问题

### 登录页提示 Supabase 未配置

补全 `frontend/.env.local` 后重启 Vite。

### API 始终返回 401

确认前后端使用同一个 Supabase project，并检查浏览器请求是否包含 Bearer Token。

### 收不到 OTP

检查 Resend 域名、SMTP 设置、Supabase Auth Logs 和 `{{ .Token }}` 邮件模板。

### AI 解析失败

检查 OpenAI key、base URL、model 和网络；页面会保留失败状态并允许重试或补充说明。

### 历史项目看不到

这是 v2.1 的预期隔离行为。历史 Demo 项目默认未归属；使用 [AUTH_SETUP.md](AUTH_SETUP.md) 中的管理员 SQL 指定 owner。

## 10. 当前演示边界

- Google/Resend 未配置时不能演示真实登录。
- 本地文件和内存 job 不适合多实例生产部署。
- 暂无项目分享、导师协作和管理员后台。
- 产品适合封闭内测，不应描述为已完成生产上线。
