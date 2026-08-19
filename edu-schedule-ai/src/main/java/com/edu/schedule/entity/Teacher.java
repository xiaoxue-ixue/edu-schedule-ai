package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Teacher extends BaseEntity {
    private String teacherNo;
    private String name;
    private String title;
    private Long campusId;
    private String phone;

    private String campusName;
}
