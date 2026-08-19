package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.Course;
import com.edu.schedule.mapper.CourseMapper;
import com.edu.schedule.util.RelationFiller;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/course")
public class CourseController {
    @Autowired
    private CourseMapper mapper;
    @Autowired
    private RelationFiller filler;

    @GetMapping
    public Result<PageResult<Course>> page(@RequestParam(defaultValue = "1") Integer page,
                                           @RequestParam(defaultValue = "10") Integer size,
                                           @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<Course> info = new PageInfo<>(mapper.selectList(keyword));
        filler.fillCourses(info.getList());
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<Course> get(@PathVariable Long id) {
        Course e = mapper.selectById(id);
        filler.fillOne(e);
        return Result.success(e);
    }

    @PostMapping
    public Result<Void> create(@RequestBody Course e) {
        mapper.insert(e);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Course e) {
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
