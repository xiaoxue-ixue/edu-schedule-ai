# ============================================================
#  智能教务 AI 排课系统 — 一键"杀端口 + 启动"脚本（Windows PowerShell 5.1+）
#  作用：
#    · 自动杀掉占用 80 / 5173 / 8080 / 3307 端口的进程（防止端口冲突）
#    · 提供菜单：1) Dev 模式  2) Prod JAR 模式（不用 Docker，不用 WSL2）
#    · 自动设置 EDU_AUTH_REGISTRATION_ENABLED / DB_PWD / JWT_SECRET 等环境变量
#  使用：
#    1) cd 到脚本所在目录（D:\TRAE SOLO CN\edu-schedule\scripts\）
#    2) 右键脚本 → "使用 PowerShell 运行"，或在 PS 里执行：
#       .\kill-and-start.ps1
#  注意：
#    · 杀进程会弹 "此操作将终止此进程。是否继续?"，全部选 Y（默认 Y 直接回车也行）
#    · 第一次 Prod 模式需要先跑过一次 mvn package '-DskipTests' 出 JAR
# ============================================================

$ErrorActionPreference = "Continue"
Set-StrictMode -Off
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

# --- 1. 定位项目根 ---
$SCRIPT_DIR = Split-Path -Parent $MyInvocation.MyCommand.Path
$PROJECT_ROOT = (Resolve-Path (Join-Path $SCRIPT_DIR "..")).Path
$BACKEND_DIR = Join-Path $PROJECT_ROOT "edu-schedule-ai"
$FRONTEND_DIR = Join-Path $PROJECT_ROOT "edu-schedule-ai-web"

Write-Host "==============================================================" -ForegroundColor Cyan
Write-Host " 智能教务 AI 排课系统 · 一键启动菜单" -ForegroundColor Cyan
Write-Host " 项目根目录 : $PROJECT_ROOT" -ForegroundColor Gray
Write-Host " 后端目录   : $BACKEND_DIR" -ForegroundColor Gray
Write-Host " 前端目录   : $FRONTEND_DIR" -ForegroundColor Gray
Write-Host "==============================================================" -ForegroundColor Cyan

# --- 2. 杀占用端口（80 / 5173 / 8080 / 3307） ---
function Kill-Port($port) {
    try {
        $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
        if ($conns -and $conns.Count -gt 0) {
            $pids = $conns | ForEach-Object { $_.OwningProcess } | Sort-Object -Unique
            foreach ($pid_ in $pids) {
                if ($pid_ -le 0) { continue }
                $proc = Get-Process -Id $pid_ -ErrorAction SilentlyContinue
                $procName = if ($proc) { $proc.ProcessName } else { "Unknown" }
                Write-Host "⚡ 发现端口 $port 被 PID=$pid_ ($procName) 占用，已自动释放" -ForegroundColor Yellow
                Stop-Process -Id $pid_ -Force -ErrorAction SilentlyContinue | Out-Null
            }
        }
        else {
            Write-Host "✔ 端口 $port 空闲" -ForegroundColor Gray
        }
    } catch {
        Write-Host "✘ 检测端口 $port 失败（跳过）: $_" -ForegroundColor DarkGray
    }
}

Write-Host ""
Write-Host "🔪 第一步：释放常用端口 (80 / 5173 / 8080 / 3307)" -ForegroundColor Magenta
Kill-Port 80     # Nginx / Docker frontend
Kill-Port 5173   # Vite dev server
Kill-Port 8080   # Spring Boot backend
Kill-Port 3307   # Docker MySQL（防止以后开了没关）

Start-Sleep -Seconds 1
Write-Host "✔ 端口清理完成" -ForegroundColor Green

# --- 3. 打印启动菜单 ---
Write-Host ""
Write-Host "📋 请选择启动模式：" -ForegroundColor Magenta
Write-Host "  [1] DEV 模式（推荐，改代码/调试用）" -ForegroundColor Cyan
Write-Host "       后端：mvn spring-boot:run '-Dspring-boot.run.profiles=dev' + 开注册"
Write-Host "       前端：npm run dev (Vite，热更新)"
Write-Host ""
Write-Host "  [2] PROD JAR 模式（不用 Docker / 不用 WSL2 · 本地生产验证）" -ForegroundColor Cyan
Write-Host "       先 mvn package 打包（target/edu-schedule-ai-1.0.0.jar）"
Write-Host "       然后 java '-Dspring.profiles.active=prod' -jar target\edu-schedule-ai-1.0.0.jar"
Write-Host "       前端：自动 dist 构建，提示用 Nginx 托管或临时用 Vite 预览"
Write-Host ""
Write-Host "  [0] 只杀端口，不启动" -ForegroundColor Cyan
Write-Host ""

$choice = Read-Host "请输入 1 / 2 / 0（默认 1）"
if ([string]::IsNullOrWhiteSpace($choice)) { $choice = "1" }

# --- 4. DEV 模式 ---
if ($choice -eq "1") {
    Write-Host ""
    Write-Host "🚀 DEV 模式启动中... (后端会开一个 PS 新窗口；前端在当前窗口)" -ForegroundColor Green

    # 4a. 启动后端（新窗口，保持 8080）
    $backendCmd = @"
Set-Location '$BACKEND_DIR'
`$env:EDU_AUTH_REGISTRATION_ENABLED='true'
Write-Host '后端目录:' (Get-Location) -ForegroundColor Cyan
mvn spring-boot:run '-Dspring-boot.run.profiles=dev'
"@
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd -WindowStyle Normal
    Start-Sleep -Seconds 3

    # 4b. 启动前端（当前窗口保持，跑 Vite）
    Write-Host "→ 前端（Vite Dev Server · http://localhost:5173）" -ForegroundColor Cyan
    Set-Location $FRONTEND_DIR
    npm run dev
    exit 0
}

# --- 5. PROD JAR 模式 ---
if ($choice -eq "2") {
    Write-Host ""
    Write-Host "🏭 PROD JAR 模式启动中...（不用 Docker / WSL2）" -ForegroundColor Green

    # 5a. 环境变量（本地测试默认：DB_PWD=123456 / JWT_SECRET 32 位固定，上生产请改强随机）
    $env:DB_PWD    = "123456"
    $env:JWT_SECRET = "edu-schedule-prod-secret-key-2026-xxxxxxxxxxxx"

    # 5b. 打包后端（如果 target 下还没有 JAR）
    $jarPath = Join-Path $BACKEND_DIR "target\edu-schedule-ai-1.0.0.jar"
    if (-not (Test-Path $jarPath)) {
        Write-Host "→ 第一次运行 PROD，先 Maven 打包后端（约 2~5 分钟）..." -ForegroundColor Cyan
        Set-Location $BACKEND_DIR
        mvn package '-DskipTests'
    }

    if (-not (Test-Path $jarPath)) {
        Write-Host "✘ 打包失败，未找到 JAR：$jarPath（请贴上面 mvn 的报错）" -ForegroundColor Red
        pause; exit 1
    }

    # 5c. 构建前端 dist
    $distDir = Join-Path $FRONTEND_DIR "dist"
    if (-not (Test-Path (Join-Path $distDir "index.html"))) {
        Write-Host "→ 构建前端 dist（npm run build · 约 1 分钟）..." -ForegroundColor Cyan
        Set-Location $FRONTEND_DIR
        npm run build
    }

    # 5d. 启动后端（新窗口）
    Write-Host "→ 启动后端 JAR（2C 2G · 健康端口 8080）" -ForegroundColor Cyan
    $backendCmd = @"
Set-Location '$BACKEND_DIR'
`$env:DB_PWD    = '$($env:DB_PWD)'
`$env:JWT_SECRET = '$($env:JWT_SECRET)'
Write-Host '后端 JAR 模式启动，PROD profile · JVM 堆限制 2GB' -ForegroundColor Cyan
java -XX:+UseZGC '-Xms2g' '-Xmx2g' '-Dspring.profiles.active=prod' '-jar' 'target\edu-schedule-ai-1.0.0.jar'
"@
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $backendCmd -WindowStyle Normal
    Start-Sleep -Seconds 8

    # 5e. 前端提示
    Write-Host ""
    Write-Host "✅ 后端 JAR 已在新窗口启动（等 30 秒 MySQL init 完成即可测）" -ForegroundColor Green
    Write-Host "→ 前端 dist 目录：$distDir" -ForegroundColor Cyan
    Write-Host "   · 方案 A（推荐长期）：用 Nginx for Windows 托管 → 根目录 nginx.conf 已配好 → 直接把 dist 拷到 nginx/html → 访问 http://localhost" -ForegroundColor Gray
    Write-Host "   · 方案 B（临时测 30 分钟）：在前端目录运行  npx vite preview --port 5173  → 访问 http://localhost:5173" -ForegroundColor Gray
    Write-Host ""
    Write-Host "三端测试账号：admin / teacher / student / stu0102 密码全是 123456" -ForegroundColor Cyan
    pause
    exit 0
}

# --- 0. 只杀端口 ---
Write-Host ""
Write-Host "✔ 仅执行了端口清理，未启动服务。按任意键退出..." -ForegroundColor Green
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
exit 0
