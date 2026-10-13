-- ============================================================
-- 001_cleanup_orphans.sql —— 清理外键实施前的孤立数据
-- ⚠️ 建议执行前手工备份：mysqldump -uroot -p"$DB_PWD" edu_schedule_ai > backup.sql（口令取自环境变量 DB_PWD）
-- ============================================================
USE edu_schedule_ai;
SET FOREIGN_KEY_CHECKS = 0;  -- 清理期间临时关闭外键，防止循环报错

-- ai_call_log.task_id → schedule_task.id: 1 行找不到目标，执行 SET NULL
UPDATE `ai_call_log` SET `task_id` = NULL WHERE `task_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `schedule_task` p WHERE p.`id` = `ai_call_log`.`task_id`);

-- ai_tool_call_log.task_id → schedule_task.id: 61 行找不到目标，执行 SET NULL
UPDATE `ai_tool_call_log` SET `task_id` = NULL WHERE `task_id` IS NOT NULL AND NOT EXISTS (SELECT 1 FROM `schedule_task` p WHERE p.`id` = `ai_tool_call_log`.`task_id`);

SET FOREIGN_KEY_CHECKS = 1;  -- 恢复外键检查