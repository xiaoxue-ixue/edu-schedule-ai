package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Room extends BaseEntity {
    private String roomCode;
    private String roomName;
    private Long campusId;
    private Integer capacity;
    private String roomType;

    private String campusName;
}
