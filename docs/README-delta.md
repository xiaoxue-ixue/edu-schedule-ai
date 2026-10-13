# README.md 增补 & FAQ 新增条目（2026-08-19 小收尾 D4）
> 这是对 `D:\TRAE SOLO CN\edu-schedule\README.md` 的**增量补充**，把今天做的 D 选项（4 件事）+ Docker/WSL2 FAQ + scripts/kill-and-start.ps1 怎么用都补上。
> 嫌手动合并麻烦？直接翻到最后 Copy 一节贴两条命令，把本 md 作为附录留着也行。

---

## 📌 一、README 顶部「快速目录」新增 4 条锚点
在目录区加下面 4 行（保持编号连续，插在「7. 常见问题 FAQ」前面）：
```markdown
4. 🔄 一键杀端口 & 启动脚本（scripts/kill-and-start.ps1）
5. 📦 生产构建优化（Vite 拆 chunk / 消日志 WARN）
6. 🗂️ docs/ 项目文档 & 包装材料
7. 🐳 Docker 部署（WSL2 要求 · 国内镜像加速 · .env 配置）
8. 常见问题 FAQ
```

---

## 📝 二、README 新增 3 个正式章节（按目录顺序，放 FAQ 前面）
### 🔄 4. 一键杀端口 & 启动脚本（scripts/kill-and-start.ps1）
开发时最讨厌的就是「上次 Ctrl+C 没杀干净，8080/5173 还在跑 → 启动报端口占用」。项目根目录 `scripts/kill-and-start.ps1` 一键搞定：

**支持菜单（选 1 / 2 / 0）：**
```
[1] DEV 模式（推荐日常改代码）
     · 自动杀 80 / 5173 / 8080 / 3307 端口
     · 后端：新 PS 窗口跑 mvn spring-boot:run '-Dspring-boot.run.profiles=dev'（自动开注册）
     · 前端：当前窗口 npm run dev → http://localhost:5173
[2] PROD JAR 模式（不用 Docker / 不用 WSL2 · 本地生产验证）
     · 自动杀端口 + 环境变量（DB_PWD / JWT_SECRET 从项目根 .env 读取，不硬编码）
     · 第一次运行自动 mvn package '-DskipTests' 打 JAR 40.87MB
     · 第一次运行自动 npm run build 出前端 dist 1.74MB
     · 后端 JAR 2C2G 启动 → 提示用 Nginx 或 npx vite preview 托管前端
[0] 只杀端口（用来清旧进程，不启动任何服务）
```

**使用方法（右键运行最方便）：**
- 打开 D:\TRAE SOLO CN\edu-schedule\scripts → **右键 kill-and-start.ps1 → 使用 PowerShell 运行**
- 或 PS 里：`cd "D:\TRAE SOLO CN\edu-schedule\scripts" ; .\kill-and-start.ps1`
- PowerShell 默认策略不让执行脚本？首次跑（管理员 PS 只执行 1 次）：
  ```powershell
  Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
  # 选 Y（允许本地脚本运行，从网上下的脚本需要签名）
  ```

---

### 📦 5. 生产构建优化（Vite 拆 chunk / 消 Logback 2 条 WARN）
#### 5.1 前端：Vite manualChunks 拆 vendor（消 chunk>500KB 警告）
之前打包 `npm run build` 会报警告：`(!) Some chunks are larger than 500 kB`。
现在已经在 `vite.config.js` 里按依赖拆成了 **4 vendor + 2 app**：
```
vendor-vue (~180KB)        Vue3 全家桶（vue/vue-router/pinia）
vendor-element-plus (~350KB) ElementPlus + dayjs 中文字体
vendor-libs (~120KB)       echarts / axios
vendor-others              其余依赖
app-layout / app-api       业务代码（布局 / API 层）
```
同时**输出文件名全部带 hash**（`assets/js/vendor-vue-[hash].js`），与 Nginx `Cache-Control: immutable` 配合 → **浏览器强缓存 1 年**，代码一改 hash 自动变 → 自动拉新版本，不用让用户手动清缓存。

**验证（自己跑一次就看到效果）：**
```
cd D:\TRAE SOLO CN\edu-schedule\edu-schedule-ai-web
npm run build
# 输出：dist/assets/js/ 下会出现 vendor-vue-xxx.js / vendor-element-plus-xxx.js 等，不再有 1.2MB 单文件。
```

#### 5.2 后端：Logback 关闭配置扫描（消 2 条 Watching WARN）
打包 JAR 启动时，Logback 会打两条没必要的 WARN：
```
WARN - Missing watchable .xml or .properties files.
WARN - Watching .xml files requires that the main configuration file is reachable as a URL
```
原因：JAR 里的 `logback-spring.xml` 是 `jar:nested:...` URL，Logback 没法当本地文件监听变更。
修复：`logback-spring.xml` 根标签 `scan="false"`（生产本就不允许热改日志配置），两条 WARN 直接消失，prod 启动 100% 纯 INFO。

---

### 🗂️ 6. docs/ 项目文档 & 包装材料
项目根目录 `docs/` 放了 4 份**简历/答辩/后续开发**直接能用的材料：
```
docs/
├── architecture-diagram.svg        ① 总体架构 SVG 图（1 页贴简历/PPT 封面背景直接用）
├── interview-qa-15.md              ② 15 道高频面试题 + 满分回答（按基础-中间件-高级分类）
├── ppt-outline-10-pages.md         ③ 答辩 PPT 大纲（10 页，每页要点+技巧+最后金句）
└── batch-import-export-design.md   ④ 批量导入导出功能蓝图（EasyExcel+3表+6接口+伪代码+坑单，3 天就能写完）
```
**怎么用：**
- 贴简历 → 先看 `② interview-qa-15.md` 前 4 题和最后金句；
- 做答辩 PPT → 直接按 `③ ppt-outline-10-pages.md` 顺序填内容，`① architecture-diagram.svg` 拖进第 3 页；
- 想加功能 → 直接按 `④ batch-import-export-design.md` 写代码，不用再想"第一步做什么"。

---

## 🤔 三、README 原「8. 常见问题 FAQ」末尾新增 4 条
（插在 "Q: A 路径 B 路径哪个好？"之后）

### Q8: Docker Desktop 启动提示 "WSL is not installed" 怎么办？
A: 这是 Windows 没启用 Linux 子系统（Docker Desktop 默认用 WSL2 当后端），**管理员 PowerShell 跑 1 条命令重启就行**：
```powershell
wsl --install
```
重启后 Ubuntu 初始化会要你设一个 WSL 内部用户名密码（随便设，不影响 Windows）。再开 Docker Desktop 等鲸鱼 🐳 变绿（显示 Docker Desktop is running）就 OK。
不想装 WSL2？→ **直接用 PROD JAR 模式（上面第 4 节的 kill-and-start 菜单 2）**，完全不用 Docker / WSL2，一样能本地体验生产级规格。

### Q9: docker compose build 拉镜像特别慢（几十 KB/s）怎么办？
A: 国内网络连 Docker Hub (hub.docker.com) 经常限速。**Docker Desktop → Settings → Docker Engine**，加下面 `registry-mirrors` 数组（保存 Apply & Restart，拉镜像速度直接 5MB/s+）：
```json
{
  "builder": { "gc": { "defaultKeepStorage": "20GB", "enabled": true } },
  "experimental": false,
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://dockerproxy.com",
    "https://docker.nju.edu.cn",
    "https://docker.mirrors.ustc.edu.cn"
  ]
}
```

### Q10: docker compose up 看到 backend / frontend 反复 Restarting？如何看报错？
A: 先跑 2 条命令 99% 定位问题：
```powershell
cd D:\TRAE SOLO CN\edu-schedule
# 看三台健康状态
docker compose ps
# 看 backend 的最后 200 行日志（缺环境变量 / DB 连不上 / 外键报错都在这里）
docker compose logs -f --tail=200 backend
```
最常见 3 种原因：
1. `.env` 没配 → 直接 3 条没定义 → 后端启动失败（拷贝 `.env.example` 为 `.env` 填好 3 条）；
2. MySQL 第一次初始化慢（20-30 秒）→ backend 健康检查失败，等 3 分钟自动恢复；
3. 80 端口被系统占用（IIS / Windows IIS Service 默认为 80 占着）→ 先跑 `scripts/kill-and-start.ps1 选项 0` 杀 80 端口再 up。

### Q11: `.env` 里的变量值想改，要重新 build 镜像吗？
A: **不用！** `.env` 是 docker compose 启动时读的环境变量（注入容器 `environment:`），改完只要：
```powershell
docker compose down        # 停三容器（命名卷数据不会丢）
docker compose up -d       # 用新 .env 重新注入，不重新 build
```
只有改了 `Dockerfile` / 源代码（Java/Vue）才需要 `docker compose up -d --build`。

---

## 📎 四、嫌手动合并麻烦？Copy 脚本把本 README-delta.md 也一并放到 D 盘 docs
把 `docs/README-delta.md` 作为附录留着，想改 D 盘 README.md 时，直接把本文件的第 2 章和第 3 章复制粘贴过去就行。

---

## ✅ 附录：如何一键把所有小收尾 + 包装材料 复制到 D 盘（1 条命令）
（已经放在主 PS 大命令里，单独列在这方便以后重跑）
