package com.edu.schedule.common;

/**
 * 教务系统常量
 */
public class ScheduleConstants {

    private ScheduleConstants() {
    }

    // 角色
    public static final String ROLE_ADMIN = "admin";
    public static final String ROLE_TEACHER = "teacher";
    public static final String ROLE_STUDENT = "student";

    // 排课任务状态
    public static final String TASK_PENDING = "PENDING";
    public static final String TASK_RUNNING = "RUNNING";
    public static final String TASK_SUCCESS = "SUCCESS";
    public static final String TASK_FAIL = "FAIL";

    // 冲突类型
    public static final String CONFLICT_TEACHER_TIME = "TEACHER_TIME";
    public static final String CONFLICT_CLASS_TIME = "CLASS_TIME";
    public static final String CONFLICT_ROOM_TIME = "ROOM_TIME";
    public static final String CONFLICT_TEACHER_UNAVAILABLE = "TEACHER_UNAVAILABLE";
    public static final String CONFLICT_ROOM_UNAVAILABLE = "ROOM_UNAVAILABLE";
    public static final String CONFLICT_ROOM_CAPACITY = "ROOM_CAPACITY";

    public static final String[] CONFLICT_TYPES = {
            "教师时间冲突", "班级时间冲突", "教室占用冲突",
            "教师不可用", "教室不可用", "教室容量不足"
    };

    // 评价维度
    public static final String[] EVAL_DIMENSIONS = {"教学态度", "教学内容", "教学方法", "教学效果"};

    // 课表版本状态
    public static final String VERSION_DRAFT = "DRAFT";
    public static final String VERSION_PUBLISHED = "PUBLISHED";
    public static final String VERSION_DISABLED = "DISABLED";
}
