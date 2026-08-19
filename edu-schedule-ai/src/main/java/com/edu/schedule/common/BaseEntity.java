package com.edu.schedule.common;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 实体基类
 */
@Data
public class BaseEntity implements Serializable {

    private Long id;

    private Date createTime;

    private Date updateTime;
}
