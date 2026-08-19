package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class FunctionTool extends BaseEntity {
    private String toolCode;
    private String toolName;
    private String description;
    private String category;
    private Integer enabled;
}
