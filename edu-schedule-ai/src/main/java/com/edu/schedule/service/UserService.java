package com.edu.schedule.service;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.entity.User;

public interface UserService {

    PageResult<User> page(Integer page, Integer size, String keyword, String role);

    /** 按班级批量创建学生账号 */
    int batchCreateStudents(Long classId, String prefix, int count);

    User getById(Long id);

    User getByUsername(String username);

    void create(User user);

    void update(User user);

    void delete(Long id);

    void changePassword(Long userId, String oldPassword, String newPassword);
}
