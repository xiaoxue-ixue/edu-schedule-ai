package com.edu.schedule.config;

import com.edu.schedule.common.ScheduleConstants;
import com.edu.schedule.entity.User;
import com.edu.schedule.mapper.UserMapper;
import com.edu.schedule.utils.PasswordUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器：首次启动时若 sys_user 为空，自动创建默认账号
 *   admin / admin        系统管理员
 *   teacher01 / 123456   教师(关联 T001 张明)
 *   student01 / 123456   学生(关联 CL202301)
 */
@Slf4j
@Component
@Order(1)
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserMapper userMapper;

    @Override
    public void run(String... args) {
        if (userMapper.countAll() > 0) {
            return;
        }
        log.info("检测到 sys_user 为空，开始初始化默认账号...");
        insert("admin", "admin", "系统管理员", ScheduleConstants.ROLE_ADMIN, null, null);
        insert("teacher01", "123456", "张明", ScheduleConstants.ROLE_TEACHER, 1L, null);
        insert("student01", "123456", "学生甲", ScheduleConstants.ROLE_STUDENT, null, 1L);
        log.info("默认账号初始化完成: admin/admin  teacher01/123456  student01/123456");
    }

    private void insert(String username, String pwd, String realName, String role, Long teacherId, Long classId) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(PasswordUtils.encode(pwd));
        u.setRealName(realName);
        u.setRole(role);
        u.setTeacherId(teacherId);
        u.setClassId(classId);
        u.setStatus(1);
        userMapper.insert(u);
    }
}
