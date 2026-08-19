package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.ScheduleRule;
import com.edu.schedule.mapper.ScheduleRuleMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/schedule-rule")
public class ScheduleRuleController {
    @Autowired
    private ScheduleRuleMapper mapper;

    @GetMapping
    public Result<PageResult<ScheduleRule>> page(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<ScheduleRule> info = new PageInfo<>(mapper.selectList(keyword));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<ScheduleRule> get(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody ScheduleRule e) {
        mapper.insert(e);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScheduleRule e) {
        e.setId(id);
        mapper.update(e);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }
}
