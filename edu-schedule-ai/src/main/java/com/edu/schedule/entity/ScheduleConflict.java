package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

/**
 * 排课冲突
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleConflict extends BaseEntity {

    private Long detailId;

    private Long taskId;

    private Long termId;

    /** TEACHER_TIME / CLASS_TIME / ROOM_TIME / TEACHER_UNAVAILABLE / ROOM_UNAVAILABLE / ROOM_CAPACITY */
    private String conflictType;

    private String description;

    /** PENDING / HANDLED */
    private String status;

    private String handleMethod;

    private Date handleTime;
}
