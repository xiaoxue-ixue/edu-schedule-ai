# 智能教务 AI 排课系统 · 15 道高频面试题 + 参考答案
> 适用场景：项目答辩 / 简历面试 / 技术分享。所有问题都基于你**已经做完的代码和架构**，不是空话。
> 每个问题分「面试官想问什么」+「满分回答话术」两部分，按难度从易到难排序。

---

## 🟢 基础 & 架构类（一面必问 4 题）

### Q1. 先花 3 分钟介绍一下你这个项目？（开场必问，决定后面深度）
**面试官想知道**：你能不能把事说清楚，业务价值 + 技术选型 + 核心难点 + 你做的成果（不能只罗列功能）。
**满分回答话术**：
> 我做的是一套「面向中小学校的 AI 智能教务排课 + 教学质量 AI 分析系统」，业务目标是替代传统 Excel 手工排课（一个教务排一学期要花一周还反复冲突）。
>
> 架构是前后端分离：后端 **Spring Boot 3 + MyBatis + MySQL 8 + @Async 双线程池 + SiliconFlow LLM**，前端 **Vue 3 + Vite + Element Plus**，生产部署 **Docker Compose 三容器编排**（Nginx 前端 + Spring Boot 后端 + MySQL）。
>
> 我做了 4 件关键的事：
> 1. **数据建模**：设计了「学期/校区/班级/课程/教师/教室/时间段」7 维关系，落地 28 张表和 40 条外键约束（CASCADE/SET NULL/RESTRICT 三种动作按业务选），从根本上杜绝孤儿数据；
> 2. **权限体系**：三层 RBAC（admin/teacher/student），基于 JWT 拦截器做 URI 前缀级权限校验，学生碰不到任何 `/api/admin/**`，三端 39 条 API 全链路验证通过；
> 3. **核心的 AI 智能排课引擎**：自研异步状态机，用 `@Async` 自定义的 scheduleExecutor 线程池（4 核 8 最大 16 队列）避免 HTTP 30 秒超时，任务先通过冲突检测循环做硬约束，再交给 Spring AI 调用 SiliconFlow LLM 做软约束的启发式时间段打分，最终**实测 26 门课程 / 0 冲突 / 教师学生班级三张课表 100% 一致**；
> 4. **生产级安全加固 7 项**：CORS 白名单配置化、多环境拆分(dev/prod) 关 SQL 日志 + Hikari 生产规格、外键、自定义异步池、注册验证码 + Caffeine 双维度限流(IP 60s 10 次 / User 180s 3 次)、敏感密钥强制环境变量注入（不给默认值，防止开发配置上生产）。
>
> 最终后端 JAR 40.87 MB，启动 5.5 秒；前端 dist 1.74 MB，Nginx 托管首屏秒开；Docker Compose 一键部署，本地开发和服务器环境零差异。

---

### Q2. 项目怎么划分的三端权限？如果学生 token 改了 role 字段能越权吗？
**面试官想知道**：你是不是真做了 RBAC，JWT 防篡改吗？
**满分回答**：
> 不会越权，我从两个地方堵死：
>
> ① **JWT 本身防篡改**：JWT 的 signature 用 32 位密钥 HMAC-SHA256 签名，用户只能改 payload 不能复算签名，后端 `JwtUtils.validateToken()` 第一步就校验签名，签名不一致直接抛 401。
>
> ② **服务端 URI 前缀拦截**：在 `JwtAuthInterceptor.preHandle()` 里按当前请求的 `request.getRequestURI()` 做前缀匹配：
> - `role=student` 只允许访问 `/api/student/**`、`/api/user/me`、`/api/auth/logout` 这几个；
> - `role=teacher` 对应 `/api/teacher/**`；
> - `role=admin` 全放行。
> 其他任何接口直接抛 `FORBIDDEN` 业务异常，`GlobalExceptionHandler` 转 403 HTTP。
>
> 这种"默认 deny，显式 allow"的拦截方式比方法注解 `@PreAuthorize` 更省心 —— 新增接口如果忘了加注解，默认只有 admin 能碰，不会出现学生能删用户的低级漏洞。

---

### Q3. 为什么排课任务要异步做？直接在 Controller 里同步跑不行吗？
**面试官想知道**：懂不懂 HTTP 生命周期 vs 业务任务生命周期，线程模型常识。
**满分回答**：
> 不行，核心矛盾是 **HTTP 超时 vs 大校排课真实耗时**：
> - Nginx 默认 60s proxy_timeout，Tomcat 默认 30s asyncTimeout；
> - 一个真实中学 40 个班 × 6 节/天 × 5 天，几千条明细 + 大量冲突回溯，纯算法跑 3-5 分钟正常，加上 LLM 启发式调用 10 分钟也有可能；
> - 同步跑会：用户浏览器超时转圈 404 / 超时后用户点 2 次又发 2 条任务把服务打挂 / 进程被重启就丢进度。
>
> 我的解法是**异步状态机**：
> 1. Controller 收到"开始排课"只做 3 件事：权限校验 → 把 schedule_task 状态改成 `RUNNING`（带幂等键：只有 PENDING 才能转 RUNNING，重复点不重复跑）→ `scheduleExecutor.submit` 丢给自定义线程池立即返回 200；
> 2. 前端轮询 `/api/schedule-task/{id}` 看 status；
> 3. 线程池里真正跑算法+LLM，每 50 条写一次进度 run_log；
> 4. 完成后状态 `COMPLETED` / 异常 `FAILED`（堆栈也存 run_log），还自动发一条 system 公告。
>
> 这样 HTTP 和业务完全解耦，用户可以关浏览器喝茶，回来再看结果，重启后端也只丢正在跑的那一个任务。

---

### Q4. 你这个项目的 AI 具体做了什么？不会就是调了个 LLM 的 hello world 吧？
**面试官想知道**：是不是真把 AI 融到业务闭环里，不是套壳。
**满分回答**：
> 有两个真实的 AI 业务闭环，不是"问 AI 一句话"：
>
> **① 排课软约束的启发式打分（核心）**：
> 硬约束（老师/教室/同班级同一时段只能有一节）我用 Java 传统冲突检测循环保证 0 冲突；
> 但软约束（同课程一天不超过 2 节、主课尽量排上午、体育不连排、老师跨校区不安排同一天 1-8 节连跑）这些是多目标优化，传统 if-else 写 300 行还调不出来；
> 我的做法是把当前候选时间段 + 已排好的上下文压缩成结构化 prompt 发给 SiliconFlow 的 Qwen3-32B，让 LLM 输出 "评分 1~10 + 调整建议"，我自己在 Java 侧再做一次评分范围校验（越界直接用默认 5 分），防止幻觉。最终 AI 给出的方案**比纯贪心在"跨校区次数"指标上少了 62%**，教师满意度明显提升。
>
> **② 教学评价数据 → AI 质量分析报告（真实业务落地）**：
> 学生在学生端对一门课从 4 个维度（态度/内容/方法/效果）打分 + 写评语；系统自动算出总分。老师 / 教务可以点"生成 AI 分析报告"，我用 4-shot prompt（3 份历史真实的好报告 + 当前这门课所有评价）让 LLM 输出「亮点 3 条 + 改进建议 3 条 + 风险提示」，直接落表 `ai_quality_analysis`。
>
> 而且为了 AI 调用稳定，我自己写了 `SpringAiModelFactory`：① 自动探测系统代理（127.0.0.1:7890 等 4 个端口连通性）避免国内直连超时；② 429 Too Many Requests 自动指数退避重试（1s / 2s / 4s / 8s）；③ 每次调用落 `ai_call_log`（耗时/Token/状态）方便排查。

---

## 🟡 中间件 & 生产加固类（二面必问 6 题）

### Q5. 为什么不用默认 @Async 线程池？要自己写一个？
**面试官想知道**：懂不懂线程池参数和资源隔离。
**满分回答**：
> Spring Boot 默认的 `SimpleAsyncTaskExecutor` 是"来一个任务建一个线程"，**没有上限**，100 个排课任务就是 100 个线程，机器直接 OOM。
>
> 我写了 `AsyncConfig` 两个独立 `ThreadPoolTaskExecutor`：
> - **scheduleExecutor（重型排课池）**：核心 4 / 最大 8 / 队列 16 / 线程名前缀 `schedule-`。因为排课是 CPU + I/O 混合（算法 + DB + LLM 调用），4 核机器最多让它跑 8 条同时跑，机器不会炸；
> - **sysExecutor（轻型杂项池）**：核心 4 / 最大 20 / 队列 200 / 前缀 `sys-`。给批量导入学生、发公告、改密码发邮件、记录 run_log 这种小任务用，**不跟重型排课抢线程**；
> - **拒绝策略是 CallerRunsPolicy**：队列满了**不丢任务**（AbortPolicy 直接抛用户就丢课了！），而是让提交任务的 HTTP Tomcat 线程自己跑这个任务，Tomcat 就会暂时不接新请求，形成"天然背压"，教务场景宁可慢一点也不能排一半丢一半。
>
> 同时实现了 `AsyncConfigurer#getAsyncUncaughtExceptionHandler`，线程池里抛的异常不会吞日志，会打 stack 到 error 文件里。

---

### Q6. 限流和验证码你是怎么做的？为什么不用 Redis？
**面试官想知道**：工程取舍，不是什么都往 Redis 上贴。
**满分回答**：
> 架构取舍：这个系统部署形态是单节点中小学校（5k 用户以内），单机 Caffeine 足够，引入 Redis 反而多一个故障点 + 增加运维成本。**能单机搞定的事坚决不上分布式中间件**。
>
> 我写了 3 个东西：
> 1. **CaptchaService（图形验证码）**：Java 2D 直接画 4 位字母+数字，带干扰线防 OCR。用 Caffeine 缓存 `captchaKey → code`，过期时间 120s。校验完立即删（一次性验证码，防重放）；
> 2. **RateLimitService（双维度限流）**：也是 Caffeine，计数窗口用"当前整分钟 / 当前整 3 分钟"做 key：
>    - IP 维度：`rl:ip:{ip}:{minute}` → 上限 10；
>    - 用户名维度：`rl:user:{username}:{3minute}` → 上限 3（防脚本爆破某一个账号密码）；
> 3. **注册总开关**：`AuthProperties.registrationEnabled`，prod yml 默认 `false`（学校教务系统一般**不开放自由注册**，学生老师都是 admin 批量导入的）。
>
> 上集群（>2 节点）的时候怎么平滑切 Redis？—— `CaptchaService` 和 `RateLimitService` 都是接口封装好的，只换实现类，Controller 和 AuthService 一行不用改。

---

### Q7. 外键约束里 CASCADE / SET NULL / RESTRICT 你怎么选？举例子。
**面试官想知道**：你不是只会建外键，是真懂业务含义。
**满分回答**：
> 我是按"被引用的那条记录被删后，引用方应该怎么办"选的，我一共写了 40 条外键，3 种策略都用到了，不搞一刀切：
>
> ① **`ON DELETE CASCADE`（强依赖，没爹就不该存在）**：
> - `schedule_version_detail.schedule_version_id → schedule_version.id`：版本明细是版本的子记录，版本删了明细当然一起删，不然孤儿数据会把"已发布课表"搞乱；
> - `schedule_conflict.schedule_task_id → schedule_task.id`：冲突是跑任务时产生的，任务没了冲突也没意义；
>
> ② **`ON DELETE SET NULL`（软依赖，爹没了儿子继续活着）**：
> - `user.class_id → clazz.id`：一个班被删了（比如届毕业清掉"高三(1)班"），学生账号必须还在（要登录看历史成绩/评教），所以把 class_id 置 NULL，**绝对不能 CASCADE 把学生删了**，这是事故级的！；
> - `clazz.head_teacher_id → teacher.id`：班主任离职删老师，班级还在，把 head_teacher_id 置空等待新指派；
>
> ③ **`ON DELETE RESTRICT`（禁止删还在被引用的）**：
> - 所有"基础数据字典表"：term / campus / course / room 这些，只要被 schedule_detail 引用了，就不允许删基础数据——不然课表里出现了"课程ID=42 不存在"，前端渲染空白。要删先把课表版本整个回滚/归档。

---

### Q8. HikariCP 参数你怎么调的？dev 和 prod 有什么区别？
**面试官想知道**：是不是只会默认值，懂不懂连接池核心参数。
**满分回答**：
> 核心是 **dev 省资源 / prod 防打挂** 两套独立参数，写在 `application-dev.yml` 和 `application-prod.yml` 里：
>
> **dev（小规格，热改 debug 用）**：
> - `minimum-idle: 2` / `maximum-pool-size: 5` → 开发机器 8G 内存 + MySQL 本机，够用不占资源；
> - `idle-timeout: 60000`（1 分钟空闲回收）+ `leak-detection-threshold: 2000`（2s 没关连接打 WARN，开发期抓连接泄漏非常好用）；
>
> **prod（生产 2C2G → 4C8G 通用规格）**：
> - `minimum-idle: 10` / `maximum-pool-size: 20` → MySQL 默认 max_connections=151，我取 13%，8 个应用实例也撞不到 151；
> - `idle-timeout: 300000`（5 分钟，避免频繁建 TCP）/ `max-lifetime: 1740000`（29 分钟，比大多数网络设备 30 min 闲置切断少 1 分钟，防止拿到断连接）；
> - `connection-timeout: 8000`（最多等 8s 拿连接，超过直接报错，不会让 HTTP 排队全挂死）；
> - `leak-detection-threshold: 30000`（30s，生产期只抓真正的泄漏，排除慢查询）。
>
> 另外我做了一件很重要的事：**生产关 SQL 日志**，MyBatis 用 `configuration.log-impl: org.apache.ibatis.logging.nologging.NoLoggingImpl`，不然把学生手机号/密码哈希打到 stdout，被 Docker 日志服务采集走，就是数据泄露事故。

---

### Q9. 多环境配置拆分你做了什么？prod 里 DB_PWD 为什么不写默认值？
**面试官想知道**：懂不懂"安全左移"，会不会踩"开发配置上生产"经典坑。
**满分回答**：
> 我拆成三份：`application.yml`（所有环境共用的通用项，比如应用名、JWT 有效期、edu.cors 前缀等）+ `application-dev.yml` + `application-prod.yml`，通过 `spring.profiles.active` 切换。
>
> 安全原则：**凡是"在开发环境合理、上生产就会出事"的配置，一律不写到通用 yml，必须到 dev/prod 各自写，而且 prod 给的默认值一定是"最严格的那个"**。
>
> 最典型的就是 `DB_PWD` 和 `JWT_SECRET` 两个敏感信息：
> - **两个一律不给冒号默认值**，全部写成 `${DB_PWD}` / `${JWT_SECRET}`。原因：本仓库是公开仓库，默认值一旦写进 yml 就等于把口令/密钥公开——尤其 JWT 密钥泄露 = 任何人可伪造 token 绕过 RBAC。不设默认值还有个额外好处：忘了注入环境变量时 Spring Boot 启动**直接失败**（报 "Could not resolve placeholder"），比"默默用默认口令跑了一个月被拖库"强一万倍。
> - 本地开发的便利性用**不入库的 `.env`**（已加入 `.gitignore`）解决：启动前从 `.env` 读 `DB_PWD` / `JWT_SECRET` 注入环境变量即可，既不写死也不影响本地一键启动。
>
> 另外 prod 里我还默认关了 2 个：① `edu.auth.registration-enabled: false`（学校教务不开放注册）② 限流阈值 IP 5 次 / User 2 次，比 dev 更严。

---

### Q10. CORS 为什么不能 `allowCredentials=true + *`？
**面试官想知道**：有没有踩过 CORS 的坑，懂不懂规范。
**满分回答**：
> 这是 W3C 的 CORS 规范明确禁止的组合：`Access-Control-Allow-Credentials: true` 时，`Access-Control-Allow-Origin` 绝对不能是 `*`，主流 Chrome/Safari 直接拒绝响应。
>
> 另外 `*` 本身就是安全漏洞：意味着任何网站（比如恶意 phishing.com）都能带你的 Cookie/JWT 发跨域请求（CSRF 变种）。
>
> 我的做法是"白名单配置化"：
> - 在 `CorsConfig` 加 `@ConfigurationProperties(prefix="edu.cors")` 绑定 `List<String> allowedOrigins`；
> - prod yml 默认空（空就一个都不允许，最严格），本地临时测用命令行 `--edu.cors.allowed-origins[0]=http://内网IP:80` 覆盖，不用改代码重打包。
> - 同时 `allowedMethods = [GET,POST,PUT,DELETE,OPTIONS]`（明确列出，不走 `*`），`maxAge: 3600`（预检缓存 1 小时，省 80% OPTIONS 流量）。

---

## 🔴 高级 & 场景追问类（三面 / 答辩加问 5 题）

### Q11. 排课过程中 LLM 输出了不合理的时间段怎么办？怎么防幻觉？
**面试官想知道**：真做过 LLM 落地的人都知道 LLM 不靠谱，你怎么兜底。
**满分回答**：
> 核心原则：**"LLM 只做建议，落地前一定再跑一遍传统规则"**，绝不相信 LLM 的输出直接入库。我的 Agent 里有三层防御：
>
> ① **Schema 约束**：Spring AI 用 `BeanOutputConverter` 把 LLM 输出约束成固定的 Java Bean（`List<ScheduleCandidate>`，里面必须有 clazzId/courseId/teacherId/roomId/day/slot），字段类型不对 / 缺字段 → 直接抛 ParseException 丢弃这次 LLM 结果，重跑。
>
> ② **硬约束校验兜底**：拿到 LLM 给的每一个候选时间段，**立即再跑一遍本地写的冲突检测循环**（检查 teacher/day/slot 冲突、room/day/slot 冲突、clazz/day/slot 冲突，3 个 HashMap 计数 O(1)）。只要 0.000001% 概率过不了 → 丢这个候选，换 AI 给的第二高分；都不行就用贪心兜底。
>
> ③ **评分范围校验**：LLM 给每个候选的"软约束评分 1~10"，Java 侧强制 `Math.min(10, Math.max(1, score))` 截断，防止 AI 输出 9999 把后面的排序全部带偏。
>
> 这套下来，AI 胡说八道最多导致"这次没给出更好的方案，退回纯贪心"，**绝不会出现课表冲突**——课表 0 冲突是硬指标，半点儿含糊不得。

---

### Q12. JWT 的缺点你知道吗？token 泄露了怎么办？你这个项目怎么应对？
**面试官想知道**：有没有深入想过 JWT 安全，不是只会生成。
**满分回答**：
> JWT 最大缺点就是**服务端不可主动作废**。一旦被 XSS 偷走，有效期（我设的 7 天）内一直能用来请求。
>
> 我的多层防御：
> ① **放 localStorage，不放 Cookie** → 天然防 CSRF（Cookie 会自动被浏览器带，localStorage 不会），但 XSS 能偷 localStorage；
> ② **全局 CSP + HttpOnly 响应头 + 前端 axios 全局拦截**：Spring Security Header 级 `X-Content-Type-Options` / `X-Frame-Options` / strict CSP（禁止 unsafe-inline script）；
> ③ **用户改密码/退出登录时在 User 表写一个 `last_token_invalid_before` 时间戳（我在 user 表加了字段），JwtAuthInterceptor 拦截时判断 `jwt.issuedAt < last_token_invalid_before` 直接拒绝** → 这样改密码后**所有之前签发的 token 全部失效**，哪怕没过期。这是 JWT 无状态体系下的主流做法，不需要引入 Redis 黑名单也能做到 99% 场景的主动作废；
> ④ **prod 用 HTTPS（Nginx 配 certbot + HSTS）**，TLS 链路层防止中间人抓包看 token。

---

### Q13. Docker Compose 里你怎么保证后端等 MySQL 启动完再启动？
**面试官想知道**：懂不懂容器编排顺序坑（`depends_on` 只等容器 running，不等 MySQL 里面可以 accept connection）。
**满分回答**：
> 对，`depends_on` 只保证 Docker 按顺序创建容器，但 MySQL 容器 running 不代表 mysqld 可以接 SQL（第一次启动还要建 data dir，要 20~30 秒），后端容器会在 10 秒内连不上 DB 直接 CrashLoopBackOff。
>
> 我的做法是 **Spring Boot 自带 + Docker healthcheck 两层**：
> ① **MySQL 侧写 healthcheck**：`test: ["CMD-SHELL", "mysqladmin ping -h localhost -uroot -p${DB_PWD} --silent || exit 1"]`（口令取环境变量，不写死），`interval: 10s` / `timeout: 5s` / `start_period: 30s` / `retries: 6`，等 mysqladmin ping 成功才算 healthy；
> ② **后端 depends_on 加 condition: service_healthy**：`depends_on: mysql: condition: service_healthy`，docker compose 会等到 mysql 变 healthy 再 start backend；
> ③ **后端自己也有重连机制**（HikariCP `connection-timeout: 8000` + Spring Retry 可加 `@Retryable`），三层保险。
>
> 这样后端启动 100% 能连到 DB，不会出现第一次部署手忙脚乱 restart。

---

### Q14. 你们这个系统如果学校要 1 万个学生同时登录看课表，瓶颈在哪？怎么优化？
**面试官想知道**：懂不懂性能瓶颈分析，不是说"加机器"就完了。
**满分回答**：
> 先定位瓶颈：1 万人同时看课表 → 全是**读请求**，写请求很少（只有教务偶尔排课改课），瓶颈在 3 处：
>
> ① **Nginx 静态资源（1.74MB）×1 万并发**：Nginx 单机就能撑 5 万 QPS，配合我现在配置的 **带 hash 文件名** + `Cache-Control: immutable` 强缓存，首屏之后 80% 是浏览器本地读，这部分没问题。升级到 10 万用户就是 Nginx 前面挂一个 CDN；
>
> ② **Spring Boot 查课表接口 `/api/student/schedule`**：每个请求查 schedule_version_detail 按 class_id 过滤。现在是直查 MySQL，单条 <10ms，但 1 万并发 = 10 万 QPS，MySQL 单实例读扛不住；
>   → 优化：**加 Redis 做"班级课表 + 教师课表缓存"**，key = `schedule:v{versionId}:c{classId}`，value 是前端要的 5×8 二维 JSON，TTL 10min；排课版本一发布就 INCR versionId → 自动失效。再加 2 台 Spring Boot 实例做 Nginx 轮询；
>
> ③ **MySQL 连接池 max=20 × 2 实例 = 40**：对于 10 万 QPS 肯定不够；→ 升级到 3 节点 MySQL Group Replication 做读写分离，80% 的读请求走从库。
>
> 我现在的架构（单体 + 无状态后端 + 状态集中到 DB）水平扩展非常顺，不用改代码，Spring Boot 实例数从 1 加到 20 就是改 `docker-compose.yml` 的 `deploy.replicas`。

---

### Q15. 这个项目你最大的技术挑战是什么？怎么解决的？
**面试官想知道**：看你解决问题的思路，看你是不是只会 CRUD。
**满分回答**：
> 最大挑战不是写代码，是**"怎么把 AI 真正用在教学业务里而不是套壳 + 怎么同时兼顾生产级安全加固"**这两件事结合起来。
>
> 一开始我也走过弯路：
> - 第一版排课想让 LLM 直接输出"完整课表 JSON 数组"→ 输出 100 次有 99 次冲突，1% 对的那次还有 1/3 节数没填，完全不可用；
> - 第一版权限直接全放 admin，三端接口通吃，当时觉得"反正是练习项目"。
>
> 后来我做了 2 个关键"拆分"决策：
> 1. **"AI + 传统规则"拆分**：硬约束（冲突）100% 用 Java 本地算法保证，软约束（评分）100% 交给 AI 做。各自专注自己擅长的：算法擅长精确，AI 擅长多目标优化。结果就是既 0 冲突，又比纯贪心在跨校区次数上优化 62%；
> 2. **"功能开发 + 生产加固"拆分**：我在跑通 MVP 之后，花了整整 3 天专门做 7 项加固（权限/CORS/多环境/Hikari/外键/线程池/限流），而不是"边写功能边顺手加"，这样才能逐项验证，不会出现"加固一半留一半"的半吊子。
>
> 最终这个项目从"一个 Vue + Spring Boot demo"，变成了**真的可以给一个 18 个班的学校用一个学期不出大问题的系统**，我觉得这个拆分思维的转变是我最大的收获。

---

## 🌟 一句话总结（面试最后被问"你还有什么想补充的"可以说）
> 这个项目我最大的收获，是真正理解了"工程和 demo 的差距不在功能，而在**约束**"。约束有：业务约束（课表绝对不能冲突）、安全约束（学生不能越权、注册不能被刷、密钥不能打日志）、性能约束（排课不能阻塞 HTTP）、运维约束（容器不能因为 MySQL 启动慢反复重启、敏感信息不能硬编码、日志不能写爆磁盘）。把这些约束全部考虑到并落地，才是一个真正能叫"项目"的东西，而不是一个课程作业。
