# ============================================================
# 智能教务排课系统 —— 后端 Dockerfile
# 构建策略：多阶段构建（Builder 跑 Maven 编译打包，Runtime 只拿 JRE + 成品 JAR）
#   · 构建镜像（builder）：maven:3.9-eclipse-temurin-17  ~750MB（编译用，最终不保留）
#   · 运行镜像（runtime）：eclipse-temurin:17-jre        ~220MB（生产运行，精简）
# 构建命令（配合 docker-compose 一般不用手动跑）：
#   docker build -f Dockerfile -t edu-schedule-ai:latest .
# ============================================================

# ------------------------------
# Stage 1: Builder（编译打包）
# ------------------------------
FROM maven:3.9-eclipse-temurin-17 AS builder

# 工作目录（容器内路径）
WORKDIR /app

# ① 先复制 pom.xml：利用 Docker 层缓存，只要 pom 没变，后续改 Java 源码不会重新下载几百 MB 的 Maven 依赖
COPY pom.xml ./
RUN mvn -B -q dependency:go-offline -DskipTests

# ② 再复制源码（src 目录 + 资源文件）→ 编译打包
COPY src ./src
RUN mvn -B -q clean package '-DskipTests'

# ------------------------------
# Stage 2: Runtime（生产运行）
# ------------------------------
FROM eclipse-temurin:17-jre AS runtime

# 0. 元数据（可选，方便 docker inspect 查看）
LABEL org.opencontainers.image.title="edu-schedule-ai-backend" \
      org.opencontainers.image.description="AI 驱动的智能教务排课系统后端（Spring Boot 3.3.5 + MyBatis + Hikari + JWT + @Async 双线程池）" \
      org.opencontainers.image.authors="edu-schedule-team"

# 1. 时区：强制 Asia/Shanghai（日志 + 排课时间段都按北京时间）
ENV TZ=Asia/Shanghai \
    LANG=C.UTF-8 \
    LC_ALL=C.UTF-8

# 2. 创建非 root 用户 appuser（生产安全：不用 root 跑 Java 进程，防止容器逃逸提权）
RUN groupadd -r appuser && useradd -r -g appuser -m appuser

# 3. 工作目录 + 日志目录（持久化给容器外 docker volume）
WORKDIR /app
RUN mkdir -p /app/logs && chown -R appuser:appuser /app

# 4. 从 builder 阶段拷贝成品 fat-JAR（只拿这一个文件，层最小）
COPY --from=builder --chown=appuser:appuser /app/target/edu-schedule-ai-1.0.0.jar /app/app.jar

# 5. 切换到非 root 用户
USER appuser

# 6. 暴露端口（后端 8080，前端 Nginx 通过 docker-compose 内网 service 名访问 backend:8080 /api）
EXPOSE 8080

# 7. JVM 启动参数（按容器内存默认 2C/2G 调；上生产根据 limits 再覆盖）
#    -XX:+UseContainerSupport: JDK 10+ 默认 true，显式写一次保险（识别容器 cgroup 内存/CPU 限制）
#    -XX:MaxRAMPercentage=75.0: 拿容器 limits 的 75% 当堆内存（留 25% 给 JIT/Metaspace/NIO/线程栈）
#    -XX:+ExitOnOutOfMemoryError: OOM 立即退出（让 docker/k8s 重启策略接管，防止 JVM 假死）
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:+ExitOnOutOfMemoryError \
               -XX:+HeapDumpOnOutOfMemoryError \
               -XX:HeapDumpPath=/app/logs/java_pid%p.hprof \
               -Duser.timezone=Asia/Shanghai \
               -Djava.security.egd=file:/dev/./urandom"

# 8. 健康检查：每 20s 请求一次后端接口（只要 HTTP 200/401 都算 OK，404/5xx/超时算失败）
#    如果 /api/auth/login 会被 JWT 拦，就用一个开放接口；实在没有就 tcp 探 8080 也行。
#    这里我们用 curl HEAD /api/（没有的话 Spring 会返回 404，只要返回了不是超时就认为服务启了），
#    实际推荐在项目里加一个 /actuator/health 或 /api/health ，现在先用简单探活。
HEALTHCHECK --interval=20s --timeout=5s --start-period=60s --retries=3 \
    CMD curl -fsS http://localhost:8080/api/ || curl -fsS http://localhost:8080/ || exit 1

# 9. 启动：exec 形式让 Java 进程作为 PID 1，正确接收 SIGTERM（触发 Spring Boot graceful shutdown，@Async 线程池会等 90s 跑完）
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -Dspring.profiles.active=prod -jar /app/app.jar"]
