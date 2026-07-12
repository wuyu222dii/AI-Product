# AI 论文共写工作台 — AWS EC2 部署操作手册（零基础版）

本文教你：**不改 GitHub 仓库里的代码**，在一台 **AWS EC2** 上部署本项目，得到一个 **`https://你的域名`** 链接，发给老师和同学。

**原理（一句话）：** 用 **Nginx** 让「网站页面」和「后端 API」共用**同一个域名**。前端代码里请求的是相对路径 `/api/v1/...`，和页面同源，浏览器**不跨域**，因此**不需要**改仓库里的 CORS 或前端 API 地址。

相关本地文档：

- [DEMO_GUIDE.md](DEMO_GUIDE.md) — 本地跑通与演示路径
- [supabase-setup.md](supabase-setup.md) — Supabase 数据库连接怎么抄

---

## 你需要提前准备的东西

| 项目 | 说明 |
| --- | --- |
| **AWS 账号** | [https://aws.amazon.com](https://aws.amazon.com) 注册（学生可用 Educate / 学校额度） |
| **一张信用卡** | AWS 验证用（`t2.micro` / `t3.micro` 免费套餐内通常几乎不扣费，仍建议设账单告警） |
| **域名（强烈建议）** | 例如 `cowriting.yourname.com`；没有域名也能用公网 IP 先 HTTP 演示，但 HTTPS 和正式分享会麻烦很多 |
| **SSH 客户端** | Mac/Linux 自带 `ssh`；Windows 用 PowerShell 或 [PuTTY](https://www.putty.org/) |
| **本机已能跑通项目** | 本地 `mvn spring-boot:run` + `npm run dev` 成功一次（确认 Supabase、OpenAI 配置有效） |
| **密钥材料** | 从本机 `backend/.env` 或团队密码库拿到：`SUPABASE_DB_*`、`OPENAI_*`（见下方模板） |
| **GitHub 仓库** | 本仓库：`https://github.com/wuyu222dii/AI-Product.git`（若你 fork 了，换成你的地址） |

**预计总耗时：** 第一次约 **3–5 小时**（含等 DNS、证书）。

---

## 原理图（部署后长什么样）

```text
浏览器访问 https://YOUR_DOMAIN
            │
            ▼
        EC2 上的 Nginx（:443）
            ├── /          → 前端静态文件（Vite build 出的 dist）
            └── /api/...   → 反代到本机 Spring Boot（:8080）
                                  ├── Supabase PostgreSQL
                                  └── OpenAI API
```

仓库**不用改**：前端已写死 `API_BASE = "/api/v1"`；生产只要 Nginx 把 `/api` 转到后端即可。

---

## 第一部分：创建 EC2 服务器

### 步骤 1.1 登录 AWS 控制台

1. 打开 [AWS 控制台](https://console.aws.amazon.com/)。
2. 右上角选择区域，建议选离新西兰近的，例如 **Asia Pacific (Sydney) `ap-southeast-2`**（延迟更低）。  
   若你的 Supabase 在东京 `ap-northeast-1`，也可选东京，数据库延迟会更友好。

### 步骤 1.2 进入 EC2

1. 顶部搜索框输入 **EC2**，点击进入 **EC2 Dashboard**。
2. 左侧点击 **Instances（实例）** → 橙色按钮 **Launch instance（启动实例）**。

### 步骤 1.3 配置实例（逐项填写）

**Name（名称）：** `aipm-cowriting`（随意）

**Application and OS Images（镜像）：**

- 选 **Ubuntu**
- 版本选 **Ubuntu Server 22.04 LTS (HVM), SSD Volume Type**
- 架构 **64-bit (x86)**

**Instance type（实例类型）：**

- 免费套餐可先试 **t2.micro** 或 **t3.micro**
- **强烈建议演示用至少 `t3.small`（2GB）或 `t3.medium`（4GB）**：本项目要跑 Java，1GB 很容易内存不够导致后端反复重启
- 作业演示卡顿时再临时升配（可能产生费用）

**Key pair（密钥对）—— 非常重要：**

1. 点击 **Create new key pair**。
2. 名称填：`aipm-key`。
3. 类型：**RSA**。
4. 格式：Mac/Linux 选 **`.pem`**；Windows 也建议先下 `.pem`。
5. 点击 **Create key pair**，浏览器会下载 **`aipm-key.pem`**。
6. **把该文件放到安全位置**（例如 `~/Downloads/aipm-key.pem`），**不要**上传到 GitHub、不要发给陌生人。
7. Mac/Linux 终端执行（路径按你实际修改）：

```bash
chmod 400 ~/Downloads/aipm-key.pem
```

**Network settings（网络）—— 点击 Edit：**

勾选：

- **Allow SSH traffic from**：选 **My IP**（仅你当前 IP 能 SSH，更安全）。
- **Allow HTTP traffic from the internet**（端口 80）。
- **Allow HTTPS traffic from the internet**（端口 443）。

**不要**额外开放 **8080** 到公网（后端只给本机 Nginx 用）。

**Configure storage（磁盘）：**

- 默认 **8 GiB** 偏紧（要装 JDK、Maven、Node、构建缓存）；**改成 20 GiB** 更稳。

### 步骤 1.4 启动并记下公网 IP

1. 右侧 **Summary** 点击 **Launch instance**。
2. 等待 **Instance state** 变为 **Running**（约 1–2 分钟），**Status checks** 变为 **2/2 checks passed**。
3. 点进该实例，在详情里找到：
   - **Public IPv4 address**（例如 `3.105.xxx.xxx`）→ 记为 **`EC2_IP`**
   - **Public DNS**（例如 `ec2-3-105-xxx-xxx.ap-southeast-2.compute.amazonaws.com`）

### 步骤 1.5（推荐）分配弹性 IP Elastic IP

EC2 **停止再启动** 后公网 IP 可能会变，域名会失效。

1. 左侧 **Network & Security** → **Elastic IPs**。
2. **Allocate Elastic IP address** → **Allocate**。
3. 选中刚分配的 IP → **Actions** → **Associate Elastic IP address**。
4. 选择你的实例 `aipm-cowriting` → **Associate**。
5. 之后用 **这个 Elastic IP** 作为 `EC2_IP`。

计费提示：IP **绑在正在运行的实例上**通常不额外收费；分配了却没绑定、或实例停了仍占着 IP，可能产生小额费用。不用时记得 **Release**。

---

## 第二部分：域名指向 EC2（强烈建议）

没有域名时，HTTPS 证书申请会困难很多；发给老师时用 IP 也不够正式。

### 步骤 2.1 在域名服务商添加 DNS 记录

在你买域名的地方（GoDaddy、Cloudflare、Namecheap、阿里云等）：

| 类型 | 名称 | 值 | TTL |
| --- | --- | --- | --- |
| **A** | `@` 或子域名如 `cowriting` | 你的 **`EC2_IP`** | 300 或默认 |

示例：若你要的地址是 `https://cowriting.example.com`，则添加：

- 类型 **A**，名称 **cowriting**，值 **EC2_IP**

等待 **5 分钟～几小时** 生效。本机测试：

```bash
ping cowriting.example.com
```

应解析到你的 EC2 IP。

下文用 **`YOUR_DOMAIN`** 代替你的完整域名，例如 `cowriting.example.com`。  
公网访问地址：**`https://YOUR_DOMAIN`**

> 若暂时没有域名：可先跳过第二部分与第九部分，用 `http://EC2_IP` 做 HTTP 演示；Nginx 的 `server_name` 写成 `_` 即可。发给老师前仍建议补上域名 + HTTPS。

---

## 第三部分：SSH 登录服务器

### 步骤 3.1 Mac / Linux

```bash
ssh -i ~/Downloads/aipm-key.pem ubuntu@EC2_IP
```

把 `EC2_IP` 换成弹性 IP 或公网 IP。

第一次会问 `Are you sure...`，输入 `yes`。

成功则提示符类似：`ubuntu@ip-172-31-xx-xx:~$`

### 步骤 3.2 Windows（PowerShell）

```powershell
ssh -i C:\Users\你的用户名\Downloads\aipm-key.pem ubuntu@EC2_IP
```

若权限错误，在「密钥文件属性 → 安全」里去掉其他用户的读取权限。

### 步骤 3.3 登录失败时检查

- 安全组是否放行 **22** 且来源包含你的 IP（换网络后「My IP」会变，需回控制台改入站规则）。
- 用户名必须是 **`ubuntu`**（Ubuntu 镜像默认用户；Amazon Linux 才是 `ec2-user`）。
- IP 是否已变（若没用 Elastic IP）。
- 私钥是否执行过 `chmod 400`。

---

## 第四部分：在服务器上安装软件

以下命令均在 **SSH 登录后的 EC2** 上执行。

### 步骤 4.1 更新系统

```bash
sudo apt update
sudo apt upgrade -y
```

### 步骤 4.2 安装 JDK 17

```bash
sudo apt install -y openjdk-17-jdk
java -version
```

应看到 `openjdk version "17.x.x"`。

### 步骤 4.3 安装 Maven

```bash
sudo apt install -y maven
mvn -version
```

### 步骤 4.4 安装 Node.js 20（用 NodeSource）

```bash
curl -fsSL https://deb.nodesource.com/setup_20.x | sudo -E bash -
sudo apt install -y nodejs
node -v
npm -v
```

### 步骤 4.5 安装 Nginx 与 Git

```bash
sudo apt install -y nginx git
sudo systemctl enable nginx
sudo systemctl start nginx
```

此时浏览器打开 `http://EC2_IP`，若看到 Nginx 默认欢迎页，说明 **80 端口与安全组已通**（重要里程碑）。

### 步骤 4.6 安装 Certbot（HTTPS 证书，稍后用）

```bash
sudo apt install -y certbot python3-certbot-nginx
```

---

## 第五部分：把项目代码放到服务器上

**不要改 GitHub** 也可以：在服务器上 `git clone` 即可。敏感配置只写在服务器本地文件，**不要** `git commit` 上去。

### 步骤 5.1 克隆仓库

```bash
cd ~
git clone https://github.com/wuyu222dii/AI-Product.git
cd AI-Product
```

若仓库是私有的，按 GitHub 提示用 Personal Access Token 或 Deploy Key，**不要**把 token 写进文档或提交历史。

确认在 **`main`** 分支：

```bash
git branch
git pull origin main
```

### 步骤 5.2 在服务器上创建「仅本机使用」的后端 `.env`（不提交 Git）

本项目后端通过 `application.yml` 的 `optional:file:.env` 读取环境变量，**工作目录里必须有 `.env`**。

```bash
sudo mkdir -p /opt/aipm
cd ~/AI-Product/backend
cp .env.example .env
nano .env
```

按你本机已跑通的配置填写（**把占位符换成真实值**）：

```bash
SUPABASE_DB_HOST=aws-1-ap-northeast-1.pooler.supabase.com
SUPABASE_DB_PORT=5432
SUPABASE_DB_NAME=postgres
SUPABASE_DB_USER=postgres.你的project-ref
SUPABASE_DB_PASSWORD=你的数据库密码
HIBERNATE_DDL_AUTO=none
OPENAI_API_KEY=sk-你的密钥
OPENAI_BASE_URL=https://api.openai.com/v1
OPENAI_MODEL=gpt-4o-mini
OPENAI_TIMEOUT_SECONDS=60
```

保存：`Ctrl+O` 回车，`Ctrl+X` 退出。

```bash
chmod 600 .env
```

**说明：**

- `.env` 已在 `.gitignore` 中，正常不会进 GitHub；仍请确认不要手动 `git add .env`。
- Host 请用 Supabase **Session pooler**（IPv4 友好），抄法见 [supabase-setup.md](supabase-setup.md)。
- `HIBERNATE_DDL_AUTO` 生产必须为 `none`；表结构用仓库里 `supabase/migrations/` 管理，不要在 EC2 上开自动建表。

也可把一份副本放到统一目录备份（可选）：

```bash
sudo cp ~/AI-Product/backend/.env /opt/aipm/.env
sudo chmod 600 /opt/aipm/.env
```

systemd 仍以 `WorkingDirectory=.../backend` 为准（见下一部分）。

---

## 第六部分：编译并配置后端

### 步骤 6.1 打包后端

```bash
cd ~/AI-Product/backend
mvn -DskipTests package
```

成功后确认 jar 名：

```bash
ls -lh target/*.jar
```

一般为：`target/cowriting-backend-0.0.1-SNAPSHOT.jar`（以 `pom.xml` 为准）。

### 步骤 6.2 创建 systemd 服务（开机自启 + 后台运行）

```bash
sudo nano /etc/systemd/system/aipm-backend.service
```

粘贴（若 jar 文件名不同，改 `ExecStart` 那一行）：

```ini
[Unit]
Description=AIPM Cowriting Spring Boot API
After=network.target

[Service]
Type=simple
User=ubuntu
WorkingDirectory=/home/ubuntu/AI-Product/backend
ExecStart=/usr/bin/java -Xms256m -Xmx768m -jar /home/ubuntu/AI-Product/backend/target/cowriting-backend-0.0.1-SNAPSHOT.jar
Restart=on-failure
RestartSec=10
SuccessExitStatus=143

[Install]
WantedBy=multi-user.target
```

**要点：**

- `WorkingDirectory` 必须是放了 `.env` 的 `backend` 目录，否则读不到数据库/OpenAI 配置。
- `-Xmx768m` 适合 small/medium；若是 `t3.micro` 可改为 `-Xmx512m`，但仍可能 OOM。

保存后执行：

```bash
sudo systemctl daemon-reload
sudo systemctl enable aipm-backend
sudo systemctl start aipm-backend
sudo systemctl status aipm-backend
```

**应显示 `active (running)`。** 若失败：

```bash
sudo journalctl -u aipm-backend -n 80 --no-pager
```

根据日志排查（常见：`.env` 缺失、Supabase 账号密码错、连不上 pooler、内存不足被杀）。

### 步骤 6.3 确认后端在本机 8080 监听

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1:8080/api/v1/workspaces
ss -lntp | grep 8080
```

返回 `200`、`401` 或带 JSON 的业务错误码均可，说明进程在监听。  
若是 `Connection refused`，说明服务没起来，回去看 `journalctl`。

---

## 第七部分：构建前端静态文件

本项目前端生产请求使用相对路径 `/api/v1`，**一般不需要**再写 `VITE_API_BASE_URL`。同域 Nginx 即可。

在 EC2 上执行：

```bash
cd ~/AI-Product/frontend
npm install
npm run build
```

成功会生成 **`dist/`** 目录，确认：

```bash
ls -la dist
```

应有 `index.html` 和 `assets/`。

### 步骤 7.1 把静态文件放到 Nginx 目录

```bash
sudo mkdir -p /var/www/aipm
sudo rm -rf /var/www/aipm/*
sudo cp -r dist/* /var/www/aipm/
sudo chown -R www-data:www-data /var/www/aipm
```

---

## 第八部分：配置 Nginx（同域反代，无需改 CORS 代码）

### 步骤 8.1 先写 HTTP 配置（证书申请前）

```bash
sudo nano /etc/nginx/sites-available/aipm
```

粘贴（**把 `YOUR_DOMAIN` 换成真实域名**；若暂无域名，把 `server_name` 写成 `_`）：

```nginx
server {
    listen 80;
    server_name YOUR_DOMAIN;

    root /var/www/aipm;
    index index.html;

    # 前端 SPA：刷新子路由不 404
    location / {
        try_files $uri $uri/ /index.html;
    }

    # 后端 API（与仓库 RestConstants.API_V1 = /api/v1 一致）
    location /api/ {
        proxy_pass http://127.0.0.1:8080;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto $scheme;

        # 上传、AI 生成可能较慢
        proxy_connect_timeout 60s;
        proxy_send_timeout 300s;
        proxy_read_timeout 300s;
        client_max_body_size 50m;
    }
}
```

启用站点并测试：

```bash
sudo ln -sf /etc/nginx/sites-available/aipm /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default
sudo nginx -t
sudo systemctl reload nginx
```

浏览器访问：

- 有域名：`http://YOUR_DOMAIN`
- 无域名：`http://EC2_IP`

应能看到论文共写工作台页面（尚未 HTTPS 也可先测页面是否出来）。

本机再确认 API 经 Nginx 可达：

```bash
curl -s -o /dev/null -w "%{http_code}\n" http://127.0.0.1/api/v1/workspaces
```

若这里是 **502**，后端未启动或未听 8080；若这里正常但浏览器不行，再查 DNS/安全组。

---

## 第九部分：申请 HTTPS 证书（Let's Encrypt）

**前提：** 域名 `YOUR_DOMAIN` 的 DNS 已指向 EC2，且 80 端口可从公网访问。

```bash
sudo certbot --nginx -d YOUR_DOMAIN
```

按提示：

1. 输入邮箱（用于证书到期提醒）。
2. 同意服务条款。
3. 是否重定向 HTTP→HTTPS：选 **2 (Redirect)** 推荐。

成功后浏览器访问：**`https://YOUR_DOMAIN`**

证书自动续期一般已配置；可检查：

```bash
sudo certbot renew --dry-run
```

---

## 第十部分：核对密钥与外部服务（必做）

本项目没有 Spotify/Google OAuth，但有两处外部依赖必须在服务器上可用。

### 10.1 Supabase

对照 [supabase-setup.md](supabase-setup.md) 确认：

- 用的是 **Session pooler** Host，不是仅 IPv6 的直连 Host
- 用户名是 `postgres.<project-ref>`
- Schema 迁移已在 Supabase 执行过（本地能跑通常说明库已就绪）

从 EC2 测出网到数据库（可选，需先装客户端）：

```bash
sudo apt install -y postgresql-client
# 按 .env 里的值拼连接串测试
```

### 10.2 OpenAI

确认 `.env` 中 `OPENAI_API_KEY` / `OPENAI_BASE_URL` / `OPENAI_MODEL` 与本地一致。

从 EC2 测出网：

```bash
curl -I https://api.openai.com
```

### 10.3 必须一致的三处（对照表）

| 位置 | 应满足 |
| --- | --- |
| 浏览器地址栏 | `https://YOUR_DOMAIN`（或临时 `http://EC2_IP`） |
| 前端请求路径 | `/api/v1/...`（仓库已写好，不用改） |
| Nginx | `location /api/` → `http://127.0.0.1:8080` |

不要把前端指到另一个域名的后端（否则会出现 CORS，然后又想去改仓库代码——这正是本文要避免的）。

---

## 第十一部分：完整验收（发给老师前自己做一遍）

用**无痕窗口**打开 **`https://YOUR_DOMAIN`**（或 `http://EC2_IP`）：

- [ ] 页面能打开，无白屏、不是 Nginx 默认页
- [ ] 刷新某个子页面路由，不出现 Nginx 404
- [ ] 浏览器 F12 → **Network**：请求发往 `https://YOUR_DOMAIN/api/v1/...`，**无 CORS 错误**、无大面积 502
- [ ] 能创建研究项目 / 工作区（证明数据库通）
- [ ] 能上传一份小文本或样例材料（见仓库 `docs/samples/`）
- [ ] 能触发一次材料解析或列表刷新
- [ ] （若配置了 OpenAI）能完成一次需要 AI 的操作（解析 / 生成 / 共写之一）
- [ ] 能进入章节工作台主流程（对照 [DEMO_GUIDE.md](DEMO_GUIDE.md)）

**播放/导出慢或超时：** 属正常可能；Nginx 已设较长 `proxy_read_timeout`。仍失败时看：

```bash
sudo journalctl -u aipm-backend -n 100 --no-pager
sudo tail -n 50 /var/log/nginx/error.log
```

---

## 第十二部分：以后更新代码怎么做

在 EC2 上：

```bash
cd ~/AI-Product
git pull origin main

cd backend
mvn -DskipTests package
sudo systemctl restart aipm-backend

cd ../frontend
npm install
npm run build
sudo rm -rf /var/www/aipm/*
sudo cp -r dist/* /var/www/aipm/
sudo chown -R www-data:www-data /var/www/aipm
sudo systemctl reload nginx
```

若只改了密钥：

```bash
nano ~/AI-Product/backend/.env
sudo systemctl restart aipm-backend
```

---

## 第十三部分：常见问题

| 现象 | 可能原因 | 处理 |
| --- | --- | --- |
| SSH 连不上 | 安全组 / IP 变更 / 密钥权限 | 检查 1.3、3.3；SSH 来源改回 My IP |
| `502 Bad Gateway` | 后端未启动 | `sudo systemctl status aipm-backend` + `journalctl` |
| 页面仍是 Nginx 欢迎页 | 没删 default 站点或没复制 dist | 重做 7.1、8.1 |
| 页面 404 / 刷新子路由 404 | `try_files` 未配或 root 错 | 检查 8.1 |
| API CORS 错误 | 前后端用了两个不同域名 | 必须用本文「同域 Nginx」；不要改仓库硬编码第三方 API 域名 |
| 后端启动即退出 | `.env` 不在 WorkingDirectory / 数据库认证失败 | 确认 `backend/.env`；对照 supabase-setup |
| 数据库连接超时 | 用了 IPv6 直连 Host | 改用 Session pooler |
| AI 调用失败 | Key 无效或出网受限 | 查 `OPENAI_*`；`curl -I https://api.openai.com` |
| 上传大文件失败 | body 过大 | 确认 Nginx `client_max_body_size 50m` 后 reload |
| 证书申请失败 | DNS 未生效 | `ping YOUR_DOMAIN` 核对 IP；安全组放行 80 |
| 实例很卡 / 后端反复重启 | 内存太小（micro） | 升到 `t3.small`/`t3.medium`，或降低 `-Xmx` |

---

## 第十四部分：费用与安全提醒

- **关闭不用的 EC2**（控制台 → Instance state → **Stop**）或设账单告警，避免忘记开机产生费用。
- 彻底不用时：**Terminate** 实例，并 **Release** 未绑定的 Elastic IP。
- **不要**把 `.pem` 密钥、`.env`、数据库密码、OpenAI Key 发到公开聊天或提交 Git。
- 公网仓库里若曾误提交真实密钥，课程/演示结束后建议在 Supabase / OpenAI 控制台**轮换密钥**。
- 安全组 SSH 尽量保持 **My IP**，不要对全世界开放 22。

---

## 发给老师/同学的一句话模板

> AI 论文共写工作台在线演示：  
> **https://YOUR_DOMAIN**  
> 建议使用 Chrome；首次进入可按演示路径创建研究项目 → 上传样例材料 → 进入学术文档章节工作台。  
> 仓库与本地运行说明见：  
> https://github.com/wuyu222dii/AI-Product  

（无域名时把链接换成 `http://EC2_IP`，并说明当前为 HTTP 临时演示。）

---

## 附录：一次成功部署的最短路径（复习用）

1. AWS 选区域 → 创建 `.pem` → `chmod 400`
2. 启动 Ubuntu 22.04，建议 `t3.small`+，20GB，安全组开 22（My IP）+ 80 + 443
3. 分配并关联 Elastic IP；域名 A 记录指向该 IP
4. SSH → 装 JDK17、Maven、Node20、Nginx、Certbot、Git
5. `git clone` → 在 `backend/.env` 填好 Supabase / OpenAI
6. `mvn package` → 配 `aipm-backend.service` 并启动
7. `npm run build` → 复制到 `/var/www/aipm`
8. Nginx 同域反代 `/api/` → Certbot 申请 HTTPS
9. 无痕窗口按第十一部分清单验收一遍

---

## 变更记录

| 日期 | 说明 |
| --- | --- |
| 2026-07-12 | 初版与新手扩充版 |
| 2026-07-12 | 按「零基础版」结构重写：Ubuntu + 同域 Nginx + HTTPS + 验收模板（对齐课程部署手册体例） |

---

*AI 论文共写工作台 · AIPM · AWS EC2*
