package com.edu.schedule.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Date;

@Data
public class TeachingEvaluation {

    private Long id;
    private Long courseId;
    private Long teacherId;
    private Long classId;
    private Long termId;
    private Long evaluatorId;
    private String evaluatorType;

    private BigDecimal attitudeScore;
    private BigDecimal contentScore;
    private BigDecimal methodScore;
    private BigDecimal effectScore;
    private BigDecimal overallScore;

    private String feedback;
    private String suggestion;
    private Date submitTime;

    private String courseName;
    private String teacherName;
    private String className;
    private String termName;

    // ============ 向后兼容：旧代码用 dim* 命名 ============
    public BigDecimal getDimAttitude() { return attitudeScore; }
    public void setDimAttitude(BigDecimal v) { this.attitudeScore = v; }
    public BigDecimal getDimContent() { return contentScore; }
    public void setDimContent(BigDecimal v) { this.contentScore = v; }
    public BigDecimal getDimMethod() { return methodScore; }
    public void setDimMethod(BigDecimal v) { this.methodScore = v; }
    public BigDecimal getDimEffect() { return effectScore; }
    public void setDimEffect(BigDecimal v) { this.effectScore = v; }
}