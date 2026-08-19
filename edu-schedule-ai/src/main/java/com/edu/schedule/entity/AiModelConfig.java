package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import com.edu.schedule.common.BooleanToIntegerDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
public class AiModelConfig extends BaseEntity {

    private String modelCode;

    private String modelName;

    private String baseUrl;

    private String apiKey;

    private String model;

    private BigDecimal temperature;

    private Integer maxTokens;

    @JsonDeserialize(using = BooleanToIntegerDeserializer.class)
    private Integer isDefault;

    @JsonDeserialize(using = BooleanToIntegerDeserializer.class)
    private Integer enabled;
}