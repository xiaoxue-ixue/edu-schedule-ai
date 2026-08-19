package com.edu.schedule.service.impl;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.ScheduleConstants;
import com.edu.schedule.entity.User;
import com.edu.schedule.mapper.UserMapper;
import com.edu.schedule.service.AuthService;
import com.edu.schedule.utils.JwtUtils;
import com.edu.schedule.utils.PasswordUtils;
import com.edu.schedule.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class AuthServiceImpl implements AuthService {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JwtUtils jwtUtils;

    @Override
    public Map<String, Object> login(String username, String password) {
        User user = userMapper.selectByUsername(username);
        if (user == null) {
            throw new BusinessException("用户名或密码错误");
        }
        if (!PasswordUtils.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        String token = jwtUtils.generate(user.getId(), user.getUsername(), user.getRole());
        user.setPassword(null);
        Map<String, Object> result = new HashMap<>();
        result.put("token", token);
        result.put("user", user);
        return result;
    }

    @Override
    public void register(String username, String password, String realName) {
        if (userMapper.selectByUsername(username) != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtils.encode(password));
        user.setRealName(realName);
        user.setRole(ScheduleConstants.ROLE_STUDENT);
        user.setStatus(1);
        userMapper.insert(user);
    }

    @Override
    public User currentUser() {
        UserContext.LoginUser loginUser = UserContext.require();
        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        user.setPassword(null);
        return user;
    }

    @Override
    public void changePassword(String oldPassword, String newPassword) {
        Long userId = UserContext.requireUserId();
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordUtils.matches(oldPassword, u.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(userId, PasswordUtils.encode(newPassword));
    }
}
