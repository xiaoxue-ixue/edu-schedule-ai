package com.edu.schedule.entity;

import com.edu.schedule.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 系统用户（管理员/教师/学生）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class User extends BaseEntity {

    private String username;

    /** 密码（BCrypt） */
    private String password;

    private String realName;

    /** admin / teacher / student */
    private String role;

    private String phone;

    private String email;

    /** 教师角色关联 edu_teacher.id */
    private Long teacherId;

    /** 学生角色关联 edu_clazz.id */
    private Long classId;

    /** 0 禁用 1 启用 */
    private Integer status;

    /** 非数据库字段：关联查询显示用 */
    private String className;
    private String teacherName;
}
