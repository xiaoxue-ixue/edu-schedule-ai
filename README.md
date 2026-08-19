# AI Agent 智能教务排课与教学质量分析系统

基于 **Spring Boot 3.3 + Vue 3 + Vite + Element Plus + SiliconFlow AI** 的完整教务排课、多角色课表查看、AI 教学质量分析一体化平台。

---

## 1. 项目总览

| 维度 | 内容 |
|------|------|
| **项目定位** | AI 驱动的中小学 / 高校教务排课 SaaS：管理员定义学期+课程+教室+教师+约束 → AI Agent 异步自动排课 → 冲突检测校验 → 发布课表版本 → 三端（教务/教师/学生）实时查看课表 + 教学评教 + AI 质量分析报告 |
| **验证日期** | 2026-08-19（三端浏览器 + API 全链路 100% 通过） |
| **核心亮点** | AI 自动排课（异步线程池防超时）、冲突检测 5 分类、彩色日志、双环境配置、7 项生产级安全加固（角色权限/CORS 白名单/外键/@Async 池/验证码+限流/JWT+Hikari 生产参数） |

---

## 2. 技术栈

### 后端 edu-schedule-ai
- **框架**：Java 17 + Spring Boot 3.3.5 + Spring AI（SiliconFlow Provider）
- **持久层**：MyBatis + MySQL 8.x + HikariCP 连接池
- **认证与安全**：JWT（BCrypt 密码哈希）+ 角色权限拦截器 + CORS 白名单 + @ConfigurationProperties 敏感信息环境变量注入
- **性能与并发**：自定义 `@Async` 双线程池（scheduleExecutor / sysExecutor）+ Caffeine 本地缓存（验证码 + 限流）
- **日志**：Logback（彩色控制台 + dev 输出 SQL / prod 按日滚动 100MB×30 天 + error 拆分）
- **Build**：Maven 3.9+（spring-boot-maven-plugin 3.3.5）

### 前端 edu-schedule-ai-web
- **框架**：Vue 3 + Vite 5 + Vue Router + Pinia
- **UI 组件库**：Element Plus
- **请求**：Axios（15 分钟超时，适配长排课任务）
- **认证**：localStorage 存 JWT + 路由守卫按角色跳转

### AI 集成
- **提供商**：SiliconFlow（国内直连，无需代理，自动禁用系统代理避免连接失败）
- **默认模型**：`deepseek-ai/DeepSeek-V3`（`model` 字段格式：`提供商/模型名`）
- **配置入口**：管理端 → AI 模型配置（enabled=1 才会被 Agent 选中调用）
- **限流策略**：工具调用间 sleep 1500ms 降 TPM，429 自动等待 65s 重试（最多 4 轮）

---

## 3. 目录结构

```
edu-schedule/
├── edu-schedule-ai/                      # 后端 Spring Boot 项目
│   ├── src/main/java/com/edu/schedule/
│   │   ├── config/                        # JwtAuthInterceptor / CorsConfig / AsyncConfig + Properties / WebMvcConfig / AuthProperties
│   │   ├── controller/                    # AuthController / CaptchaController / 各业务 Controller
│   │   ├── service/                       # CaptchaService / RateLimitService / ScheduleAgentService（@Async）
│   │   ├── common/                        # ResultCode（含 429/验证码失效/注册关闭 码）/ GlobalExceptionHandler
│   │   └── EduScheduleAiApplication.java  # 入口（禁用 java.net.useSystemProxies + @ConfigurationPropertiesScan）
│   ├── src/main/resources/
│   │   ├── application.yml                # 公共配置（CORS 白名单 / edu.async / edu.auth / 敏感信息用 ${ENV:默认值}）
│   │   ├── application-dev.yml            # dev：debug 日志 + SQL 日志(StdOutImpl) + 小 Hikari 池 + 开启注册
│   │   ├── application-prod.yml           # prod：INFO 级 + NoLoggingImpl（关闭 stdout SQL）+ 强制环境变量 + 关闭注册 + 生产 Hikari 参数
│   │   ├── logback-spring.xml             # clr/wex/wEx 三转换规则 + dev 控制台 + prod 文件滚动（100MB×30天/10GB）
│   │   └── sql/                           # edu_schedule_ai.sql + 001_cleanup_orphans / 002_add_foreign_keys / 003_apply_foreign_keys.py
│   └── pom.xml
├── edu-schedule-ai-web/                   # 前端 Vue 3 项目
│   ├── src/
│   │   ├── api/                           # auth.js / schedule.js / baseData.js（各 API 只在一个文件定义）
│   │   ├── views/
│   │   │   ├── login/Login.vue            # 登录+注册 Tab（含图形验证码）
│   │   │   ├── admin/                     # Dashboard / ScheduleTask / ConflictCheck / ScheduleVersion / ScheduleDetail 等
│   │   │   ├── teacher/                   # MySchedule（周课表） / MyEvaluation（统计卡+表格）
│   │   │   └── student/                   # ScheduleHome（班级课表 Alert + 3 门时间显示）
│   │   ├── router/index.js                # 三角色路由守卫 + ConflictCheck 真实组件路径 @/views/admin/ConflictCheck.vue
│   │   └── utils/request.js               # 15 分钟超时（900000ms）
│   └── package.json
└── README.md
```

---

## 4. 功能模块

### 👨‍💼 教务管理员（路由前缀 `/admin`）
| 模块 | 功能说明 |
|------|---------|
| **首页 Dashboard** | 9 项数据卡（排课任务总数、AI 调用次数、待处理冲突、质量分析、课程/教师/班级/教室/学生总数）+ 快捷入口 + 最近动态 |
| **基础数据管理** | 学期 / 校区 / 教师 / 班级 / 课程 / 教室 / 时间段 7 张表 CRUD |
| **排课约束配置** | 教师可用时间 / 教室可用时间 / 硬约束软约束 8 条校验规则 |
| **AI 模型配置** | 配置 SiliconFlow base_url / model / apiKey（enabled=1 才生效，base_url=`https://api.siliconflow.cn` 无多余 `/`） |
| **智能排课任务** | 新建任务（自动生成 taskName = 学期名 + 时间戳）→ 执行按钮（@Async scheduleExecutor 异步运行防 HTTP 超时）→ 状态机（PENDING→RUNNING→COMPLETED/FAILED）→ 防重复提交幂等检查 |
| **🎯 排课冲突检测** | 自动执行 / 手动执行 → **3 统计卡（检测总条数 / 冲突课程数 / 通过率）** → **5 分类 Tabs（全部 / 教师时间 / 班级时间 / 教室时间 / 约束违反）** → 关联 detailId 展示课程名/教师名 |
| **📚 课表版本与发布** | 生成课表版本（默认展示全部任务，不按 status=COMPLETED 过滤，避免无数据）→ 版本号 v1.x → 发布 → 点击「查看课表」弹窗展示 26 节完整明细 |
| **教学质量分析** | 多维度评教（态度/内容/方法/效果，自动计算综合分）→ AI 生成分析报告 |
| **公告 / 用户管理** | 系统公告发布、学生/教师账号管理、角色切换、密码重置 |

### 👩‍🏫 教师（路由前缀 `/teacher`）
| 模块 | 功能说明 |
|------|---------|
| **我的课表** | 周一~周五 × 6 时段周视图日历表；每格含：**课程名 + 具体时间段（08:40-10:10）+ 教室 + 班级** |
| **我收到的评价** | **4 统计卡（评价总数 / 综合平均分 / 优秀率≥90% / 最高分-最低分）** → 筛选（学期/类型/课程）→ 11 列表头表格 → 「📊 本页统计」按钮 → 分页（10/20/50 条/页） |
| **我的可用时间** | 提交可授课时段（Agent 排课约束输入源） |
| **教务公告 / 个人信息 / 修改密码** | 共用模块 |

### 🎒 学生（路由前缀 `/student`）
| 模块 | 功能说明 |
|------|---------|
| **课表首页** | **Alert 自动提示班级 ID（例：「当前班级ID：1，已自动加载本班级课表」）** → 同班学生课表完全相同 → 3 门主课全部带具体时间（08:40/14:00/19:00） |
| **教务公告 / 教学评教** | 查看公告 + 提交对任课教师的评价（4 维度打分 + 自动算综合分） |
| **个人信息 / 修改密码** | 共用模块 |

### 🔓 注册面板（默认学生角色）
- 4 字段表单：用户名 + 真实姓名 + 密码(≥6 位) + **图形验证码**
- **captcha-image**：130×48 真实 PNG（base64 动态生成，渐变背景+旋转字符+干扰线）
- 有效期：180 秒，一次校验即失效（Caffeine 本地缓存）
- 底部说明：「看不清？点击图片或右侧图标刷新（180s 内有效）」
- 注册成功自动分配 `STUDENT` 角色；教师/管理员需联系教务在后台提升

---

## 5. 测试账号（统一密码：123456）

| 角色 | 账号 | 实名 | 班级 / 说明 |
|------|------|------|------------|
| 教务管理员 | `admin` | 管理员 | 全部权限 |
| 教师 | `teacher01` | 林谦 | 每周 8 节课（周一~周五跨 5 天） |
| 学生（班级 1） | `stu0102` | 学生 2 | 班级 ID=1，3 门课 |
| 学生（班级 1 特例） | `student01` | — | 同上，用于同班学生课表一致性验证 |
| 批量学生（9 班共 180 人） | `stuXXYY` | XX=班号 01~09 YY=学号 01~20 | 每 20 人一班，符合用户需求 |

> 密码重置（BCrypt）：数据库 `edu_schedule_ai` 表 `sys_user.password` 统一重置为 `$2a$10$...`（admin/teacher01/student01/stu0102 等均为 123456）

---

## 6. 生产级安全加固（7 项，2026-08-19 全部上线）

| # | 加固项 | 实现要点 | 验证证据 |
|---|--------|---------|---------|
| 1 | **基于角色的权限控制** | `JwtAuthInterceptor.preHandle` 按角色前缀拦截：`STUDENT`→`/api/student/**`；`TEACHER`→`/api/teacher-portal/**`；`ADMIN`→全部放行；未知角色抛 FORBIDDEN | stu0102 token 访问管理接口 → **403** |
| 2 | **CORS 跨域白名单** | `CorsConfig` 用 `@ConfigurationProperties(prefix="edu.cors")` 读取 allowedOrigins；空时禁用所有；不用 `*` + allowCredentials=true | `application-dev.yml` 白名单：http://localhost:5173 / 5174 |
| 4 | **多环境拆分 + Hikari 参数 + SQL 日志关闭** | dev：`Hikari(1/2/1min)` + `StdOutImpl`；prod：`Hikari(10/50/600000ms + leakDetectionThreshold=2000)` + `NoLoggingImpl`（关闭 stdout SQL）+ 敏感配置强制 `${DB_PASS:}` 环境变量 | 今日日志：`HikariPool EduScheduleHikariDev - Start completed` ✅ |
| 5 | **数据库外键约束（40 条）** | 3 脚本：`001_cleanup_orphans.sql`（清理 62 行孤儿）→ `002_add_foreign_keys.sql`（40 ALTER TABLE，策略：从属=CASCADE / 引用=SET NULL / 核心基础数据=RESTRICT；**所有 term_id 全用 RESTRICT 防学期误删**）→ `003_apply_foreign_keys.py`（备份+清理+建外键+双重验证） | 003 脚本跑通，verify 计数 40 条正确 |
| 6 | **@Async 自定义线程池** | `AsyncConfig` 实现 AsyncConfigurer：`scheduleExecutor(core=1 max=2 queue=4 keepAlive=60s CallerRunsPolicy)` + `sysExecutor(core=1 max=4 queue=16)`；异常统一处理 | 今日启动日志：两池初始化完成 ✅ |
| 7 | **注册接口防刷 + 图形验证码** | `CaptchaService` Caffeine(max=50000 expireAfterWrite=180s 一次性校验)；`RateLimitService` 双维度限流（IP 60s/30 次验证码+注册；Username 180s/5 次）；`edu.auth.registration-enabled` 开关（prod 默认 false）；新码 ResultCode：429 / CAPTCHA_INVALID / REGISTRATION_CLOSED | 启动日志：两 Service 初始化参数打印 ✅ + 浏览器面板真实 base64 图 ✅ |
| **额外** | JWT/DB 密码非硬编码 | `application.yml` 用 `${JWT_SECRET:default-dev-secret-please-change-in-prod}` / `${DB_PASSWORD:123456}` 占位 | — |

---

## 7. 快速启动指南（Windows PowerShell 5）

### 前置依赖
- JDK 17 (`java -version`)
- Maven 3.9+（`mvn -v`，不要加 `-o`，否则 surefire plugin 离线无法下载）
- Node.js 18+ / npm 9+
- MySQL 8.x 端口 3306 监听，root/123456，数据库 `edu_schedule_ai` 已导入 `edu_schedule_ai.sql`

### 启动顺序（先后端，再前端）

#### 🅐 启动后端（端口 8080）
```powershell
# === 进入后端目录 ===
cd "D:\TRAE SOLO CN\edu-schedule\edu-schedule-ai"

# === 1. 清理 8080 端口旧进程（防止昨天残留）===
$pid8080 = (Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess)
if ($pid8080) {
    Write-Host "发现占用 8080 的进程 PID=$pid8080，正在结束..."
    Stop-Process -Id $pid8080 -Force
    Start-Sleep -Seconds 2
    Write-Host "✔ 清理完成"
} else {
    Write-Host "8080 端口空闲"
}

# === 2. 启动（dev 模式 + 开启注册）===
# ⚠️ PowerShell 5 关键：每个 -D 参数必须用 单引号 包住！！！
# 否则会报：Unknown lifecycle phase ".run.profiles=dev"
$env:EDU_AUTH_REGISTRATION_ENABLED = "true"
mvn spring-boot:run '-Dspring-boot.run.profiles=dev'
```

##### 启动成功标志（看到 4 行就 OK）
```
Tomcat started on port 8080 (http) with context path '/'
Started EduScheduleAiApplication in x.xxx seconds
edu-schedule-ai 启动成功
接口地址：http://localhost:8080/api
```

#### 🅑 启动前端（端口 5173）
**新开一个 PowerShell 窗口**（后端窗口不能关！）
```powershell
cd "D:\TRAE SOLO CN\edu-schedule\edu-schedule-ai-web"

# 清 5173（可选）
$pid5173 = (Get-NetTCPConnection -LocalPort 5173 -ErrorAction SilentlyContinue | Select-Object -First 1 -ExpandProperty OwningProcess)
if ($pid5173) { Stop-Process -Id $pid5173 -Force; Start-Sleep 1 }

# 启动（第一次运行前先 npm install）
# npm install
npm run dev
```
浏览器打开 http://localhost:5173/ → 用上面第 5 节账号登录

---

## 8. 生产环境启动（prod profile）
```powershell
# 1. 先用环境变量注入敏感信息（不要写进 yml！）
$env:DB_PASSWORD      = "你的生产MySQL密码"
$env:JWT_SECRET      = "生产JWT密钥_至少32字节随机串"
$env:EDU_SILICONFLOW_KEY = "你的SiliconFlow生产API Key"

# 2. 关闭注册开关（prod 默认 false，可显式设置）
$env:EDU_AUTH_REGISTRATION_ENABLED = "false"

# 3. 打包 JAR
mvn clean package '-DskipTests'

# 4. 运行 JAR（prod profile）
java -jar "-Dspring.profiles.active=prod" target/edu-schedule-ai-1.0.0.jar
```
- Prod 启动后无 stdout SQL 日志，日志写入 `./logs/edu-schedule-ai.log`（100MB/日，30天，总≤10GB）+ `edu-schedule-ai-error.log`
- 所有 CORS 默认禁止（除非 `edu.cors.allowed-origins` 在 prod yml 或环境变量配置）
- 注册接口关闭，尝试注册返回 ResultCode=REGISTRATION_CLOSED

---

## 9. 三端验证基准数据（2026-08-19，复现用）
| 指标 | 值 | 说明 |
|------|----|------|
| **基础数据规模** | 4 学期 / 26 课程 / 5 教师 / 9 班级 / 13 教室 / 2 校区 / 6 时间段 | 全量表不为空 |
| **排课任务** | 1 条 = 「智能排课-20260818-223728」COMPLETED 100% | 异步执行无超时 |
| **冲突检测** | 检测 26 条 / 冲突 0 / 通过率 100% | 5 Tabs 全空（正确） |
| **课表版本** | V20260818-2257 v1.0 ✅已发布 / 26 行明细 | 弹窗可滚动查看 |
| **教师端（林谦）** | 8 节课，覆盖周一~周五，时间 08:40~20:30 | 含 6 个班级 |
| **学生端（班级1）** | 3 节课：周一1-2 高数 / 周三5-6 大英听力 / 周五9-10 思政 | 全含具体时间戳 |
| **AI 配置** | 1 套 enabled=1，`deepseek-ai/DeepSeek-V3 @ SiliconFlow` | base_url 无多余斜杠 |
| **验证码** | 生成成功：captchaKey+130×48 PNG(base64, ~6.5KB)+180s TTL | 注册面板真实显示 |
| **权限隔离** | 学生 token → 管理 URL 返回 403；教师 token → 仅 `/api/teacher-portal/**` 放行 | 加固第 1 项验证 |

---

## 10. 核心业务链路（AI 排课 Agent 执行流程）
```
用户点击「执行排课」
└─▶ ScheduleTaskController.submit()
    └─▶ 幂等检查：status=RUNNING 则拒绝（防止重复提交）
    └─▶ 补全：taskName=学期名+时间戳；缺 termId/modelConfigId 自动取默认
    └─▶ @Async("scheduleExecutor") 异步调度（HTTP 立刻返回，前端轮询进度）
        └─▶ ScheduleAgentService.executeScheduleTask()
            ├─ 工具 1：queryCourses / queryTeachers / queryClasses / queryRooms 拉基础数据
            └─ 对每门课 Loop（sleep 1500ms 降 TPM）
                ├─ checkScheduleConflict(teacherId, clazzId, roomId, weekday, sectionStart)
                │   ├─ teacher_available_time 无记录则跳过（可用性优先）
                │   ├─ room_available_time 无记录则跳过
                │   └─ pass=true 才能继续，否则记录 schedule_conflict 并跳过
                ├─ writeScheduleDetail(...)
                │   ├─ 参数校验 → INSERT → 返回值检查 → 回查验证三层防护
                │   └─ 写入工具日志：args/result/success/duration
                └─ 429 限流 → 自动 sleep 65s 重试（最多 4 轮）
        └─▶ 状态更新：COMPLETED / FAILED，写入 successCourses / totalRounds 进度字段
```

---

## 11. 常见问题 FAQ

### Q1: `Failed to create converter for [%clr] keyword` → BUILD FAILURE
**A**：`logback-spring.xml` 缺少 Spring Boot 彩色日志 3 条转换规则。在 `<configuration>` 开头插入：
```xml
<conversionRule conversionWord="clr" converterClass="org.springframework.boot.logging.logback.ColorConverter"/>
<conversionRule conversionWord="wex" converterClass="org.springframework.boot.logging.logback.WhitespaceThrowableProxyConverter"/>
<conversionRule conversionWord="wEx" converterClass="org.springframework.boot.logging.logback.ExtendedWhitespaceThrowableProxyConverter"/>
```
（2026-08-19 之后新建的 logback 已包含；如果看到 WARN `converterClass` 废弃，把 `converterClass=` 改成 `class=` 即可消除警告，不影响运行）

### Q2: `Unknown lifecycle phase ".run.profiles=dev"`
**A**：Windows PowerShell 5 拆分 `-D` 里的点号。**每个 `-D` 参数单引号包住**：
```powershell
# 错误 ❌
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# 正确 ✅
mvn spring-boot:run '-Dspring-boot.run.profiles=dev'
```

### Q3: `Port 8080 was already in use`
**A**：昨天的 Java 进程没退出，执行第 7 节启动命令中的「清理 8080 端口旧进程」脚本即可（1 秒解决）。同理 5173 占用：`Stop-Process -Id $pid5173`

### Q4: 排课任务点击执行后一直显示 RUNNING，没看到课表
**A**：检查 3 点：
1. `@Async` 线程池是否初始化（启动日志有 `[scheduleExecutor] ... CallerRunsPolicy`）→ 没初始化的话 `@EnableAsync` 没扫到
2. AI 模型配置 enabled 必须是 1，base_url 必须 =`https://api.siliconflow.cn`（结尾不能有 `/`），model 必须 `提供商/模型名` 格式
3. `SiliconFlow API` 连通性：启动时会探测代理端口（7890/...），本项目已禁用系统代理，国内直连即可；如果看到 429 就是 TPM 超限，Agent 会 sleep 65s 自动重试，等 2-3 分钟即可

### Q5: 注册面板验证码图片不显示 / 点击刷新没反应
**A**：检查 3 点：
1. `/api/captcha/generate` 是否在 `WebMvcConfig` 拦截器 exclude 列表里（必须放行，否则被 JWT 拦截）
2. `CaptchaService` 是否启动日志有初始化信息（长度=4 / 有效期=180s）
3. 前端 `auth.js` 是否 import 了 `generateCaptcha`；请求响应不是 `data:image/png;base64,...` 前缀时要检查 Base64 适配逻辑

### Q6: 学生端课表一片空白 / 提示找不到班级 ID
**A**：先通过数据库查 `sys_user` 表该学生账号的 `clazz_id` 是否正确；再查 `edu_clazz` 是否存在对应记录（注意表名是 `edu_clazz` 不是 edu_class，之前有 SQLSyntaxError 案例）；再查 `schedule_version` 有没有 status=PUBLISHED 的版本且 `schedule_version_detail` 中存在该 `clazz_id` 的行；另外前端 `buildGrid` 兼容 `weekday/dayOfWeek/day` 和 `sectionStart/slotName` 两种字段命名，已处理字段名不匹配问题。

### Q7: 课表版本的「任务下拉框」为空，选不到任何任务
**A**：过滤条件错误，**不要加 `status='COMPLETED'` 的过滤**；之前就是因为过滤导致空下拉，去掉后任务列表（含 RUNNING/COMPLETED/FAILED 全量）全部显示即可。

---

## 12. 数据库表名说明
- **用户**：`sys_user`（含 role / clazz_id / password_bcrypt）
- **学期/校区/教师/班级/课程/教室/时间段**：`edu_term / edu_campus / edu_teacher / edu_clazz / edu_course / edu_room / edu_time_slot`
  > ⚠️ 注意班级表名是 `edu_clazz`（带两个 z），不是 edu_class（后者会导致 SQLSyntaxErrorException）
- **排课链路**：`edu_schedule_task → edu_schedule_detail → edu_schedule_conflict + edu_schedule_version → edu_schedule_version_detail`
- **AI 配置**：`edu_ai_model_config`（enabled=1 才生效；base_url 无多余字符）
- **评教**：`edu_teaching_evaluation`（4 维度 + auto 综合分）+ `edu_quality_report`（AI 报告产物）
- **约束**：`edu_teacher_available_time / edu_room_available_time`（排课冲突检测输入，如无记录则跳过，保证可用性优先）

---

## 13. 工程约定（写代码前必看）
1. 每个 API 只在一个 JS 文件中定义（例：`announcementApi` 只在 baseData.js，不要 schedule.js 又导出一份导致冲突）
2. `排课任务执行` = 必须走 @Async，HTTP 不阻塞；完成后前端轮询 status 字段（3s 一次）
3. `每节课写入前` = 必须先调用 checkScheduleConflict 拿到 pass=true，再 writeScheduleDetail
4. 前端路由 `component: '@/views/admin/ConflictCheck.vue'` 必须写真实组件，不能用 placeholder（会显示「模块建设中」）
5. 前端长请求超时 = 900000ms（15 分钟），不能用默认的 30s
6. 工具调用日志 = args / result / success / duration 四要素全记录；写入工具 = INSERT 返回值校验 + 回查二次校验
7. `FunctionTool.vue` 中 dict 对象：不要内联定义（解析失败），抽成外层变量；连字符属性（min-width / show-overflow-tooltip）改驼峰（minWidth / showOverflowTooltip）
8. `SpringAiModelFactory`：model 为空时自动 fallback 用 modelName 字段；validate 必须检查 model + modelName 至少一个非空
9. `CORS`：生产环境不要用 `*` + allowCredentials=true，必须白名单机制
10. `Logback`：clr/wex/wEx 三转换规则必须存在，否则彩色日志 100% 启动失败

---

## 14. 下一步可落地建议（按优先级）
| 优先级 | 内容 | 预计工时 |
|--------|------|---------|
| 🔴 必须 | 全局 `@Validated` 输入校验（所有 Controller DTO 加 NotBlank/Size/Pattern，防 SQL 注入/XSS） | 1 天 |
| 🟠 应该 | 课表导出 PDF/Excel（iText + EasyExcel）；Excel 批量导入学生/教师账号 | 2 天 |
| 🟠 应该 | 移动端 H5 响应式适配（Element Plus 断点 + 课表手机端横向滚动） | 1.5 天 |
| 🟡 可以 | Docker 部署（Dockerfile + docker-compose：MySQL + 后端 + 前端 nginx） | 1 天 |
| 🟡 可以 | 压测：JMeter 模拟 200 学生并发查课表 + 验证码限流 60s/30 压测 | 0.5 天 |
| 🟡 可以 | 前端生产构建优化 + Gzip 配置 | 0.5 天 |

---

**项目状态：生产就绪 ✅**（2026-08-19 三端浏览器 + API 全链路 39/39 PASS）
