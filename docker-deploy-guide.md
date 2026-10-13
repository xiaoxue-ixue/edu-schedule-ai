# 智能教务排课系统 —— Docker Compose 部署指南

> 三容器全栈一键启动：**MySQL 8 + Spring Boot 后端 + Nginx Vue 前端**
>
> 预计首次构建耗时 5~10 分钟（Maven/npm 下载依赖），之后重新构建 1~3 分钟（Docker 层缓存）。

---

## 一、前置条件（1 分钟检查）
| 工具 | 要求 | 验证命令 |
|------|------|---------|
| Docker Desktop（Windows）| >= 4.20；Linux 内核模式（WSL2 或 Hyper-V）| `docker --version` → 有输出 |
| Docker Compose V2 | 随 Docker Desktop 自带 | `docker compose version` → v2.x |
| 磁盘剩余空间 | >= 20 GB（镜像 + MySQL 数据）| 我的电脑看 C 盘 / D 盘都行，Docker 默认存 C 盘 |
| 端口占用 | **本机 80 / 3307 / 8080** 三个端口空闲（见第 4 节如何检查）| 见下 |

---

## 二、文件放对位置（必做！）
用 README 里给的 5 条 Copy-Item 命令把文件复制完后，你的目录结构**必须**长这样（否则 build 会报找不到 Dockerfile）：

```
D:\TRAE SOLO CN\edu-schedule\      ← 项目根（docker-compose.yml 就在这里）
├── .env.example
├── .env                            ← 你把 .env.example 复制一份改名，改 DB_PWD / JWT_SECRET / AI Key
├── docker-compose.yml              ← 三容器编排（本指南的核心）
├── docker-deploy-guide.md          ← 本指南
│
├── docker/
│   └── mysql-init/
│       └── edu_schedule_ai_YYYYMMDD.sql   ← ⚠️ 建议：本地 DB 导出的 .sql 放这里，首次启动自动导入
│
├── edu-schedule-ai/                ← 后端目录
│   ├── Dockerfile                  ← 后端 Dockerfile（Maven→JRE17）
│   ├── .dockerignore
│   ├── pom.xml
│   └── src/
│
└── edu-schedule-ai-web/            ← 前端目录
    ├── Dockerfile                  ← 前端 Dockerfile（Node→Nginx）
    ├── nginx.conf                  ← 关键：Vue history 模式 + /api 反代 backend
    ├── .dockerignore
    ├── package.json
    └── src/
```

---

## 三、开始部署（5 步，复制粘贴即可）

### Step 0：检查端口（PS 里跑）
如果某端口被占，要么 kill 占用进程，要么改 `docker-compose.yml` 里的 ports 映射：
```powershell
# 检查 80 / 8080 / 3307 三个端口
"80", "8080", "3307" | ForEach-Object {
    $p = Get-NetTCPConnection -LocalPort $_ -ErrorAction SilentlyContinue | Select-Object -ExpandProperty OwningProcess -Unique
    if ($p) { Write-Host "❌ 端口 $_ 被占用，PID=$p"; Get-Process -Id $p | Select-Object Id, ProcessName, Path }
    else { Write-Host "✔ 端口 $_ 空闲" }
}
```

---

### Step 1：准备 .env
```powershell
# 在项目根目录
cd "D:\TRAE SOLO CN\edu-schedule"

# 从模板复制
Copy-Item .env.example .env

# 打开记事本编辑，改 DB_PWD / JWT_SECRET / EDU_SILICONFLOW_API_KEY 三个值
notepad .env
```

JWT_SECRET 生成一条 64 位随机串（直接复制这条，输出结果贴 .env 里）：
```powershell
# 生成 64 位 JWT 密钥（复制输出）
-join ((48..57) + (65..90) + (97..122) | Get-Random -Count 64 | ForEach-Object {[char]$_})
```

---

### Step 2（⭐ 强烈建议）：导入本地数据库 → docker MySQL
如果你跳过这一步，docker MySQL 是空库，系统可以启动但只有首次初始化的 3 个账号（admin/admin、teacher01/123456、student01/123456），没有排课/学期/班级等数据。

**方法 A：命令行 mysqldump 导出（最快，推荐）**
```powershell
# 前提：把项目根 .env 里的 DB_PWD 读进当前会话（口令不写死在命令里）
Get-Content ".\.env" | Where-Object { $_ -match '^\s*DB_PWD\s*=' } | ForEach-Object { $env:DB_PWD = ($_ -split '=',2)[1].Trim() }

# ① 导出本地 MySQL 的 edu_schedule_ai（root 口令取自 .env 的 DB_PWD）
#    输出文件放到 docker/mysql-init/ 目录（docker-compose 已挂这个目录）
mkdir -Force "docker\mysql-init" | Out-Null

& "C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqldump.exe" `
    -h localhost -P 3306 -u root "-p$env:DB_PWD" `
    --databases edu_schedule_ai `
    --default-character-set=utf8mb4 `
    --routines --triggers --events `
    > "docker\mysql-init\edu_schedule_ai_$(Get-Date -Format yyyyMMdd).sql"

# ② 验证 SQL 文件存在且大于 100KB（太小说明没导出成功）
Get-ChildItem "docker\mysql-init\*.sql" | Select-Object Name, @{Name='KB';Expression={[math]::Round($_.Length/1KB,1)}}
```
> 如果 `mysqldump.exe` 不在上面路径，就用 `Get-Command mysqldump.exe` 找路径，或者打开 **MySQL Workbench / Navicat / DBeaver** 导出 SQL 文件，保存到 `D:\TRAE SOLO CN\edu-schedule\docker\mysql-init\` 里，文件名随意，后缀 `.sql` 就行。

**方法 B：以后再手动导入（第一次先启动空库）**
跳过这步，容器起来后再用 Navicat 连 `localhost:3307`，然后导入 .sql（docker compose 映射的宿主机 3307 → 容器 3306）。

---

### Step 3：一键构建 + 启动
```powershell
cd "D:\TRAE SOLO CN\edu-schedule"

# 首次启动：构建镜像 + 启动 + 后台运行
#   --build  强制重新 build（如果改了 Java/前端源码，必须加）
#   -d       后台启动（不占当前终端）
docker compose up -d --build
```

⏳ 首次构建**过程**你会看到：
1. 阶段 1/3：`Building backend` → Maven 下载依赖（几百 MB）+ 编译 112 个 Java 文件 + 打包 JAR → 约 3~7 分钟
2. 阶段 2/3：`Building frontend` → npm ci 下载依赖 + npm run build（16 秒）→ 约 1~3 分钟
3. 阶段 3/3：三容器创建 + 启动 → 等 HEALTHCHECK 变 healthy

---

### Step 4：验证（三容器都 healthy = 成功）
等 2~3 分钟（MySQL 第一次初始化慢），然后跑：
```powershell
docker compose ps
```
✅ **预期输出（三列 STATUS 都带 healthy）**：
```
NAME          IMAGE                         COMMAND                  SERVICE     CREATED         STATUS                   PORTS
edu-mysql     mysql:8.0                     "docker-entrypoint.s…"   mysql       3 minutes ago   Up 3 minutes (healthy)   33060/tcp, 0.0.0.0:3307->3306/tcp
edu-backend   edu-schedule-ai:latest        "sh -c 'java ${JAVA_…"   backend     2 minutes ago   Up 2 minutes (healthy)   0.0.0.0:8080->8080/tcp
edu-frontend  edu-schedule-frontend:latest  "/docker-entrypoint.…"   frontend    2 minutes ago   Up 2 minutes (healthy)   0.0.0.0:80->80/tcp
```

三个服务**全部 healthy** 后，打开浏览器验证三端：

| 要访问的 | 地址 | 说明 |
|---------|------|------|
| 三端统一入口（前端 Nginx 80） | http://localhost | ✅ **最常用，用这个！** /api 走同源反代到 backend，无跨域 |
| 后端 API 直连（调试）| http://localhost:8080/api | 比如 `GET /api/semester/list`，返回 401 说明后端活着在（要 JWT 登录）|
| 容器内数据库直连 | localhost:**3307**，用户 root，密码 `.env` 里的 DB_PWD | 用 Navicat/DBeaver 连，看数据对不对 |

---

### Step 5（可选）：查看实时日志（排错神器）
```powershell
# 后端日志（最常用：排课/登录失败等）
docker compose logs -f --tail=200 backend

# 前端 Nginx 日志（access.log + error.log）
docker compose logs -f --tail=200 frontend

# MySQL 日志（慢查询 / 连接失败）
docker compose logs -f --tail=200 mysql

# 三服务一起看（混乱但全）
docker compose logs -f
```

---

## 四、日常运维命令速查

| 操作 | 命令 | 说明 |
|------|------|------|
| 停止所有容器（保留数据）| `docker compose stop` | 之后 `docker compose start` 可直接起来，不用重 build |
| 启动已停止的容器 | `docker compose start` | 不用重新构建，秒起 |
| 重启所有服务 | `docker compose restart` | 改了代码/配置后如果不想重 build，先试试这个（其实改代码必须重 build）|
| 代码改了 → 更新并重启 | `docker compose up -d --build` | 重新 build 有变动的镜像，然后重启 |
| **彻底删除容器**（⚠️ 保留数据库卷和日志卷）| `docker compose down` | 再次 up 会用现存卷，数据还在 |
| **⚠️ 彻底重置（数据库清空！）**| `docker compose down -v` | 三个命名卷一起删除，下次 up 是全新空库，用于"想重新导一遍数据" |
| 进容器里看文件 | `docker compose exec backend bash` 或 `mysql` 的：`docker compose exec mysql mysql -uroot -p` | 进入容器 shell |
| 看容器资源占用（CPU/内存）| `docker stats` | 看 backend 占了多少内存/CPU |

---

## 五、常见问题 & 解决方案（7 条）

### ❓ 1. docker compose up 报 "port is already allocated"
→ 端口被占用。用 Step 0 找到占用 PID，`Stop-Process -Id <PID> -Force`，或改 `docker-compose.yml` 里 ports：比如把 `"80:80"` 改成 `"8081:80"`，然后 `docker compose up -d` 重配。

### ❓ 2. edu-backend 容器不断 restart，STATUS  unhealthy
→ 必看后端日志找原因：`docker compose logs -f --tail=300 backend`
常见原因：
- **DB_PWD 没设** → .env 没复制或为空，backend 启动立即失败（prod 设计：无默认值，防止裸连）→ 复制 .env.example → .env，填值，再 `docker compose up -d`
- **JWT_SECRET 太短** → 报 "The secret key must be at least 256 bits" → 生成长度 64+ 的随机串覆盖
- **MySQL 还在初始化** → 第一次启动 MySQL 要 20~30s，backend depends_on: service_healthy 会自动等，如果过了 2 分钟还是 unhealthy 才是问题

### ❓ 3. 浏览器打开 http://localhost 是空白页但控制台 404
→ Nginx 反代 `/api` 失败。按顺序查：
① `docker compose ps` 三容器 healthy？
② 打开浏览器 DevTools → Network → 刷新 → 看 `/api/xxx` 请求的 Status：
   - **CORS 报错** → 你直接访问了 localhost:8080 调 API（跨域），应该走 localhost 的同源 `/api`，或者改后端 CORS 白名单
   - **502 Bad Gateway** → backend 没起来（参考问题 2）
   - **401 Unauthorized** → 正常！没登录就是 401，去登录页登录就对了

### ❓ 4. docker/mysql-init 里放了 SQL 但数据没导入
→ **Docker MySQL 只会在"卷是空的 / 第一次启动"时执行 initdb.d 下的 SQL**，第二次启动就不会跑了！
解决：
```powershell
# ① 清空命名卷 + 容器（⚠️ 会删所有数据，确保 SQL 在手）
docker compose down -v

# ② 重启 + 重建（第一次启动就会执行 init SQL）
docker compose up -d --build
```

### ❓ 5. 第一次 Maven 构建下载依赖失败（国内网络慢）
→ 后端 Dockerfile 第一阶段用的是 maven:3.9-eclipse-temurin-17，默认走 Maven Central（外国），国内网络可能失败。
解决（二选一）：
- **A. 改 Dockerfile 加阿里云 mirror**（推荐，永久解决）：在后端 Dockerfile 的 `RUN mvn -B -q dependency:go-offline...` 前，加一段 `COPY settings.xml /root/.m2/settings.xml`，settings.xml 配阿里云 mirror
- **B. 用本地 .m2 缓存**（不进容器）：在 docker-compose.yml backend 服务里加 volumes `- ~/.m2:/root/.m2:ro`，宿主机已经下好的依赖直接用

### ❓ 6. Windows Docker Desktop 构建报错 "error during connect"
→ Docker Desktop 没启动！在开始菜单搜 "Docker Desktop"，打开后等左下角鲸鱼 🐳 变绿（Running），再跑命令。

### ❓ 7. JAR 40.87MB / dist 1.74MB 会不会有文件没打进去？
→ 不会。我们 A 选项本地验证过：后端 JAR 能正常启动（Hikari + 双线程池 + NoLoggingImpl 全对），前端 dist 能被 Nginx 正常托管（Gzip 后首页秒开）。Docker 构建就是走**完全相同**的 mvn package / npm run build，所以产物和本地一致。

---

## 六、上生产服务器（简要）
如果你以后要部署到阿里云/腾讯云 ECS（单主机，不用 K8s），流程：
1. 服务器装 Docker + Docker Compose V2（Ubuntu 22.04 一句 curl 搞定）
2. 把整个 `D:\TRAE SOLO CN\edu-schedule`（除 node_modules / target）传到服务器 `/opt/edu-schedule/`（SCP/Rsync）
3. 服务器上 `cd /opt/edu-schedule && cp .env.example .env`，改生产强密码 + JWT_SECRET + AI Key
4. `docker compose up -d --build`
5. 配 Nginx 反代 + HTTPS 证书（Let's Encrypt，certbot 一句）→ 公网 443 → 服务器 80（frontend）
6. 建议开启：ufw 防火墙只开放 22/80/443，**把 8080/3307 在云安全组关掉**（公网别暴露数据库和后端直连端口）

---

**📝 遇到报错第一件事：贴 `docker compose ps` + `docker compose logs -f --tail=200 backend` 两个命令的输出**，95% 的问题都能从日志里一眼定位 ✨
