package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.RunLog;
import com.edu.schedule.mapper.RunLogMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/run-log")
public class RunLogController {
    @Autowired
    private RunLogMapper mapper;

    @GetMapping
    public Result<PageResult<RunLog>> page(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<RunLog> info = new PageInfo<>(mapper.selectList(keyword));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<RunLog> get(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }
}
