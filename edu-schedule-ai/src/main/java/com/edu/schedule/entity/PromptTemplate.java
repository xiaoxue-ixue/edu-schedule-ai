package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Prompt 模板
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PromptTemplate extends BaseEntity {

    private String code;

    private String name;

    /** SCHEDULE / EVALUATION 等 */
    private String category;

    /** 模板内容，支持 {变量} 占位 */
    private String content;

    /** 变量列表，逗号分隔 */
    private String variables;

    private Integer enabled;
}
