# Supabase Auth 配置与验收指南

更新时间：`2026-07-21`

本项目已完成 Google OAuth、6 位邮箱 OTP、JWT 校验和用户数据隔离代码。Google、Resend 和 Supabase Dashboard 中的私密配置必须由项目所有者完成，不写入仓库。

参考文档：[Supabase Google OAuth](https://supabase.com/docs/guides/auth/social-login/auth-google)、[Supabase Custom SMTP](https://supabase.com/docs/guides/auth/auth-smtp)、[Supabase Email Templates](https://supabase.com/docs/guides/auth/auth-email-templates)、[Supabase JWT](https://supabase.com/docs/guides/auth/jwts)、[Resend SMTP](https://resend.com/docs/send-with-smtp)。

## 1. 前后端环境变量

前端创建 `frontend/.env.local`：

```bash
VITE_SUPABASE_URL=https://<project-ref>.supabase.co
VITE_SUPABASE_PUBLISHABLE_KEY=<publishable-key>
VITE_API_PROXY_TARGET=http://127.0.0.1:8080
```

publishable key 可以出现在浏览器中；不得放入 `service_role`、secret key、数据库密码或 Google Client Secret。

后端 `backend/.env` 增加：

```bash
SUPABASE_URL=https://<project-ref>.supabase.co
SUPABASE_JWT_AUDIENCE=authenticated
APP_ENV=development
APP_LEGACY_DEMO_OWNER_ID=
```

Spring Boot 使用 `${SUPABASE_URL}/auth/v1/.well-known/jwks.json` 验证 ES256 JWT，不保存 JWT Secret。

## 2. URL 配置

在 Supabase Dashboard 的 Authentication URL Configuration 中设置：

- 本地 Site URL：`http://localhost:5173`
- 本地 Redirect URL：`http://localhost:5173/auth/callback`
- 生产 Site URL：真实 HTTPS 域名
- 生产 Redirect URL：`https://<your-domain>/auth/callback`

不要使用通配到外部域名的 Redirect URL。前端 `returnTo` 只接受以 `/app` 开头的站内路径。

## 3. Google 登录

1. 在 Google Auth Platform 创建 Web application OAuth client。
2. Authorized JavaScript origins 加入 `http://localhost:5173` 和生产站点 Origin。
3. Authorized redirect URIs 加入 Supabase Dashboard Google Provider 页面给出的回调地址，通常为：

```text
https://<project-ref>.supabase.co/auth/v1/callback
```

4. 在 Supabase Authentication Providers 的 Google 配置中填入 Client ID 和 Client Secret，并启用 Provider。
5. Google 侧最小 scopes 使用 `openid`、email 和 profile。

应用前端调用 `signInWithOAuth`，OAuth 完成后回到 `/auth/callback`，再恢复原 `/app` 路径。

## 4. Resend 自定义 SMTP

Supabase 默认邮件服务只适合受限测试，真实用户 OTP 必须使用自定义 SMTP。

1. 在 Resend 验证发送域名并创建 API Key。
2. 在 Supabase Authentication SMTP Settings 中启用 Custom SMTP。
3. 建议配置：

| 字段 | 值 |
| --- | --- |
| Host | `smtp.resend.com` |
| Port | `465` |
| Username | `resend` |
| Password | Resend API Key |
| Sender email | 已验证域名下的地址，例如 `auth@example.com` |
| Sender name | `AI 论文共写工作台` |

也可使用 `587 + STARTTLS`。Resend API Key 只保存在 Dashboard，不进入前后端环境变量。

## 5. 6 位 OTP 邮件模板

在 Supabase Authentication Email Templates 的 Magic Link / OTP 模板中直接展示：

```html
<h2>登录 AI 论文共写工作台</h2>
<p>你的 6 位验证码是：</p>
<p style="font-size: 28px; font-weight: 700; letter-spacing: 4px;">{{ .Token }}</p>
<p>验证码仅用于本次登录，请勿转发。</p>
```

必须使用 `{{ .Token }}`，否则用户会收到链接而不是当前界面所需的 6 位数字。前端通过 `verifyOtp({ type: "email" })` 验证。

## 6. 数据库迁移

迁移文件：

```text
supabase/migrations/20260721002107_user_auth_and_workspace_isolation.sql
```

当前真实数据库已执行并登记该迁移。新环境使用：

```bash
npx supabase link --project-ref <project-ref>
npx supabase db push
npx supabase db advisors --type all --level warn
```

迁移完成后应满足：

- `user_profiles.id` 外键指向 `auth.users.id`。
- 新 workspace 必须拥有 `user_id`。
- 历史 Demo workspace 为 `user_id=null + legacy_unowned=true`。
- `anon/authenticated` 对业务表没有直接授权。
- 应用仍由 Spring Boot 数据库连接访问业务表。

## 7. 历史项目管理员归属

系统没有公开认领接口。确认目标 Auth 用户 UUID 后，仅由管理员执行：

```sql
begin;

update public.workspaces
set user_id = '<auth-user-uuid>'::uuid,
    legacy_unowned = false,
    updated_at = now()
where id in ('<workspace-uuid>'::uuid);

commit;
```

开发环境也可用 `APP_LEGACY_DEMO_OWNER_ID=<auth-user-uuid>` 临时查看所有未归属 Demo 项目；`APP_ENV=production` 时该配置自动失效。

## 8. 验收清单

1. Google 登录后进入 `/app/projects`，刷新页面仍保持会话。
2. 邮箱收到 6 位验证码，粘贴后可登录。
3. 同一邮箱分别使用 Google 和 OTP 登录时，Supabase Users 中应为同一用户 ID。
4. 未登录请求 `/api/v1/workspaces` 返回 `401`。
5. 用户 A 创建项目和上传文件后，用户 B 列表中不可见。
6. 用户 B 猜测 A 的 workspace、material、section、job 或 export UUID 时均返回 `404`。
7. 用户 A 退出后，用户资料状态和临时 Blob URL 被清理。
8. Supabase Auth Logs 与 Resend 邮件记录无异常。

当前仓库未配置 Google/Resend 私密凭据，以上第 1-3、5-8 项需要完成 Dashboard 配置后用两个真实账号执行。

## 9. 常见问题

### 登录页提示尚未配置 Supabase

检查 `frontend/.env.local` 是否包含 URL 和 publishable key，重启 Vite。

### Google 返回 redirect_uri_mismatch

Google 的 Redirect URI 应是 Supabase Auth callback，不是前端 `/auth/callback`；前端地址配置在 Supabase Redirect URLs 中。

正确对照：

| 位置 | 应填写 |
| --- | --- |
| Google Authorized redirect URIs | `https://<project-ref>.supabase.co/auth/v1/callback` |
| Google Authorized JavaScript origins | `http://localhost:5173` 与生产 Origin |
| Supabase Site URL | `http://localhost:5173`（生产改为 HTTPS 域名） |
| Supabase Redirect URLs | `http://localhost:5173/auth/callback` 与生产 `/auth/callback` |

### Google 已授权，但回调页提示「没有恢复到有效登录会话」

常见原因：

1. **PKCE 授权码被消费两次**：开发模式下 React StrictMode 会挂载回调页两次；第二次 `/token` 会返回 `invalid flow state`。
2. Supabase 把用户送回 **Site URL（`/`）** 而不是 `/auth/callback`。

当前代码已处理：

- `detectSessionInUrl: false`，只由回调页换码
- `exchangeOAuthCode` 对同一 `code` 做单次 in-flight 去重
- `OAuthCodeCatcher` 会把 `/?code=...` 转发到 `/auth/callback`

请硬刷新后再试 Google 登录。若仍失败，打开浏览器地址栏，确认最终出现：

```text
http://localhost:5173/auth/callback?code=...
```

并在 Supabase Authentication → URL Configuration 中加入：

```text
http://localhost:5173/auth/callback
```

### 收不到验证码

检查 Resend 域名验证、SMTP API Key、Sender address、Supabase Auth Logs，以及模板中是否使用 `{{ .Token }}`。

### API 返回 401 / 页面提示「登录已失效，请重新登录」

前端 Google 会话存在、但 `/api/v1/me` 等接口 401，通常是后端 JWT 校验失败：

1. 确认 `backend` 已启动（`mvn spring-boot:run`）。后端未启动时也会出现接口失败。
2. 确认 `backend/.env` 的 `SUPABASE_URL` 与前端 `VITE_SUPABASE_URL` 指向同一项目。
3. Supabase 当前签发 **ES256** JWT。后端 `SecurityConfig` 必须在 `NimbusJwtDecoder` 上显式允许 `ES256`；若只接受默认 RS256，会出现「前端已登录、接口全 401」。
