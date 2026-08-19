# ============================================================
# 智能教务排课系统 —— 前端 Dockerfile
# 构建策略：多阶段构建（Builder 跑 npm build，Runtime 只拿 Nginx + dist 静态文件）
#   · 构建镜像（builder）：node:20-alpine   ~200MB（编译用，最终不保留）
#   · 运行镜像（runtime）：nginx:alpine     ~40MB （生产运行，alpine 最小化）
# 关键特性：
#   · Nginx 配置支持 Vue Router history 模式（刷新不 404）
#   · /api 请求反向代理到后端容器 backend:8080（docker-compose 内网）
#   · Gzip 压缩 JS/CSS，体积极小
# ============================================================

# ------------------------------
# Stage 1: Builder（npm 构建）
# ------------------------------
FROM node:20-alpine AS builder

# 工作目录
WORKDIR /app

# ① 先复制 package.json + package-lock.json：Docker 缓存层，依赖不变不重新 node_modules
#    如果没有 package-lock.json 就用 yarn.lock / pnpm-lock.yaml，这里以 npm 为准（本地用的是 npm）
COPY package*.json ./

# npm ci：严格按 lockfile 安装，比 npm install 更快更可重复
# --no-audit / --no-fund：跳过审计和赞助提示，减少构建日志噪音
RUN npm ci --no-audit --no-fund || npm install --no-audit --no-fund

# ② 再复制源码（. 表示全部；.dockerignore 会排除 node_modules/dist/.git 等）
COPY . .

# ③ 生产打包（Vite 输出到 /app/dist）
RUN npm run build

# ------------------------------
# Stage 2: Runtime（Nginx 托管静态文件）
# ------------------------------
FROM nginx:alpine AS runtime

# 0. 元数据
LABEL org.opencontainers.image.title="edu-schedule-ai-frontend" \
      org.opencontainers.image.description="AI 驱动的智能教务排课系统前端（Vue 3 + Vite 5 + Element Plus）" \
      org.opencontainers.image.authors="edu-schedule-team"

# 1. 时区：Asia/Shanghai（Nginx access/error 日志时间）
ENV TZ=Asia/Shanghai

# 2. 复制 Nginx 配置：覆盖 default.conf（Vue history 模式 + /api 反代后端）
COPY nginx.conf /etc/nginx/conf.d/default.conf

# 3. 复制构建产物 dist：从 builder 阶段只拿静态文件（HTML/JS/CSS/字体/图片）
COPY --from=builder /app/dist /usr/share/nginx/html

# 4. 暴露 80 端口：宿主机 docker-compose 会映射 80:80（或者 8081:80 本地调试）
EXPOSE 80

# 5. 健康检查：HEAD http://localhost:80/index.html 能返回就行
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD wget --spider -q http://localhost:80/ || exit 1

# 6. nginx:alpine 默认 CMD 就是 nginx -g daemon off;，不用改
#    前台运行，docker stop 时会发 SIGTERM 给 Nginx 优雅退出
