package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.User;
import com.edu.schedule.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping
    public Result<PageResult<User>> page(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String keyword,
                                         @RequestParam(required = false) String role) {
        return Result.success(userService.page(page, size, keyword, role));
    }

    @GetMapping("/{id}")
    public Result<User> get(@PathVariable Long id) {
        return Result.success(userService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody User user) {
        userService.create(user);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.update(user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        userService.delete(id);
        return Result.success();
    }

    /** 按班级批量创建学生账号（用户名前缀+序号，默认密码123456） */
    @PostMapping("/batch-students")
    public Result<Integer> batchCreateStudents(@RequestBody java.util.Map<String, Object> body) {
        Long classId = Long.valueOf(body.get("classId").toString());
        String prefix = body.get("prefix") == null ? null : body.get("prefix").toString();
        int count = Integer.parseInt(body.get("count").toString());
        int created = userService.batchCreateStudents(classId, prefix, count);
        return Result.success(created);
    }
}
