-- ============================================================
-- 002_add_foreign_keys.sql —— 为所有关联字段添加外键约束（40 条）
-- ⚠️ 执行顺序：
--    ① 先执行 mysqldump 备份整库（强烈建议！）
--    ② 执行 001_cleanup_orphans.sql（清理孤立数据，防止 1452 错误）
--    ③ 执行本脚本
--
-- 外键删除策略说明（业务语义驱动，非"一刀切 CASCADE"）：
--   · ON DELETE CASCADE  —— "从属数据"，父记录没了子记录必然无意义
--       例：schedule_detail.task_id（任务删了，排课明细自然要删）
--   · ON DELETE SET NULL —— "引用型字段"，父记录删除后子记录仍有价值
--       例：teaching_evaluation.evaluator_id（用户删了，评价本身不应该消失）
--   · ON DELETE RESTRICT —— "主数据基础表"，禁止在有引用时删除
--       例：edu_term、edu_course、edu_teacher、edu_clazz、edu_room、edu_time_slot
-- ============================================================

USE edu_schedule_ai;

-- 建议关闭自动提交 + 唯一外键索引名前缀 fk_
SET FOREIGN_KEY_CHECKS = 1;
SET NAMES utf8mb4;

-- =====================================
-- 1. 系统用户 → 教师/班级
-- =====================================
-- 用户关联的教师/班级被删，用户账号仍要保留 → SET NULL
ALTER TABLE sys_user ADD CONSTRAINT fk_user_teacher FOREIGN KEY (teacher_id) REFERENCES edu_teacher(id) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE sys_user ADD CONSTRAINT fk_user_class   FOREIGN KEY (class_id)   REFERENCES edu_clazz(id)   ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 2. 教师/班级/课程/教室 → 校区
-- =====================================
-- 校区删除会导致大量记录受影响，先清引用 → SET NULL
ALTER TABLE edu_teacher ADD CONSTRAINT fk_teacher_campus FOREIGN KEY (campus_id) REFERENCES edu_campus(id) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE edu_clazz   ADD CONSTRAINT fk_clazz_campus   FOREIGN KEY (campus_id) REFERENCES edu_campus(id) ON DELETE SET NULL ON UPDATE CASCADE;
ALTER TABLE edu_room    ADD CONSTRAINT fk_room_campus    FOREIGN KEY (campus_id) REFERENCES edu_campus(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 3. 课程 → 教师（主讲老师被删仍保留课程 → SET NULL）
-- =====================================
ALTER TABLE edu_course ADD CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES edu_teacher(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 4. 教师/教室可用时间
-- =====================================
-- 教师/教室/节次被删 → 可用时间自然失效；节次被删先清引用
ALTER TABLE edu_teacher_available_time
  ADD CONSTRAINT fk_tat_teacher   FOREIGN KEY (teacher_id) REFERENCES edu_teacher(id)   ON DELETE CASCADE     ON UPDATE CASCADE,
  ADD CONSTRAINT fk_tat_slot      FOREIGN KEY (slot_id)    REFERENCES edu_time_slot(id) ON DELETE RESTRICT    ON UPDATE CASCADE;
ALTER TABLE edu_room_available_time
  ADD CONSTRAINT fk_rat_room      FOREIGN KEY (room_id)   REFERENCES edu_room(id)      ON DELETE CASCADE     ON UPDATE CASCADE,
  ADD CONSTRAINT fk_rat_slot      FOREIGN KEY (slot_id)   REFERENCES edu_time_slot(id) ON DELETE RESTRICT    ON UPDATE CASCADE;

-- =====================================
-- 5. 排课任务 → 学期 / AI 模型配置
-- =====================================
-- 学期是重要历史归档 → RESTRICT（禁止删学期，如果硬删要先清所有任务）
-- AI 模型配置删除 → 任务仍保留，只置空 model_config_id
ALTER TABLE schedule_task
  ADD CONSTRAINT fk_task_term          FOREIGN KEY (term_id)         REFERENCES edu_term(id)         ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_task_model_config  FOREIGN KEY (model_config_id) REFERENCES ai_model_config(id)   ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 6. 排课明细（任务的从属数据 + 引用主数据）
-- =====================================
-- 排课任务删除 → 明细 CASCADE 跟随删除
-- 学期/课程/教师/班级/教室/节次 → 禁止在有排课时被删（RESTRICT，保护排课历史）
ALTER TABLE schedule_detail
  ADD CONSTRAINT fk_detail_task    FOREIGN KEY (task_id)    REFERENCES schedule_task(id) ON DELETE CASCADE  ON UPDATE CASCADE,
  ADD CONSTRAINT fk_detail_term    FOREIGN KEY (term_id)    REFERENCES edu_term(id)      ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_detail_course  FOREIGN KEY (course_id)  REFERENCES edu_course(id)    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_detail_teacher FOREIGN KEY (teacher_id) REFERENCES edu_teacher(id)   ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_detail_class   FOREIGN KEY (class_id)   REFERENCES edu_clazz(id)     ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_detail_room    FOREIGN KEY (room_id)    REFERENCES edu_room(id)      ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_detail_slot    FOREIGN KEY (slot_id)    REFERENCES edu_time_slot(id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- =====================================
-- 7. 排课冲突（跟随明细/任务）
-- =====================================
ALTER TABLE schedule_conflict
  ADD CONSTRAINT fk_conflict_detail FOREIGN KEY (detail_id) REFERENCES schedule_detail(id) ON DELETE CASCADE  ON UPDATE CASCADE,
  ADD CONSTRAINT fk_conflict_task   FOREIGN KEY (task_id)   REFERENCES schedule_task(id)   ON DELETE CASCADE  ON UPDATE CASCADE,
  ADD CONSTRAINT fk_conflict_term   FOREIGN KEY (term_id)   REFERENCES edu_term(id)        ON DELETE RESTRICT ON UPDATE CASCADE;

-- =====================================
-- 8. 课表版本（快照对象，不跟随任务强删）
-- =====================================
ALTER TABLE schedule_version
  ADD CONSTRAINT fk_version_term FOREIGN KEY (term_id) REFERENCES edu_term(id)      ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_version_task FOREIGN KEY (task_id) REFERENCES schedule_task(id) ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 9. 课表版本明细快照（跟随版本 CASCADE，引用主数据 SET NULL）
--    语义：版本被删 → 快照全删；课程被删 → 快照仍保留其他排课信息
-- =====================================
ALTER TABLE schedule_version_detail
  ADD CONSTRAINT fk_sv_detail_version FOREIGN KEY (version_id) REFERENCES schedule_version(id) ON DELETE CASCADE  ON UPDATE CASCADE,
  ADD CONSTRAINT fk_sv_detail_course  FOREIGN KEY (course_id)  REFERENCES edu_course(id)      ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT fk_sv_detail_teacher FOREIGN KEY (teacher_id) REFERENCES edu_teacher(id)     ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT fk_sv_detail_class   FOREIGN KEY (class_id)   REFERENCES edu_clazz(id)       ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT fk_sv_detail_room    FOREIGN KEY (room_id)    REFERENCES edu_room(id)        ON DELETE SET NULL ON UPDATE CASCADE,
  ADD CONSTRAINT fk_sv_detail_slot    FOREIGN KEY (slot_id)    REFERENCES edu_time_slot(id)   ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 10. 教学评价（引用主数据禁止删，评价人被删 SET NULL）
-- =====================================
ALTER TABLE teaching_evaluation
  ADD CONSTRAINT fk_eval_course    FOREIGN KEY (course_id)    REFERENCES edu_course(id)   ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_eval_teacher   FOREIGN KEY (teacher_id)   REFERENCES edu_teacher(id)  ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_eval_class     FOREIGN KEY (class_id)     REFERENCES edu_clazz(id)    ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_eval_term      FOREIGN KEY (term_id)      REFERENCES edu_term(id)     ON DELETE RESTRICT ON UPDATE CASCADE,
  ADD CONSTRAINT fk_eval_evaluator FOREIGN KEY (evaluator_id) REFERENCES sys_user(id)     ON DELETE SET NULL ON UPDATE CASCADE;

-- =====================================
-- 11. AI 教学质量分析
-- =====================================
ALTER TABLE ai_quality_analysis ADD CONSTRAINT fk_qa_term FOREIGN KEY (term_id) REFERENCES edu_term(id) ON DELETE RESTRICT ON UPDATE CASCADE;

-- =====================================
-- 12. AI 调用日志 / 工具调用日志（从属排课任务 + 层级 CASCADE）
-- =====================================
ALTER TABLE ai_call_log      ADD CONSTRAINT fk_call_log_task   FOREIGN KEY (task_id)   REFERENCES schedule_task(id) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE ai_tool_call_log
  ADD CONSTRAINT fk_tool_call_log_call   FOREIGN KEY (call_log_id) REFERENCES ai_call_log(id)    ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT fk_tool_call_log_task   FOREIGN KEY (task_id)     REFERENCES schedule_task(id)  ON DELETE CASCADE ON UPDATE CASCADE;

-- =====================================
-- 13. 运行日志（操作人被删不丢日志）
-- =====================================
ALTER TABLE run_log ADD CONSTRAINT fk_run_log_operator FOREIGN KEY (operator_id) REFERENCES sys_user(id) ON DELETE SET NULL ON UPDATE CASCADE;