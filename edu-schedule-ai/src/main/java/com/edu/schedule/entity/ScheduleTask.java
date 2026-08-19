package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 排课任务
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleTask extends BaseEntity {

    private String taskName;

    private Long termId;

    private Long modelConfigId;

    private String promptTemplateCode;

    /** 任务目标 */
    private String objective;

    /** 课程范围 */
    private String courseScope;

    /** 规则策略 */
    private String ruleStrategy;

    /** PENDING / RUNNING / SUCCESS / FAIL */
    private String status;

    /** 排课总结 */
    private String result;

    private Integer totalRounds;

    private Integer totalCourses;

    private Integer successCourses;

    /** 非数据库字段：关联查询 edu_term 表的学期名称，用于前端列表/下拉框显示 */
    private String termName;
}
