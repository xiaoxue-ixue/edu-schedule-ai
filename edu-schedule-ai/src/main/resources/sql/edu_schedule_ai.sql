-- ============================================================
-- AI Agent 智能教务排课与教学质量分析系统 数据库脚本
-- 数据库: MySQL 5.7 / 8.0  字符集: utf8mb4
-- 说明: 执行本脚本后会自动建库建表并写入初始化数据
--       sys_user 账号由后端 DataInitializer 在首次启动时自动创建
--       (admin/admin  teacher01/123456  student01/123456)
--       AI 模型配置里的 API Key 需替换为真实 Key 才能使用 AI 功能
-- ============================================================

DROP DATABASE IF EXISTS edu_schedule_ai;
CREATE DATABASE edu_schedule_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE edu_schedule_ai;

-- ---------------------------- 系统用户 ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  username    VARCHAR(64)  NOT NULL UNIQUE COMMENT '用户名/工号/学号',
  password    VARCHAR(128) NOT NULL COMMENT 'BCrypt 密码',
  real_name   VARCHAR(64)  NOT NULL COMMENT '姓名',
  role        VARCHAR(16)  NOT NULL COMMENT 'admin/teacher/student',
  phone       VARCHAR(32),
  email       VARCHAR(128),
  teacher_id  BIGINT COMMENT '教师角色关联 edu_teacher.id',
  class_id    BIGINT COMMENT '学生角色关联 edu_clazz.id',
  status      TINYINT DEFAULT 1 COMMENT '0禁用 1启用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '系统用户';

-- ---------------------------- 校区 ----------------------------
DROP TABLE IF EXISTS edu_campus;
CREATE TABLE edu_campus (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  campus_code VARCHAR(32) NOT NULL UNIQUE,
  campus_name VARCHAR(64) NOT NULL,
  address     VARCHAR(255),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '校区';

-- ---------------------------- 学期 ----------------------------
DROP TABLE IF EXISTS edu_term;
CREATE TABLE edu_term (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  term_code   VARCHAR(32) NOT NULL UNIQUE,
  term_name   VARCHAR(64) NOT NULL,
  start_date  DATE,
  end_date    DATE,
  status      TINYINT DEFAULT 1 COMMENT '0停用 1启用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '学期';

-- ---------------------------- 教师 ----------------------------
DROP TABLE IF EXISTS edu_teacher;
CREATE TABLE edu_teacher (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_no  VARCHAR(32) NOT NULL UNIQUE COMMENT '工号',
  name        VARCHAR(64) NOT NULL,
  title       VARCHAR(32) COMMENT '职称',
  campus_id   BIGINT,
  phone       VARCHAR(32),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '教师';

-- ---------------------------- 班级 ----------------------------
DROP TABLE IF EXISTS edu_clazz;
CREATE TABLE edu_clazz (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  class_code  VARCHAR(32) NOT NULL UNIQUE,
  class_name  VARCHAR(64) NOT NULL,
  grade       VARCHAR(16) COMMENT '年级',
  student_count INT COMMENT '人数',
  campus_id   BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '班级';

-- ---------------------------- 课程 ----------------------------
DROP TABLE IF EXISTS edu_course;
CREATE TABLE edu_course (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_code VARCHAR(32) NOT NULL UNIQUE,
  course_name VARCHAR(128) NOT NULL,
  credit      DECIMAL(3,1),
  hours       INT COMMENT '总学时',
  teacher_id  BIGINT COMMENT '主讲教师',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '课程';

-- ---------------------------- 教室 ----------------------------
DROP TABLE IF EXISTS edu_room;
CREATE TABLE edu_room (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_code   VARCHAR(32) NOT NULL UNIQUE,
  room_name   VARCHAR(64) NOT NULL,
  campus_id   BIGINT,
  capacity    INT COMMENT '容量',
  room_type   VARCHAR(32) COMMENT '普通/机房/实验室',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '教室';

-- ---------------------------- 时间段(节次) ----------------------------
DROP TABLE IF EXISTS edu_time_slot;
CREATE TABLE edu_time_slot (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  slot_no     INT NOT NULL UNIQUE COMMENT '节次序号 1-N',
  slot_name   VARCHAR(32) NOT NULL COMMENT '第1-2节',
  start_time  VARCHAR(16),
  end_time    VARCHAR(16),
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '时间段(节次)';

-- ---------------------------- 教师可用时间 ----------------------------
DROP TABLE IF EXISTS edu_teacher_available_time;
CREATE TABLE edu_teacher_available_time (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_id   BIGINT NOT NULL,
  day_of_week  INT NOT NULL COMMENT '1-7 周一到周日',
  slot_id      BIGINT NOT NULL,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_teacher_day_slot (teacher_id, day_of_week, slot_id)
) COMMENT '教师可用时间';

-- ---------------------------- 教室可用时间 ----------------------------
DROP TABLE IF EXISTS edu_room_available_time;
CREATE TABLE edu_room_available_time (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id      BIGINT NOT NULL,
  day_of_week  INT NOT NULL,
  slot_id      BIGINT NOT NULL,
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_day_slot (room_id, day_of_week, slot_id)
) COMMENT '教室可用时间';

-- ---------------------------- 排课规则 ----------------------------
DROP TABLE IF EXISTS schedule_rule;
CREATE TABLE schedule_rule (
  id                BIGINT AUTO_INCREMENT PRIMARY KEY,
  rule_name         VARCHAR(128) NOT NULL,
  rule_type         VARCHAR(16) NOT NULL COMMENT 'HARD 硬规则 / SOFT 软规则',
  priority          INT DEFAULT 50 COMMENT '优先级 越大越优先',
  trigger_condition VARCHAR(512) COMMENT '触发条件',
  action            VARCHAR(512) COMMENT '处理动作',
  enabled           TINYINT DEFAULT 1,
  create_time       DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time       DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '排课规则';

-- ---------------------------- AI 模型配置 ----------------------------
DROP TABLE IF EXISTS ai_model_config;
CREATE TABLE ai_model_config (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  model_code    VARCHAR(64) NOT NULL UNIQUE COMMENT '编码 唯一',
  model_name    VARCHAR(128) NOT NULL COMMENT '展示名',
  base_url      VARCHAR(255) NOT NULL COMMENT 'OpenAI 兼容地址',
  api_key       VARCHAR(255) COMMENT '密钥(需替换为真实Key)',
  model         VARCHAR(128) COMMENT '模型名 如 gpt-4o-mini',
  temperature   DECIMAL(3,2) DEFAULT 0.70,
  max_tokens    INT DEFAULT 4096,
  is_default    TINYINT DEFAULT 0 COMMENT '是否默认 1是',
  enabled       TINYINT DEFAULT 1,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'AI 模型配置(数据库化)';

-- ---------------------------- Prompt 模板 ----------------------------
DROP TABLE IF EXISTS ai_prompt_template;
CREATE TABLE ai_prompt_template (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  code        VARCHAR(64) NOT NULL UNIQUE COMMENT '模板编码',
  name        VARCHAR(128) NOT NULL,
  category    VARCHAR(32) COMMENT 'SCHEDULE/EVALUATION 等',
  content     TEXT COMMENT '模板内容 支持变量占位',
  variables   VARCHAR(512) COMMENT '变量列表 逗号分隔',
  enabled     TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'Prompt 模板';

-- ---------------------------- AI 调用日志 ----------------------------
DROP TABLE IF EXISTS ai_call_log;
CREATE TABLE ai_call_log (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id      BIGINT COMMENT '关联排课任务',
  model_code   VARCHAR(64),
  model        VARCHAR(128),
  prompt       TEXT COMMENT '请求内容摘要',
  response     TEXT COMMENT '模型回复摘要',
  input_tokens INT,
  output_tokens INT,
  total_tokens INT,
  duration_ms  INT COMMENT '耗时毫秒',
  status       VARCHAR(16) COMMENT 'SUCCESS/FAIL',
  error_msg    TEXT COMMENT '错误信息（TEXT 类型，避免超长截断）',
  call_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task (task_id)
) COMMENT 'AI 调用日志';

-- ---------------------------- AI 工具调用日志 ----------------------------
DROP TABLE IF EXISTS ai_tool_call_log;
CREATE TABLE ai_tool_call_log (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  call_log_id  BIGINT COMMENT '关联 AI 调用',
  task_id      BIGINT,
  tool_name    VARCHAR(128) NOT NULL COMMENT '工具名',
  tool_args    TEXT COMMENT '入参 JSON',
  tool_result  TEXT COMMENT '返回 JSON',
  success      TINYINT,
  duration_ms  INT,
  call_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_call (call_log_id),
  INDEX idx_task (task_id)
) COMMENT 'AI 工具调用日志';

-- ---------------------------- Function 工具启用状态 ----------------------------
DROP TABLE IF EXISTS function_tool;
CREATE TABLE function_tool (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  tool_code   VARCHAR(64) NOT NULL UNIQUE COMMENT '与后端 @Tool 一一对应',
  tool_name   VARCHAR(128) NOT NULL,
  description VARCHAR(512),
  category    VARCHAR(32) COMMENT 'QUERY/CHECK/WRITE/KNOWLEDGE',
  enabled     TINYINT DEFAULT 1,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT 'Function 工具启用状态';

-- ---------------------------- 教务知识库 ----------------------------
DROP TABLE IF EXISTS education_knowledge;
CREATE TABLE education_knowledge (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  title         VARCHAR(255) NOT NULL,
  category      VARCHAR(64) COMMENT '制度/规则/流程/口径',
  content       TEXT COMMENT '原始内容',
  slice_content TEXT COMMENT '自动切片后的内容(分号分隔)',
  slice_count   INT DEFAULT 0,
  status        TINYINT DEFAULT 1,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '教务知识库';

-- ---------------------------- 排课任务 ----------------------------
DROP TABLE IF EXISTS schedule_task;
CREATE TABLE schedule_task (
  id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_name           VARCHAR(128) NOT NULL,
  term_id             BIGINT NOT NULL,
  model_config_id     BIGINT,
  prompt_template_code VARCHAR(64),
  objective           VARCHAR(512) COMMENT '任务目标',
  course_scope        VARCHAR(512) COMMENT '课程范围',
  rule_strategy       VARCHAR(512) COMMENT '规则策略',
  status              VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/RUNNING/SUCCESS/FAIL',
  result              TEXT COMMENT '排课总结',
  total_rounds        INT DEFAULT 0 COMMENT '工具往返轮数',
  total_courses       INT DEFAULT 0,
  success_courses     INT DEFAULT 0,
  create_time         DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time         DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '排课任务';

-- ---------------------------- 排课明细 ----------------------------
DROP TABLE IF EXISTS schedule_detail;
CREATE TABLE schedule_detail (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  task_id        BIGINT NOT NULL,
  term_id        BIGINT NOT NULL,
  course_id      BIGINT NOT NULL,
  teacher_id     BIGINT NOT NULL,
  class_id       BIGINT NOT NULL,
  room_id        BIGINT NOT NULL,
  day_of_week    INT NOT NULL COMMENT '1-7',
  slot_id        BIGINT NOT NULL,
  week_pattern   VARCHAR(64) DEFAULT '1-16' COMMENT '周次',
  source         VARCHAR(16) DEFAULT 'AGENT' COMMENT 'AGENT/MANUAL',
  conflict_flag  TINYINT DEFAULT 0 COMMENT '0无冲突 1有冲突',
  conflict_reason VARCHAR(512),
  status         TINYINT DEFAULT 1,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_task (task_id),
  INDEX idx_term (term_id),
  INDEX idx_teacher_time (teacher_id, day_of_week, slot_id),
  INDEX idx_class_time (class_id, day_of_week, slot_id),
  INDEX idx_room_time (room_id, day_of_week, slot_id)
) COMMENT '排课明细';

-- ---------------------------- 排课冲突 ----------------------------
DROP TABLE IF EXISTS schedule_conflict;
CREATE TABLE schedule_conflict (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  detail_id      BIGINT COMMENT '关联排课明细',
  task_id        BIGINT,
  term_id        BIGINT,
  conflict_type  VARCHAR(32) NOT NULL COMMENT 'TEACHER_TIME/CLASS_TIME/ROOM_TIME/TEACHER_UNAVAILABLE/ROOM_UNAVAILABLE/ROOM_CAPACITY',
  description    VARCHAR(512),
  status         VARCHAR(16) DEFAULT 'PENDING' COMMENT 'PENDING/HANDLED',
  handle_method  VARCHAR(512) COMMENT '处理方式',
  handle_time    DATETIME,
  create_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_task (task_id)
) COMMENT '排课冲突';

-- ---------------------------- 课表版本 ----------------------------
DROP TABLE IF EXISTS schedule_version;
CREATE TABLE schedule_version (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_name VARCHAR(128) NOT NULL,
  term_id      BIGINT NOT NULL,
  task_id      BIGINT,
  status       VARCHAR(16) DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/DISABLED',
  publish_time DATETIME,
  remark       VARCHAR(512),
  create_time  DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '课表版本';

-- ---------------------------- 课表版本快照明细 ----------------------------
DROP TABLE IF EXISTS schedule_version_detail;
CREATE TABLE schedule_version_detail (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  version_id    BIGINT NOT NULL,
  course_id     BIGINT,
  teacher_id    BIGINT,
  class_id      BIGINT,
  room_id       BIGINT,
  day_of_week   INT,
  slot_id       BIGINT,
  week_pattern  VARCHAR(64),
  INDEX idx_version (version_id)
) COMMENT '课表版本快照明细';

-- ---------------------------- 公告 ----------------------------
DROP TABLE IF EXISTS announcement;
CREATE TABLE announcement (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  title         VARCHAR(255) NOT NULL,
  content       TEXT,
  status        TINYINT DEFAULT 1 COMMENT '0下架 1发布',
  publish_time  DATETIME,
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '教务公告';

-- ---------------------------- 教学评价 ----------------------------
DROP TABLE IF EXISTS teaching_evaluation;
CREATE TABLE teaching_evaluation (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY,
  course_id      BIGINT NOT NULL,
  teacher_id     BIGINT NOT NULL,
  class_id       BIGINT NOT NULL,
  term_id        BIGINT NOT NULL,
  evaluator_id   BIGINT COMMENT '评价人ID',
  evaluator_type VARCHAR(32) COMMENT 'STUDENT/CLASS/SUPERVISOR',
  dim_attitude   DECIMAL(3,1) COMMENT '教学态度',
  dim_content    DECIMAL(3,1) COMMENT '教学内容',
  dim_method     DECIMAL(3,1) COMMENT '教学方法',
  dim_effect     DECIMAL(3,1) COMMENT '教学效果',
  overall_score  DECIMAL(3,1) COMMENT '综合评分(后端计算)',
  feedback       TEXT COMMENT '文字反馈',
  suggestion     TEXT COMMENT '改进建议',
  submit_time    DATETIME DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_course (course_id),
  INDEX idx_teacher (teacher_id)
) COMMENT '教学评价';

-- ---------------------------- AI 教学质量分析 ----------------------------
DROP TABLE IF EXISTS ai_quality_analysis;
CREATE TABLE ai_quality_analysis (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY,
  term_id       BIGINT,
  dimension     VARCHAR(32) COMMENT 'COURSE/TEACHER/CLASS',
  target_id     BIGINT COMMENT '课程/教师/班级ID',
  target_name   VARCHAR(128),
  eval_count    INT,
  avg_score     DECIMAL(3,1),
  quality_level VARCHAR(16) COMMENT '优秀/良好/合格/不合格',
  summary       TEXT COMMENT 'AI摘要',
  strengths     TEXT COMMENT '优势',
  problems      TEXT COMMENT '问题',
  suggestions   TEXT COMMENT '改进建议',
  report        TEXT COMMENT '完整AI报告',
  create_time   DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT 'AI 教学质量分析';

-- ---------------------------- 运行日志 ----------------------------
DROP TABLE IF EXISTS run_log;
CREATE TABLE run_log (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  module      VARCHAR(32) COMMENT 'TASK/AI/CONFLICT/VERSION/QUALITY',
  type        VARCHAR(32) COMMENT '操作类型',
  content     VARCHAR(512),
  operator_id BIGINT,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) COMMENT '运行日志';


-- ============================================================
-- 初始化数据
-- ============================================================

-- 校区
INSERT INTO edu_campus(campus_code, campus_name, address) VALUES
('C01','主校区','大学路1号'),
('C02','南校区','南环路66号');

-- 学期
INSERT INTO edu_term(term_code, term_name, start_date, end_date, status) VALUES
('2025-2026-1','2025-2026学年第一学期','2025-09-01','2026-01-15',1),
('2025-2026-2','2025-2026学年第二学期','2026-02-24','2026-06-30',1);

-- 时间段(节次)
INSERT INTO edu_time_slot(slot_no, slot_name, start_time, end_time) VALUES
(1,'第1-2节','08:00','09:35'),
(2,'第3-4节','10:00','11:35'),
(3,'第5-6节','14:00','15:35'),
(4,'第7-8节','16:00','17:35'),
(5,'第9-10节','19:00','20:35');

-- 教师
INSERT INTO edu_teacher(teacher_no, name, title, campus_id, phone) VALUES
('T001','张明','教授',1,'13800000001'),
('T002','李华','副教授',1,'13800000002'),
('T003','王芳','讲师',2,'13800000003'),
('T004','赵强','副教授',2,'13800000004');

-- 班级
INSERT INTO edu_clazz(class_code, class_name, grade, student_count, campus_id) VALUES
('CL202301','计算机2301班','2023级',40,1),
('CL202302','计算机2302班','2023级',38,1),
('CL202303','软件2301班','2023级',42,2),
('CL202304','网络2301班','2023级',35,2);

-- 课程
INSERT INTO edu_course(course_code, course_name, credit, hours, teacher_id) VALUES
('CS101','高等数学',5,80,1),
('CS102','数据结构',4,64,2),
('CS103','操作系统',4,64,2),
('CS104','计算机网络',3,48,3),
('CS105','数据库原理',4,64,4),
('CS106','人工智能导论',3,48,1);

-- 教室
INSERT INTO edu_room(room_code, room_name, campus_id, capacity, room_type) VALUES
('R101','A楼101',1,60,'普通'),
('R102','A楼102',1,80,'普通'),
('R201','A楼201机房',1,50,'机房'),
('R301','B楼301',2,120,'普通'),
('R302','B楼302实验室',2,40,'实验室');

-- 教师可用时间(示例: 教师1 周一至周五 全天可用)
INSERT INTO edu_teacher_available_time(teacher_id, day_of_week, slot_id) VALUES
(1,1,1),(1,1,2),(1,1,3),(1,1,4),(1,2,1),(1,2,2),(1,2,3),(1,2,4),
(1,3,1),(1,3,2),(1,3,3),(1,3,4),(1,4,1),(1,4,2),(1,4,3),(1,4,4),(1,5,1),(1,5,2),
(2,1,1),(2,1,2),(2,2,1),(2,2,2),(2,3,1),(2,3,2),(2,4,1),(2,4,2),(2,5,1),(2,5,2),
(3,2,3),(3,2,4),(3,3,3),(3,3,4),(3,4,3),(3,4,4),
(4,1,3),(4,1,4),(4,3,1),(4,3,2),(4,5,3),(4,5,4);

-- 教室可用时间(示例: 教室1 全周可用)
INSERT INTO edu_room_available_time(room_id, day_of_week, slot_id) VALUES
(1,1,1),(1,1,2),(1,1,3),(1,1,4),(1,2,1),(1,2,2),(1,2,3),(1,2,4),
(1,3,1),(1,3,2),(1,3,3),(1,3,4),(1,4,1),(1,4,2),(1,4,3),(1,4,4),(1,5,1),(1,5,2),(1,5,3),(1,5,4),
(2,1,1),(2,1,2),(2,2,1),(2,2,2),(2,3,1),(2,3,2),(2,3,3),(2,3,4),
(3,2,3),(3,2,4),(3,3,3),(3,3,4),(3,4,3),(3,4,4),
(4,1,1),(4,1,2),(4,1,3),(4,1,4),(4,3,1),(4,3,2),(4,5,3),(4,5,4);

-- 排课规则
INSERT INTO schedule_rule(rule_name, rule_type, priority, trigger_condition, action, enabled) VALUES
('教师时间冲突硬约束','HARD',100,'同一教师在同一时间点已排课','禁止排课,换时间',1),
('班级时间冲突硬约束','HARD',100,'同一班级在同一时间点已排课','禁止排课,换时间',1),
('教室占用硬约束','HARD',100,'同一教室在同一时间点已被占用','禁止排课,换教室',1),
('教师可用时间硬约束','HARD',95,'教师在该时间点不可用','禁止排课,换时间',1),
('教室可用时间硬约束','HARD',95,'教室在该时间点不可用','禁止排课,换教室',1),
('教室容量硬约束','HARD',90,'教室容量小于班级人数','禁止排课,换教室',1),
('一天不连排超4节','SOFT',60,'同一班级单天排课超4节','尽量分散',1),
('课程分散排课','SOFT',50,'同一课程尽量不连续两天','尽量隔天',1);

-- AI 模型配置(默认占位,需替换 api_key 为真实 Key)
INSERT INTO ai_model_config(model_code, model_name, base_url, api_key, model, temperature, max_tokens, is_default, enabled) VALUES
('default','默认模型(OpenAI兼容)','https://api.openai.com/v1','','gpt-4o-mini',0.70,4096,1,1);

-- Prompt 模板(排课 Agent)
INSERT INTO ai_prompt_template(code, name, category, content, variables, enabled) VALUES
('SCHEDULE_AGENT','排课Agent系统提示词','SCHEDULE',
'你是一个高校智能排课 Agent。任务目标: {objective}\n执行步骤:\n1. 调用查询工具拿到真实教师/教室/班级/课程/规则的ID\n2. 逐节做教师可用、教室可用、教室容量、时间占用四层检测\n3. 检测全通过才调用写入工具落课; 任一不通过就换时间或换教室重来\n4. 全部课程排完后给出本次排课总结\n约束: 只能通过工具读写数据, 不要编造ID, 不要跳过检测。',
'objective',1);

-- Function 工具(8个, 与后端 @Tool 一一对应)
INSERT INTO function_tool(tool_code, tool_name, description, category, enabled) VALUES
('queryTeachers','查询教师资源','查询可排课教师列表及其可用时间','QUERY',1),
('queryRooms','查询教室资源','查询教室列表、容量与可用时间','QUERY',1),
('queryClasses','查询班级资源','查询班级列表与人数','QUERY',1),
('queryCourses','查询课程资源','查询待排课程列表与主讲教师','QUERY',1),
('queryScheduleRules','查询排课规则','查询硬/软规则与优先级','QUERY',1),
('checkScheduleConflict','排课冲突检测','对指定 教师/班级/教室/星期/节次 做四层约束检测','CHECK',1),
('writeScheduleDetail','写入排课明细','将一节排课结果写入排课明细表(唯一可写工具)','WRITE',1),
('searchEducationKnowledge','教务知识库检索','按关键词检索教务制度/排课规则说明','KNOWLEDGE',1);

-- 教务知识库(示例,会自动切片)
INSERT INTO education_knowledge(title, category, content, slice_content, slice_count, status) VALUES
('排课管理制度','制度','第一章 总则: 排课应遵循教师时间不冲突、教室不冲突、班级不冲突三大硬约束。第二章 教师可用时间由教务处登记, Agent排课不得超出可用范围。第三章 教室容量不得小于班级人数。','第一章 总则: 排课应遵循教师时间不冲突、教室不冲突、班级不冲突三大硬约束;第二章 教师可用时间由教务处登记, Agent排课不得超出可用范围;第三章 教室容量不得小于班级人数',3,1),
('调课流程','流程','调课需由任课教师提交申请, 教务处审批后修改排课明细并重新检测冲突, 调课结果需通知相关班级与教师。','调课需由任课教师提交申请;教务处审批后修改排课明细并重新检测冲突;调课结果需通知相关班级与教师',3,1);

-- 公告
INSERT INTO announcement(title, content, status, publish_time) VALUES
('关于2025-2026学年第一学期排课工作的通知','请各位教师于开学前在系统内核对本人的可用时间,如有调整及时联系教务处。',1,NOW());

-- 教学评价(示例数据, 供 AI 质量分析使用)
INSERT INTO teaching_evaluation(course_id, teacher_id, class_id, term_id, evaluator_id, evaluator_type, dim_attitude, dim_content, dim_method, dim_effect, overall_score, feedback, suggestion, submit_time) VALUES
(2,2,1,1,NULL,'STUDENT',9.0,8.5,9.0,8.0,8.6,'讲解清晰','多增加实验课时',NOW()),
(2,2,1,1,NULL,'STUDENT',8.0,9.0,8.5,8.5,8.5,'内容充实','节奏稍快',NOW()),
(3,2,2,1,NULL,'STUDENT',9.5,9.0,9.0,9.0,9.1,'非常优秀','保持',NOW()),
(4,3,3,1,NULL,'STUDENT',8.0,8.0,7.5,8.0,7.9,'一般','增加案例',NOW()),
(5,4,4,1,NULL,'STUDENT',9.0,9.5,9.0,9.5,9.3,'很好','无',NOW());

-- 运行日志(示例)
INSERT INTO run_log(module, type, content, operator_id, create_time) VALUES
('TASK','INIT','系统初始化完成',1,NOW());
