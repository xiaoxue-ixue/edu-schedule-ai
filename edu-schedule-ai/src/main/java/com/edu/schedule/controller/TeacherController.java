package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.Teacher;
import com.edu.schedule.mapper.TeacherMapper;
import com.edu.schedule.util.RelationFiller;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/teacher")
public class TeacherController {
    @Autowired
    private TeacherMapper mapper;
    @Autowired
    private RelationFiller filler;

    @GetMapping
    public Result<PageResult<Teacher>> page(@RequestParam(defaultValue = "1") Integer page,
                                            @RequestParam(defaultValue = "10") Integer size,
                                            @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Teacher> info = new PageInfo<>(mapper.selectList(keyword));
        filler.fillTeachers(info.getList());
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<Teacher> get(@PathVariable Long id) {
        Teacher e = mapper.selectById(id);
        filler.fillOne(e);
        return Result.success(e);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Teacher e) {
        mapper.insert(e);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Teacher e) {
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
