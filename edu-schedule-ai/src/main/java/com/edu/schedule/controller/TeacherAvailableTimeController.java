package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.TeacherAvailableTime;
import com.edu.schedule.entity.User;
import com.edu.schedule.mapper.TeacherAvailableTimeMapper;
import com.edu.schedule.mapper.UserMapper;
import com.edu.schedule.utils.UserContext;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher-available-time")
public class TeacherAvailableTimeController {

    @Autowired
    private TeacherAvailableTimeMapper mapper;
    @Autowired
    private UserMapper userMapper;

    @GetMapping
    public Result<PageResult<TeacherAvailableTime>> page(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer size,
                                                         @RequestParam(required = false) Long teacherId) {
        String role = UserContext.getRole();
        // 教师角色：只能查自己的
        if ("teacher".equalsIgnoreCase(role)) {
            teacherId = currentTeacherId();
        }
        PageHelper.startPage(page, size);
        PageInfo<TeacherAvailableTime> info = new PageInfo<>(mapper.selectList(teacherId));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody TeacherAvailableTime e) {
        String role = UserContext.getRole();
        // 教师角色：强制设置为自己的 teacherId，防止越权
        if ("teacher".equalsIgnoreCase(role)) {
            e.setTeacherId(currentTeacherId());
        }
        mapper.insert(e);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        String role = UserContext.getRole();
        // 教师角色：验证记录属于自己才能删
        if ("teacher".equalsIgnoreCase(role)) {
            TeacherAvailableTime existing = mapper.selectById(id);
            if (existing == null || !existing.getTeacherId().equals(currentTeacherId())) {
                throw new BusinessException(com.edu.schedule.common.ResultCode.FORBIDDEN, "无权删除他人数据");
            }
        }
        mapper.deleteById(id);
        return Result.success();
    }

    private Long currentTeacherId() {
        UserContext.LoginUser loginUser = UserContext.require();
        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null || user.getTeacherId() == null) {
            throw new BusinessException(com.edu.schedule.common.ResultCode.FORBIDDEN, "当前账号未关联教师信息");
        }
        return user.getTeacherId();
    }
}