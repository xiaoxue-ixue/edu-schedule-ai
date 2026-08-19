package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.Term;
import com.edu.schedule.mapper.TermMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/term")
public class TermController {
    @Autowired
    private TermMapper mapper;

    @GetMapping
    public Result<PageResult<Term>> page(@RequestParam(defaultValue = "1") Integer page,
                                         @RequestParam(defaultValue = "10") Integer size,
                                         @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Term> info = new PageInfo<>(mapper.selectList(keyword));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<Term> get(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Term e) {
        mapper.insert(e);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Term e) {
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
