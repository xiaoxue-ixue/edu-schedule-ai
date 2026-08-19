package com.edu.schedule.service.impl;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.PageResult;
import com.edu.schedule.entity.User;
import com.edu.schedule.mapper.UserMapper;
import com.edu.schedule.service.UserService;
import com.edu.schedule.utils.PasswordUtils;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public PageResult<User> page(Integer page, Integer size, String keyword, String role) {
        PageHelper.startPage(page, size);
        PageInfo<User> info = new PageInfo<>(userMapper.selectList(keyword, role));
        info.getList().forEach(u -> u.setPassword(null));
        return PageResult.from(info);
    }

    @Override
    public User getById(Long id) {
        User u = userMapper.selectById(id);
        if (u != null) {
            u.setPassword(null);
        }
        return u;
    }

    @Override
    public User getByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    @Override
    public void create(User user) {
        if (userMapper.selectByUsername(user.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        user.setPassword(PasswordUtils.encode(user.getPassword() == null ? "123456" : user.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        userMapper.insert(user);
    }

    @Override
    public void update(User user) {
        if (user.getId() == null) {
            throw new BusinessException("用户id不能为空");
        }
        // 密码单独处理：空表示不改
        userMapper.update(user);
    }

    @Override
    public void delete(Long id) {
        userMapper.deleteById(id);
    }

    @Override
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw new BusinessException("用户不存在");
        }
        if (!PasswordUtils.matches(oldPassword, u.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        userMapper.updatePassword(userId, PasswordUtils.encode(newPassword));
    }

    @Override
    public int batchCreateStudents(Long classId, String prefix, int count) {
        if (classId == null) throw new BusinessException("班级ID不能为空");
        if (count <= 0 || count > 500) throw new BusinessException("创建数量须在1-500之间");
        String p = (prefix == null || prefix.isBlank()) ? "stu" : prefix.trim();
        int success = 0;
        for (int i = 1; i <= count; i++) {
            String username = String.format("%s%03d", p, i);
            if (userMapper.selectByUsername(username) != null) continue;
            User u = new User();
            u.setUsername(username);
            u.setPassword(PasswordUtils.encode("123456"));
            u.setRealName(username);
            u.setRole("student");
            u.setClassId(classId);
            u.setStatus(1);
            try { userMapper.insert(u); success++; } catch (Exception ignore) {}
        }
        return success;
    }
}
