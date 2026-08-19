package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.Clazz;
import com.edu.schedule.mapper.ClazzMapper;
import com.edu.schedule.util.RelationFiller;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clazz")
public class ClazzController {
    @Autowired
    private ClazzMapper mapper;
    @Autowired
    private RelationFiller filler;

    @GetMapping
    public Result<PageResult<Clazz>> page(@RequestParam(defaultValue = "1") Integer page,
                                          @RequestParam(defaultValue = "10") Integer size,
                                          @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Clazz> info = new PageInfo<>(mapper.selectList(keyword));
        filler.fillClazz(info.getList());
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<Clazz> get(@PathVariable Long id) {
        Clazz e = mapper.selectById(id);
        filler.fillOne(e);
        return Result.success(e);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Clazz e) {
        mapper.insert(e);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Clazz e) {
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
