package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 教务知识库（自动切片）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class Knowledge extends BaseEntity {

    private String title;

    /** 制度 / 规则 / 流程 / 口径 */
    private String category;

    /** 原始内容 */
    private String content;

    /** 自动切片后的内容，分号分隔 */
    private String sliceContent;

    private Integer sliceCount;

    private Integer status;
}
