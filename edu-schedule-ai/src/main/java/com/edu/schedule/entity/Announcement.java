package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class Announcement extends BaseEntity {
    private String title;
    private String content;
    private Integer status;
    private Date publishTime;

    // ============ 新增字段 ============
    /** SYSTEM/TEACHING/SCHEDULE/MAINTENANCE */
    private String type;
    /** HIGH/MEDIUM/LOW */
    private String priority;
    /** 发布人 */
    private String createBy;
}