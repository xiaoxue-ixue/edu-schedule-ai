package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScheduleRule extends BaseEntity {
    private String ruleName;
    private String ruleType;
    private Integer priority;
    private String triggerCondition;
    private String action;
    private Integer enabled;
}
