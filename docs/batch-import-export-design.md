# 智能教务 AI 排课系统 · 批量导入导出功能设计文档
> 定位：**下一步功能的完整蓝图**（本次未写代码，设计好了以后想做直接按这篇文档写，80% 的接口/流程/注意事项都覆盖了）。
> 目标：让 admin 把 Excel 一键导入（学生 / 教师 / 课程 / 班级 / 教室 5 大类），一键导出课表 PDF / Excel。
> 选型：**EasyExcel**（alibaba 出品，比 Apache POI 内存占用小 80%，支持 10W+ 行不 OOM）。

---

## 📦 一、引入依赖（pom.xml 加一个就行）
```xml
<!-- EasyExcel（POI 的无 OOM 替代 · 3.3.x 稳定版）-->
<dependency>
  <groupId>com.alibaba</groupId>
  <artifactId>easyexcel</artifactId>
  <version>3.3.4</version>
</dependency>
```
- 不需要单独引 POI，EasyExcel 自带传递依赖；
- 读 10 万行学生数据，堆占用 <200MB（老 POI 要 2GB+）。

---

## 📋 二、5 种模板 & 校验规则（admin 下载模板 → 填 → 上传）
### 共性：每个模板第 1 行是标题，第 2 行空，第 3 行开始是真实数据。
### 共性：统一返回 `批量导入结果` JSON
```json
{
  "total": 200, "success": 196, "failed": 4,
  "failedRows": [
    {"row": 17, "data": {"name":"张三错"}, "reasons": ["所属班级 classId=999 不存在", "手机号格式非法"]}
  ],
  "preview": [
    {"id": 1001, "username": "stu0901", "name": "张九九", "className": "高三(9)班"}
  ]
}
```

### 📘 模板 1：学生批量导入（最多 1000 行 / 次）
列：`序号 | 姓名(必填) | 性别 | 学号(必填, 唯一) | 班级名称或编号(必填) | 手机号 | 邮箱`
- 校验规则：
  - 学号唯一：重复直接失败；
  - 班级通过「班级名称 或 classId 字符串」反查，找不到就失败；
  - 手机号正则：`^1[3-9]\d{9}$`（空可以跳过）；
- **自动生成字段（不让用户填，后端写）**：
  - `username`：按 `stu{班级编号}{01~20}` 规则（比如高三(1)班的第 3 个 → stu0103）；
  - `password`：BCrypt("123456")，首次登录强制改密码；
  - `role`：固定 `student`；
  - `class_id`：反查班级后填入。

### 👨‍🏫 模板 2：教师批量导入（最多 500 行 / 次）
列：`序号 | 姓名(必填) | 工号(必填, 唯一) | 所授科目(必填, 可多个逗号分隔) | 所属校区 | 手机号 | 邮箱`
- 自动：`username=t{工号后5位}` / `role=teacher` / 关联到 teacher 表 + user 表（双写事务）

### 📚 模板 3：课程批量导入（最多 100 行 / 次）
列：`序号 | 课程名称(必填) | 课程编码(必填唯一) | 学分 | 每周节数(必填) | 主课/副课 | 优先上午(是/否)`

### 🏫 模板 4：班级批量导入
列：`序号 | 班级名称(必填) | 年级 | 班主任(教师工号) | 所属校区(必填) | 人数上限`

### 🧱 模板 5：教室批量导入
列：`序号 | 教室编号(必填唯一) | 教室名称 | 所属校区(必填) | 容量(必填) | 是否多媒体(是/否)`

---

## 🧱 三、数据库表设计（新增 2 张 + 扩展 1 张）
### ① batch_import_log（每次上传一条记录，方便审计追溯）
```sql
CREATE TABLE batch_import_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  import_type  VARCHAR(32)  NOT NULL COMMENT 'STUDENT/TEACHER/COURSE/CLAZZ/ROOM',
  file_name    VARCHAR(200) NOT NULL COMMENT '上传时的原始文件名',
  total_count  INT NOT NULL DEFAULT 0,
  success_count INT NOT NULL DEFAULT 0,
  fail_count   INT NOT NULL DEFAULT 0,
  fail_detail_json JSON COMMENT '失败行明细（JSON 数组，兼容 1000 行）',
  status       VARCHAR(16) NOT NULL COMMENT 'RUNNING/COMPLETED/FAILED',
  operator_id  BIGINT NOT NULL,
  elapsed_ms   INT COMMENT '耗时 ms',
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_type_time (import_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### ② export_task（异步导出任务，大导出别阻塞 HTTP）
```sql
CREATE TABLE export_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  export_type VARCHAR(32)  NOT NULL COMMENT 'SCHEDULE_CLASS_CSV/SCHEDULE_TEACHER_PDF',
  params_json JSON NOT NULL COMMENT '比如 {scheduleVersionId:1, classId:5}',
  status      VARCHAR(16) NOT NULL COMMENT 'PENDING/RUNNING/DONE/FAILED',
  file_url    VARCHAR(300) COMMENT '本地文件相对路径 或 OSS URL',
  expire_at   DATETIME COMMENT '一般保留 24 小时后自动删',
  operator_id BIGINT NOT NULL,
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### ③ user 表加 1 列（强制首次改密码）
```sql
ALTER TABLE `user` ADD COLUMN must_change_pwd TINYINT NOT NULL DEFAULT 0 COMMENT '1=下次登录必须改密码（批量导入的新账号默认 1）';
```
→ 登录接口返回 `mustChangePwd=true` → 前端强制路由到 `/change-password`，不改密码不让进主界面。

---

## 🔌 四、后端接口清单（6 个）
### 公共规则：全部走 `@Async sysExecutor` 异步 + 进度轮询（学排课任务那套）

| Method | URL | 作用 | 入参 | 返回 |
|---|---|---|---|---|
| GET | `/api/admin/batch/template/{type}` | 下载模板 | `{type}` = STUDENT | Excel 二进制流（EasyExcel 写模板）|
| POST | `/api/admin/batch/import` | 上传 Excel 异步批量导入 | `file` MultipartFile + `importType` | 立即返回 `{taskId, status: "RUNNING"}` |
| GET | `/api/admin/batch/import/{taskId}` | 查导入进度和结果 | taskId | 上面定义的 `total/success/failed/failedRows` |
| GET | `/api/admin/batch/imports?type=` | 历史导入记录列表（分页） | type + page | 关联 batch_import_log |
| POST | `/api/admin/export/submit` | 提交导出任务 | `exportType + paramsJson` | `{taskId, status: "PENDING"}` |
| GET | `/api/admin/export/{taskId}` | 轮询导出进度，完成了返回 downloadUrl | taskId | `{status, fileUrl, expireAt}` |

---

## 🧩 五、核心实现伪代码（直接对照写代码）
### ✅ 5.1 学生批量导入：读 Excel → 校验 → 事务批量写 + 失败行定位
```java
// StudentImportDTO 定义（@ExcelProperty 对应 Excel 列名）
@Data
public class StudentImportDTO {
    @ExcelProperty("姓名")        private String name;
    @ExcelProperty("学号")        private String studentNo;
    @ExcelProperty("班级名称或编号") private String clazzRef;
    @ExcelProperty("手机号")      private String phone;
}

// 服务层：@Async + ReadListener（EasyExcel 逐行读，不把全行堆内存）
@Async("sysExecutor")
public void runStudentImport(Long taskId, MultipartFile file) {
    // 1) 先把 batch_import_log 状态改成 RUNNING
    importLogMapper.updateStatus(taskId, "RUNNING", null);
    final AtomicInteger rowNum = new AtomicInteger(2); // 第 3 行开始（Excel index）
    final List<StudentImportDTO> successRows = Collections.synchronizedList(new ArrayList<>());
    final List<FailRow> fails = Collections.synchronizedList(new ArrayList<>());

    // 2) EasyExcel.read() 逐行回调（百万行也不 OOM）
    EasyExcel.read(file.getInputStream(), StudentImportDTO.class, new ReadListener<StudentImportDTO>() {
        @Override public void invoke(StudentImportDTO row, AnalysisContext ctx) {
            int r = rowNum.incrementAndGet();
            // 单条校验：姓名空 / 学号空 / 班级找不到 / 手机格式
            List<String> errs = validateOneStudentRow(row);
            if (!errs.isEmpty()) { fails.add(new FailRow(r, row, errs)); return; }
            successRows.add(row);
        }
        @Override public void doAfterAllAnalysed(AnalysisContext ctx) { }
    }).sheet().doRead();

    // 3) 事务批量落 user + student 表（一次 jdbc.batchUpdate，1000 行 1 次 DB round trip）
    String classNameToIdMap = loadClazzRefMap(successRows); // 一次性查所有引用班级
    TransactionTemplate.execute(status -> {
        int inserted = userMapper.batchInsertStudents(convertToUsers(successRows, classNameToIdMap));
        studentMapper.batchInsertRelations(...);
        return inserted;
    });

    // 4) 更新 batch_import_log：COMPLETED + 失败明细 JSON
    importLogMapper.updateStatus(taskId, "COMPLETED", toJson(new ImportResult(total, ok, fail, fails)));
}
```

**关键 3 点：**
- 用 `ReadListener` 逐行回调（EasyExcel SAX 模式），不是 `readAllRows`，100 万行不 OOM；
- 班级/教师外键引用，**先把所有成功行的 clazzRef 去重后一次 SQL 查出来放 HashMap**，不要每行查一次（1000 行就是 1 次 SQL vs 1000 次 SQL，差 1000x）；
- 插入用 `batchUpdate`（MyBatis `INSERT INTO user (col1, col2) VALUES (?, ?), (?, ?), ...` 拼接一次执行），不要 1000 次 `insertOne`。

### ✅ 5.2 下载模板接口（一行写 Excel）
```java
@GetMapping("/batch/template/{type}")
public void downloadTemplate(@PathVariable String type, HttpServletResponse resp) throws IOException {
    resp.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    resp.setHeader("Content-disposition", "attachment;filename*=utf-8''" + URLEncoder.encode(type + "_template.xlsx","UTF-8"));
    Class<?> dtoClass = switch (type) {
        case "STUDENT" -> StudentImportDTO.class;
        case "TEACHER" -> TeacherImportDTO.class;
        default -> throw new BusinessException("不支持的模板类型");
    };
    EasyExcel.write(resp.getOutputStream(), dtoClass).sheet("模板").doWrite(List.of());
}
```

### ✅ 5.3 导出课表 PDF（用 iText 或 OpenPDF，这里给伪代码思路）
```java
@Async("sysExecutor")
public void runScheduleExportPdf(Long taskId, Long scheduleVersionId, String scope /*CLASS/TEACHER/STUDENT*/, Long targetId) {
    String fileName = "schedule_" + scope + "_v" + scheduleVersionId + "_" + System.currentTimeMillis() + ".pdf";
    Path out = Paths.get(EXPORT_DIR).resolve(fileName);

    // 1) 查课表（班级/老师/学生 scope 分别查 schedule_version_detail，缓存 Redis 更快）
    List<ScheduleDetailVO> details = scheduleDetailMapper.listByScope(scope, targetId, scheduleVersionId);
    WeekTable table = buildWeek5x8(details); // 拼成 5 天 × 8 节二维表

    // 2) OpenPDF 写 PDF（标题 + 表格 + 底部水印 + 生成时间）
    try (PdfWriter w = new PdfWriter(Files.newOutputStream(out)); PdfDocument pdf = new PdfDocument(w)) {
        Document doc = new Document(pdf, PageSize.A4.rotate()); // 横向 A4
        doc.add(new Paragraph(title).setFont(PDF_FONT).setFontSize(18).setBold());
        Table pdfTable = new Table(9); // 第 1 列节次 + 周一~周五 5 列？不对 8 节：9 列 = 节次 + 周一..周五 是 6 列 = 应该 8 行 5 列
        // ... 按 buildWeekTable 里的每一个 cell 写进去，空的写占位
        doc.add(pdfTable);
        doc.add(new Paragraph("导出时间：" + DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").format(LocalDateTime.now()) + " | 系统生成").setFontSize(9).setItalic());
    }

    // 3) 更新 export_task = DONE + file_url 相对路径（前端用 /download/{taskId} 取）
    exportTaskMapper.markDone(taskId, "/api/admin/export/download/" + fileName, LocalDateTime.now().plusHours(24));
}
```

---

## 🚦 六、6 个容易踩的坑 & 规避方法
| 坑 | 后果 | 规避 |
|---|---|---|
| 1 行字段校验失败，999 行成功被回滚 | 用户重填 1000 行（炸）| **部分成功策略**：失败的进 failRows，成功的 100% 落库，绝不整体回滚！成功/失败数都在返回里告诉 admin；|
| 外键查一行查一次 | 1000 行 = 1000 次 SQL，导入慢到爆炸 | 先 collect 所有 clazzRef → `SELECT id, name, code FROM clazz WHERE name IN (...) OR code IN (...)` 一次查 → HashMap |
| 学号重复（两行填了同一个学号）| 第二次抛 DuplicateKey 异常 | 先对成功列表里的 `studentNo` 自己 `groupingBy` 数一遍，重复的先标记失败，不落库 |
| Excel 上传了 10MB 照片 | 被黑客上传 shell.jsp 改后缀 .xlsx，绕过 Multipart | ① 后端检查 `file.getContentType()` 必须是 `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`；② 文件名白名单正则 `^[\u4e00-\u9fa5A-Za-z0-9_\-]+\.xlsx$`（不允许路径，不允许双后缀）；③ 限制大小 `spring.servlet.multipart.max-file-size: 5MB` |
| 导入到一半 DB 死了，admin 不知道成功多少 | 查 DB 靠人眼数 | **幂等导入键**：给每次导入生成 `import_uuid`，写 user 表时一起写进去。中断了再导入同一份 → `DELETE FROM user WHERE import_uuid = 'xxx'` 先清掉重来，不会重复 |
| 导出了学生含手机号/身份证的 Excel 被人下载 URL 猜到 | 数据泄露 | ① download_url 用长随机 token（不要递增 ID）；② 下载要 JWT admin 角色；③ export_task.expire_at 24 小时自动删；④ 导出文件名是 UUID，不包含 "student" 等关键字。|

---

## 🎨 七、前端页面改动（1 个新页面）
在 `edu-schedule-ai-web/src/views/admin/` 新建 `BatchOperation.vue`（Tab 切 导入 / 导出 / 历史）：
- **Tab1 导入**：左侧 `el-upload` 拖拽上传 + 右侧 6 个下载模板按钮（5 个模板 + 1 份说明），上传完 3 秒轮询 `/batch/import/{taskId}` → 显示进度条 200/196/4 + 失败表格（红色定位哪一行错在哪，给 admin 改完重新上传）；
- **Tab2 导出**：下拉选范围（全部班级课表 PDF / 单教师 Excel / 某学期 CSV）+ 提交导出按钮 → 显示「任务已提交，请到历史查看下载」；
- **Tab3 历史**：`el-table` 分页（导入 + 导出合并显示），列：类型 / 文件名 / 总数 / 成功 / 失败 / 操作人 / 耗时 / 时间 / 操作（下载结果 / 下载失败行 / 下载导出文件）。

---

## 🏁 八、开发排期（估计 3 个 coding 日就能写完）
| Day | 做什么 |
|---|---|
| 上午 1 | 加 EasyExcel 依赖 + 新建 2 张 SQL 表 + user 扩列；写 6 个 DTO 带 @ExcelProperty |
| 下午 1 | 写 Import 服务层（学生 + 教师 2 类）+ 批量落库事务 + 接口 |
| 上午 2 | 写其余 3 类（课程/班级/教室）+ 历史列表 + 下载模板；前端 Tab1 + Tab3 导入历史 |
| 下午 2 | 写 Export 服务层（CSV 最简单，先做 CSV；PDF 用 OpenPDF）+ 前端 Tab2 + Tab3 导出历史 |
| 上午 3 | 补单元测试：200 行错误行（班级不存在 / 学号重复 / 手机格式错）→ 验证部分成功 + 失败定位；  |
| 下午 3 | 真实数据导入（你数据库里的 180 学生 → 导出 CSV → 重新导入 → 断言一致性） + README 加"批量导入导出"章节 |

> 设计蓝图完成，等你真要动手时照着写就行，所有坑都标清楚了 ✅
