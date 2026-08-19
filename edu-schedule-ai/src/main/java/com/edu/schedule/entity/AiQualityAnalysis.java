package com.edu.schedule.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * AI 教学质量分析报告
 */
@Data
public class AiQualityAnalysis {

    private Long id;

    private Long termId;

    /** COURSE / TEACHER / CLASS */
    private String dimension;

    private Long targetId;

    private String targetName;

    private Integer evalCount;

    private BigDecimal avgScore;

    /** 优秀 / 良好 / 合格 / 不合格 */
    private String qualityLevel;

    private String summary;

    private String strengths;

    private String problems;

    private String suggestions;

    private String report;

    private Date createTime;
}
