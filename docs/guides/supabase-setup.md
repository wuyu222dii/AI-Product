# Supabase 配置指南（cowriting-backend）

## 一、在 Supabase 控制台准备

1. 登录 [Supabase Dashboard](https://supabase.com/dashboard)，打开你的项目。
2. 左侧 **Project Settings** → **Database**：
   - 记下 **Database password**（忘记则点 **Reset database password** 生成新密码）。
   - 记下 **Project ref**（项目 URL 里 `https://<project-ref>.supabase.co` 那一段，例如 `lrxkcxhftjnkpolezuvh`）。
3. 左侧 **Connect**（或 Database 页的连接说明）：
   - 选择 **Session pooler**（IPv4 友好，适合本机 Spring Boot / `psql`）。
   - **不要**用仅 IPv6 的直连 `db.<project-ref>.supabase.co`（很多网络连不上）。
4. 从 Session pooler 连接信息中抄下：
   - **Host**（形如 `aws-1-ap-northeast-1.pooler.supabase.com`）
   - **Port**（Session 一般为 `5432`）
   - **Database**：`postgres`
   - **User**：`postgres.<project-ref>`（不是单独的 `postgres`）

## 二、配置本机 `backend/.env`

在 `backend` 目录执行：

```bash
cp .env.example .env
```

编辑 `.env`，填入 Dashboard 上的值：

```bash
SUPABASE_DB_HOST=<Session pooler 的 Host>
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres.<你的 project-ref>
SUPABASE_DB_PASSWORD=<数据库密码>
```

示例（请换成你自己的 host / ref / 密码）：

```bash
SUPABASE_DB_HOST=aws-1-ap-northeast-1.pooler.supabase.com
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres.lrxkcxhftjnkpolezuvh
SUPABASE_DB_PASSWORD=你的真实密码
```

注意：

- `.env` 已在 `.gitignore` 中，勿提交到 Git。
- **不要**在系统环境变量里设置 `SPRING_DATASOURCE_USERNAME=postgres`，否则会认证失败。
- 只使用 `SUPABASE_DB_*` 即可；`application.yml` 已对接。

## 三、后端如何读取配置

`application.yml` 会通过 `optional:file:.env` 加载上述变量，并组装 JDBC：

- URL：`jdbc:postgresql://{HOST}:{PORT}/{NAME}?sslmode=require&currentSchema=public`
- 用户名 / 密码：来自 `SUPABASE_DB_USER`、`SUPABASE_DB_PASSWORD`

启动（必须在 `backend` 目录，才能找到 `.env`）：

```bash
cd backend
mvn spring-boot:run
```

成功时日志会出现：`Started CowritingBackendApplication`。

## 四、用 psql 验证连接（可选）

macOS 若提示 `psql: command not found`，可用 Homebrew 的 libpq：

```bash
brew install libpq
export PATH="/opt/homebrew/opt/libpq/bin:$PATH"
```

连接（使用 pooler，与 `.env` 一致）：

```bash
cd backend
export $(grep -v '^#' .env | xargs)
psql "postgresql://${SUPABASE_DB_USER}:${SUPABASE_DB_PASSWORD}@${SUPABASE_DB_HOST}:${SUPABASE_DB_PORT}/${SUPABASE_DB_NAME}?sslmode=require"
```

能进入 `postgres=#` 说明账号、密码、host 正确。

## 五、初始化表结构（可选）

项目根目录有 [postgresql_schema.sql](../../postgresql_schema.sql)。开发阶段 JPA 已设置 `ddl-auto: update`，一般会自动建表。

若要在 Supabase SQL Editor 手动执行，可复制该文件内容到 Dashboard → **SQL** → New query 运行。

## 六、常见错误

| 现象 | 处理 |
|------|------|
| `password authentication failed for user "postgres"` | **密码错误**（最常见）：Dashboard → Database → **Reset database password**，把新密码写入 `.env` 的 `SUPABASE_DB_PASSWORD`，不要用旧密码 |
| `Unable to determine Dialect without JDBC metadata` | 通常是连不上库时的连带报错，先按上一条修密码/连接，不必单独配 dialect |
| `tenant/user ... not found` | pooler **区域** 或 **project ref** 与 Dashboard 不一致，重新复制 Connect 里的 Host / User |
| `The connection attempt failed`（直连 `db.*.supabase.co`） | 改用 Session pooler Host |
| 启动报找不到 `SUPABASE_DB_*` | 在 `backend` 目录启动，并确认存在 `backend/.env` |
| `psql: command not found` | 安装 `libpq` 并把其 `bin` 加入 `PATH`（见第四节） |
| `max clients reached in session mode` / `EMAXCONNSESSION` | Session pooler 总连接数有限（常见 15）。先结束多余的 `mvn spring-boot:run` / Java 进程；`application.yml` 已把 Hikari 池设为 5，勿再调大 |

## 七、检查清单

- [ ] Dashboard 已拿到 Session pooler 的 Host、Port、User、Password
- [ ] `backend/.env` 五项均已填写且与 Dashboard 一致
- [ ] 未在 shell 里设置 `SPRING_DATASOURCE_USERNAME=postgres`
- [ ] `cd backend && mvn spring-boot:run` 能正常启动
