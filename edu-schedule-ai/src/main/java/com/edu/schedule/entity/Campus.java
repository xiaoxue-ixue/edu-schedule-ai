package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Campus extends BaseEntity {
    private String campusCode;
    private String campusName;
    private String address;
}
