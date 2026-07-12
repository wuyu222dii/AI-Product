# AI 论文共写工作台 — Vercel 前端部署操作手册（零基础版）

本文教你：把本项目的 **前端** 部署到 [Vercel](https://vercel.com/harry-w-projects)（团队空间 `harry-w-projects`），得到一个可分享的 **`https://xxx.vercel.app`** 链接。

**重要前提（先读这一段）：**

| 组件 | 能不能放在 Vercel？ | 说明 |
| --- | --- | --- |
| **前端**（`frontend/`，React + Vite） | **能** | Vercel 专门适合托管这种静态前端 |
| **后端**（`backend/`，Spring Boot / Java） | **不能** | Vercel 不跑 Java 服务；后端要另外部署 |
| **数据库**（Supabase PostgreSQL） | **不用迁到 Vercel** | 继续用现有 Supabase，由后端连接 |

因此本手册的完整上线路径是：

```text
浏览器 → Vercel 上的前端页面
              │
              │  请求 /api/v1/...（相对路径，和页面同域名）
              ▼
         Vercel 反向代理（rewrites）
              │
              ▼
         你已经部署好的后端（例如 AWS EC2）
              ├── Supabase PostgreSQL
              └── OpenAI API
```

前端代码里写死的是 `API_BASE = "/api/v1"`（见 `frontend/src/services/api.js`）。只要用 Vercel 把 `/api` **转发**到后端，就和 [aws-ec2-deploy.md](aws-ec2-deploy.md) 里 Nginx 同域反代是同一思路——**一般不需要改业务代码**。

相关文档：

- [DEMO_GUIDE.md](DEMO_GUIDE.md) — 本地跑通与演示路径
- [supabase-setup.md](supabase-setup.md) — 数据库配置
- [aws-ec2-deploy.md](aws-ec2-deploy.md) — **后端**在 AWS EC2 上的部署（零基础）

**预计总耗时：** 前端上 Vercel 约 **20–40 分钟**；若后端还没部署，请先按 AWS 手册把后端跑通（再加几小时）。

---

## 你需要提前准备的东西

| 项目 | 说明 |
| --- | --- |
| **GitHub 账号** | 仓库已推到 GitHub（或你已 fork） |
| **Vercel 账号** | 用 GitHub 登录 [https://vercel.com](https://vercel.com)，并加入团队 [harry-w-projects](https://vercel.com/harry-w-projects) |
| **本机已能跑通项目** | 本地 `mvn spring-boot:run` + `npm run dev` 成功过一次 |
| **后端公网地址** | 例如 `https://api.你的域名.com` 或 `https://你的域名.com`（只要浏览器能访问到 `/api/v1/...`）。没有后端时，Vercel 上只能打开页面壳子，**创建项目 / 上传 / AI 都会失败** |
| **浏览器** | 建议 Chrome |

仓库地址（若你 fork 了，换成你的）：

```text
https://github.com/wuyu222dii/AI-Product.git
```

---

## 部署前自检（强烈建议做完再点 Deploy）

在本机终端执行：

```bash
# 1）后端能响应（把端口按你本地为准）
curl -s http://127.0.0.1:8080/api/v1/workspaces | head

# 2）前端能生产构建
cd frontend
npm install
npm run build
```

`npm run build` 成功后会出现 `frontend/dist/`。若构建失败，先在本地修好再部署，否则 Vercel 上也会红。

确认后端公网可用（把地址换成你的）：

```bash
curl -s https://你的后端域名/api/v1/workspaces | head
```

能返回 JSON（哪怕是空列表）再继续。

---

## 第一部分：注册 / 登录 Vercel 并进入团队

### 步骤 1.1 打开 Vercel

1. 浏览器打开：[https://vercel.com](https://vercel.com)
2. 点 **Log in** / **Sign Up**
3. 选择 **Continue with GitHub**（推荐，后面导入仓库最省事）
4. 按提示授权 Vercel 读取你的 GitHub 仓库

### 步骤 1.2 进入团队空间 `harry-w-projects`

1. 打开：[https://vercel.com/harry-w-projects](https://vercel.com/harry-w-projects)
2. 若提示登录，用上一步的账号登录
3. 若提示你不在该团队：请团队管理员在 Vercel → **Settings** → **Members** 里邀请你的邮箱 / GitHub，你接受邀请后再打开该链接

登录成功后，你应能看到该团队下的项目列表（可能暂时是空的）。

> 若你暂时进不了团队，也可以先在 **个人账号** 下部署练手；流程相同，只是项目会出现在个人 Dashboard，而不是 `harry-w-projects`。

---

## 第二部分：从 GitHub 导入本项目

### 步骤 2.1 新建项目

1. 在 [harry-w-projects](https://vercel.com/harry-w-projects) 页面点 **Add New…** → **Project**
2. 在 **Import Git Repository** 里找到本仓库（例如 `AI-Product`）
3. 若列表里没有：
   - 点 **Adjust GitHub App Permissions** / **Configure**
   - 在 GitHub 授权页勾选该仓库（或选 All repositories）
   - 保存后回到 Vercel 刷新，再选仓库
4. 点仓库右侧的 **Import**

### 步骤 2.2 配置「只构建前端」（最关键，容易错）

本仓库是 **前后端 monorepo**。Vercel 默认会把仓库根目录当项目根，必须改成 `frontend`。

在 **Configure Project** 页面按下面设置：

| 配置项 | 填什么 | 为什么 |
| --- | --- | --- |
| **Project Name** | 例如 `aipm-frontend` 或 `ai-cowriting` | 会出现在 `xxx.vercel.app` 里，可之后改 |
| **Framework Preset** | **Vite** | 前端是 Vite；选 Vite 会自动带上合理默认值 |
| **Root Directory** | 点 **Edit**，选 **`frontend`** | 必须指向前端目录，否则构建脚本找不到 |
| **Build Command** | `npm run build` | 一般已自动填好，不要改成根目录命令 |
| **Output Directory** | `dist` | Vite 默认产物目录 |
| **Install Command** | `npm install` | 默认即可 |

核对示意：

```text
Root Directory:  frontend
Framework:       Vite
Build Command:   npm run build
Output Directory: dist
```

**Environment Variables：** 本项目前端当前**没有**必须配置的 `VITE_*` 生产变量（API 用相对路径 `/api/v1`）。这一步可以先空着。

先**不要**点 Deploy——先做第三部分的 `vercel.json`（或先 Deploy 一次看页面，再补代理；推荐一次配好再 Deploy）。

---

## 第三部分：配置同域 API 代理 + SPA 路由（必做）

没有这一步会出现两类经典问题：

1. 页面能开，但点「创建项目」等操作失败（`/api` 打到 Vercel 自己，得到 404）
2. 刷新 `/projects`、`/workspace/...` 等子路径出现 **404**（React Router 需要把所有前端路由回退到 `index.html`）

### 步骤 3.1 在 `frontend` 目录新增 `vercel.json`

在本机仓库里创建文件：

**路径：** `frontend/vercel.json`  
（因为 Vercel 的 Root Directory 是 `frontend`，配置文件要放在这个目录下，而不是仓库最外层。）

**内容模板**（把 `YOUR_BACKEND_ORIGIN` 换成你的后端**协议 + 域名**，不要带末尾斜杠）：

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://YOUR_BACKEND_ORIGIN/api/:path*"
    },
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

**填写示例：**

| 你的后端实际地址 | `destination` 应写成 |
| --- | --- |
| `https://cowriting.example.com`（同域 Nginx，API 也在该域名下） | `https://cowriting.example.com/api/:path*` |
| `https://api.example.com`（API 单独子域） | `https://api.example.com/api/:path*` |
| 临时用 EC2 公网 IP + HTTPS（较少见） | `https://x.x.x.x/api/:path*` |

**注意：**

- `source` 是 `/api/:path*`，`destination` 里也要保留 `/api/:path*`。后端期望的路径是 `/api/v1/...`，**不要**写成只转到 `https://后端/` 而丢掉 `/api`。
- 规则顺序：先写 `/api` 代理，再写 SPA 的 `/(.*)` → `/index.html`。顺序反了可能导致 API 也被当成前端页面。
- 若后端只有 HTTP（例如临时 `http://EC2_IP`），destination 可用 `http://...`，但浏览器对混合内容更敏感，**强烈建议后端也上 HTTPS**（见 AWS 手册 Certbot 部分）。

### 步骤 3.2 提交并推送到 GitHub

```bash
cd /你的/仓库路径
git add frontend/vercel.json
git status
git commit -m "$(cat <<'EOF'
Add Vercel rewrites for SPA and API proxy.

EOF
)"
git push
```

（若你还不想提交到主仓库，也可以在 Vercel 导入后，用 Vercel 网页编辑器临时加文件；长期仍建议放进 Git。）

### 步骤 3.3（可选）用 Vercel 控制台改 Rewrites

若暂时不能改仓库，也可在项目部署后：

1. 打开项目 → **Settings** → **Rewrites**（或部分界面在 **Settings → Domains / Redirects** 附近）
2. 用与上面相同的两条规则

官方说明见：[Rewrites on Vercel](https://vercel.com/docs/routing/rewrites)。**以仓库里的 `frontend/vercel.json` 为准更稳妥**，避免控制台与文件冲突。

---

## 第四部分：第一次部署

### 步骤 4.1 点击 Deploy

回到 **Configure Project**（或 Import 后的设置页），确认：

- Root Directory = `frontend`
- Framework = Vite
- 已推送含正确 `vercel.json` 的提交（或你准备在下一轮 Deployment 带上它）

点 **Deploy**。

### 步骤 4.2 看构建日志

1. 进入 **Deployments** 最新一条
2. 点进去看 **Building** 日志
3. 成功标志大致包括：
   - `npm install` 结束
   - `vite build` 成功
   - 出现类似 `Build Completed` / `Ready`
4. 失败时常见原因见本文「常见问题」

### 步骤 4.3 打开线上地址

部署成功后，Vercel 会给一个域名，例如：

```text
https://aipm-frontend.vercel.app
```

或带哈希的预览域名：

```text
https://aipm-frontend-xxxx-harry-w-projects.vercel.app
```

在团队 [harry-w-projects](https://vercel.com/harry-w-projects) 的项目页也能看到 **Domains** / **Visit**。

用 **无痕窗口** 打开正式域名，避免本地缓存干扰。

---

## 第五部分：上线验收清单（按顺序打勾）

在无痕窗口打开你的 `https://xxx.vercel.app`：

| # | 检查项 | 期望结果 |
| --- | --- | --- |
| 1 | 首页能打开 | 不是空白页、不是 Vercel 404 |
| 2 | 自动进到项目列表 `/projects` | 与本地 Demo 一致 |
| 3 | 浏览器开发者工具 → Network | 对 `/api/v1/workspaces` 的请求状态为 **200**（不是 404/502） |
| 4 | 创建一个研究项目 | 能成功并出现在列表 |
| 5 | 上传一份样例材料 | 上传成功，能进入解析流程 |
| 6 | 刷新当前子路由（如 `/projects`） | **不出现 404**（验证 SPA rewrite） |
| 7 | 打开一个深链再刷新 | 仍能进入对应页面 |

样例材料与演示路径见 [DEMO_GUIDE.md](DEMO_GUIDE.md)。

**发给老师/同学的一句话模板：**

> AI 论文共写工作台在线演示：  
> **https://你的项目.vercel.app**  
> 建议使用 Chrome；首次进入可按演示路径创建研究项目 → 上传样例材料 → 进入学术文档章节工作台。

---

## 第六部分：以后如何更新网站

每次你把改动推到 GitHub 的关联分支（通常是 `main` / `master`）：

1. Vercel 会 **自动重新构建并部署**
2. 在 **Deployments** 里可看到新记录
3. 若某次坏了：在该 Deployment 右侧菜单选 **Promote** / **Rollback** 回滚到上一版（界面文案可能略有差异）

只改前端、不改 `vercel.json` 时，一般不用动代理配置。

若后端域名变了：改 `frontend/vercel.json` 里的 `destination`，提交推送，等重新部署即可。

---

## 第七部分：自定义域名（可选）

免费 `.vercel.app` 已够演示。若要用自己的域名：

1. 项目 → **Settings** → **Domains**
2. 添加 `www.你的域名.com` 或 `app.你的域名.com`
3. 按页面提示去域名服务商添加 **A / CNAME** 记录
4. 等 DNS 生效（几分钟到几小时），Vercel 会自动签 HTTPS 证书

注意：若你用自定义域名，且 API 代理仍指向另一台后端，**无需**把后端也改成同一域名；保持 `vercel.json` 指向正确后端即可。若希望「前端域名 = 后端域名」整站同域，更适合用 [aws-ec2-deploy.md](aws-ec2-deploy.md) 的 Nginx 方案，而不是拆开 Vercel + 另域后端。

---

## 第八部分：和 AWS EC2 方案怎么选

| 场景 | 更推荐 |
| --- | --- |
| 只要快速分享**前端演示站**，后端已有公网地址 | **本文：Vercel** |
| 希望一个域名同时出页面和 API、改配置少、课程作业整包上机 | **AWS EC2 + Nginx** |
| 完全零运维、但后端是 Java | 前端 Vercel + 后端仍需 EC2 / 其他能跑 JVM 的平台 |

两者可组合：Vercel 托管前端，EC2 只跑 Spring Boot；用 `vercel.json` 把 `/api` 指到 EC2。

---

## 第九部分：常见问题

| 现象 | 可能原因 | 处理 |
| --- | --- | --- |
| Build 失败：找不到 `package.json` | Root Directory 没设成 `frontend` | Project Settings → General → Root Directory 改为 `frontend`，再 Redeploy |
| Build 失败：`npm` / Vite 报错 | 本地本身 build 不过，或 Node 版本差异 | 本地先 `npm run build`；Settings → General → Node.js Version 选 **20.x** |
| 页面空白 | 构建产物路径错，或 JS 报错 | 看 Deployment 的 Output；浏览器 Console 看报错 |
| 打开子路径 / 刷新 404 | 缺少 SPA rewrite | 确认 `frontend/vercel.json` 有 `/(.*)` → `/index.html`，且文件在 Root Directory 下 |
| 接口 404 | 没有把 `/api` 代理出去 | 检查 rewrite 的 `source`/`destination`；在 Network 看请求 URL 是否仍是 `xxx.vercel.app/api/...` |
| 接口 502 / 504 | 后端挂了、地址错、或后端只允许内网 | 本机 `curl` 后端公网地址；检查 EC2 安全组 80/443；看后端 systemd 日志 |
| CORS 报错 | 前端直连了另一个域名的 API，没用同域代理 | 回到「相对路径 + Vercel rewrite」方案，不要在前端写死第三方 API 域名（除非另做 CORS） |
| 能列项目但上传很大文件失败 | 代理/后端 body 限制 | 后端 Nginx 已加大 `client_max_body_size`；Vercel 对请求体也有平台限制，超大文件需控制体积或走直传策略 |
| AI 相关接口失败 | 后端 `OPENAI_*` 未配或 Key 无效 | 与 Vercel 无关，查后端 `.env`（见 DEMO_GUIDE / AWS 手册） |
| 进不了 [harry-w-projects](https://vercel.com/harry-w-projects) | 未加入团队或未登录 | 找管理员邀请；用同一 GitHub 登录 |
| 部署成功但像旧版页面 | 看错预览部署 / 缓存 | 打开 Production 域名；无痕窗口；确认最新 Deployment 已是 **Ready** 且已 Promote 到 Production |

---

## 第十部分：安全提醒

- **不要**把 `backend/.env`、数据库密码、OpenAI Key 配进 Vercel 前端环境变量（前端变量可能被打包进浏览器）。
- 密钥只放在**后端服务器**环境里。
- Vercel 项目权限：团队内尽量按需邀请，避免无关账号能改 Production 域名与 Rewrites。
- 公网仓库勿提交真实密钥；若曾泄露，轮换 Supabase / OpenAI 密钥。

---

## 附录 A：推荐的 `frontend/vercel.json` 完整示例

假设后端已按 AWS 手册部署在 `https://cowriting.example.com`：

```json
{
  "$schema": "https://openapi.vercel.sh/vercel.json",
  "rewrites": [
    {
      "source": "/api/:path*",
      "destination": "https://cowriting.example.com/api/:path*"
    },
    {
      "source": "/(.*)",
      "destination": "/index.html"
    }
  ]
}
```

把 `cowriting.example.com` 换成你的真实后端域名即可。

---

## 附录 B：一次成功部署的最短路径（复习用）

1. 后端已公网可访问：`curl https://后端/api/v1/workspaces`
2. GitHub 登录 Vercel，进入 [harry-w-projects](https://vercel.com/harry-w-projects)
3. Import 仓库 → **Root Directory = `frontend`** → Framework = **Vite**
4. 在 `frontend/vercel.json` 写好 `/api` 代理 + SPA fallback，push
5. Deploy → 等 Ready
6. 无痕打开 `*.vercel.app`，按第五部分清单验收

---

## 变更记录

| 日期 | 说明 |
| --- | --- |
| 2026-07-12 | 初版：面向零基础的 Vercel 前端部署（团队 harry-w-projects）+ 外部后端 rewrite |

---

*AI 论文共写工作台 · AIPM · Vercel*
