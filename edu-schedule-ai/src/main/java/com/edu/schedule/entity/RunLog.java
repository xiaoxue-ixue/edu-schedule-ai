package com.edu.schedule.entity;

import lombok.Data;
import java.util.Date;

@Data
public class RunLog {
    private Long id;
    private Date createTime;
    private String module;
    private String type;
    private String content;
    private Long operatorId;
}
