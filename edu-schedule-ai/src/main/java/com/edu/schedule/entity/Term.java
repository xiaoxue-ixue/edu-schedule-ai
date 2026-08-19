package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
public class Term extends BaseEntity {
    private String termCode;
    private String termName;
    private Date startDate;
    private Date endDate;
    private Integer status;
}
