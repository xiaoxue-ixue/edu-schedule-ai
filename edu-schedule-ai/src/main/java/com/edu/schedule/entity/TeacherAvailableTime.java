package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TeacherAvailableTime extends BaseEntity {
    private Long teacherId;
    private Integer dayOfWeek;
    private Long slotId;
}
