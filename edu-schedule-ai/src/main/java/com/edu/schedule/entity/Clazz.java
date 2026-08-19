package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class Clazz extends BaseEntity {
    private String classCode;
    private String className;
    private String grade;
    private Integer studentCount;
    private Long campusId;

    private String campusName;
}
