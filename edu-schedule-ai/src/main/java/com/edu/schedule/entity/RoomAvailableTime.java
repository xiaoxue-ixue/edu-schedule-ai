package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RoomAvailableTime extends BaseEntity {
    private Long roomId;
    private Integer dayOfWeek;
    private Long slotId;
}
