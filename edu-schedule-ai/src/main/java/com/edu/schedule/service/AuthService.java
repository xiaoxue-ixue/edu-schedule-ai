package com.edu.schedule.service;

import com.edu.schedule.entity.User;

import java.util.Map;

public interface AuthService {

    /**
     * 登录，返回 {token, user}
     */
    Map<String, Object> login(String username, String password);

    /**
     * 注册（仅学生）
     */
    void register(String username, String password, String realName);

    /**
     * 当前登录用户
     */
    User currentUser();

    /**
     * 修改密码
     */
    void changePassword(String oldPassword, String newPassword);
}
