package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.TeacherAvailableTime;
import com.edu.schedule.mapper.TeacherAvailableTimeMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher-available-time")
public class TeacherAvailableTimeController {
    @Autowired
    private TeacherAvailableTimeMapper mapper;

    @GetMapping
    public Result<PageResult<TeacherAvailableTime>> page(@RequestParam(defaultValue = "1") Integer page,
                                                         @RequestParam(defaultValue = "10") Integer size,
                                                         @RequestParam(required = false) Long teacherId) {
        PageHelper.startPage(page, size);
        PageInfo<TeacherAvailableTime> info = new PageInfo<>(mapper.selectList(teacherId));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @PostMapping
    public Result<Void> create(@RequestBody TeacherAvailableTime e) {
        mapper.insert(e);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }
}
