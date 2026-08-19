package com.edu.schedule.entity;

import lombok.Data;

import java.util.Date;

/**
 * AI 调用日志
 */
@Data
public class AiCallLog {

    private Long id;

    private Long taskId;

    private String modelCode;

    private String model;

    private String prompt;

    private String response;

    private Integer inputTokens;

    private Integer outputTokens;

    private Integer totalTokens;

    private Integer durationMs;

    /** SUCCESS / FAIL */
    private String status;

    private String errorMsg;

    private Date callTime;
}
