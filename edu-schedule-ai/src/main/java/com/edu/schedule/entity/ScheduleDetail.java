package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排课明细（Agent 真正写入的数据）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleDetail extends BaseEntity {

    private Long taskId;

    private Long termId;

    private Long courseId;

    private Long teacherId;

    private Long classId;

    private Long roomId;

    /** 1-7 周一到周日 */
    private Integer dayOfWeek;

    private Long slotId;

    /** 周次，如 1-16 */
    private String weekPattern;

    /** AGENT / MANUAL */
    private String source;

    /** 0无冲突 1有冲突 */
    private Integer conflictFlag;

    private String conflictReason;

    private Integer status;

    /** 非数据库字段：JOIN 查询用 */
    private String courseName;
    private String teacherName;
    private String className;
    private String roomName;
    private String slotName;
    private String startTime;
    private String endTime;
}
