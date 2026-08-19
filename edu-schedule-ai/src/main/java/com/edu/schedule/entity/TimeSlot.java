package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TimeSlot extends BaseEntity {
    private Integer slotNo;
    private String slotName;
    private String startTime;
    private String endTime;
}
