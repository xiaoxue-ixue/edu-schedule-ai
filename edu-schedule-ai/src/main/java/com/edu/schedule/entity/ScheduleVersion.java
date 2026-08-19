package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleVersion extends BaseEntity {

    private String versionName;

    private Long termId;

    private Long taskId;

    /** DRAFT / PUBLISHED / OFFLINE */
    private String status;

    private Date publishTime;

    /** 版本描述/备注（复用 remark 字段） */
    private String remark;
}