package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class Course extends BaseEntity {
    private String courseCode;
    private String courseName;
    private BigDecimal credit;
    private Integer hours;
    private Long teacherId;

    private String teacherName;
}
