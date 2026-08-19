package com.edu.schedule.entity;

import lombok.Data;

import java.util.Date;

/**
 * AI 工具调用日志
 */
@Data
public class AiToolCallLog {

    private Long id;

    private Long callLogId;

    private Long taskId;

    private String toolName;

    private String toolArgs;

    private String toolResult;

    private Integer success;

    private Integer durationMs;

    private Date callTime;
}
