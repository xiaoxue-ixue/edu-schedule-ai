package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.PromptTemplate;
import com.edu.schedule.mapper.PromptTemplateMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/prompt-template")
public class PromptTemplateController {

    @Autowired
    private PromptTemplateMapper mapper;

    @GetMapping
    public Result<PageResult<PromptTemplate>> page(@RequestParam(defaultValue = "1") Integer page,
                                                    @RequestParam(defaultValue = "10") Integer size,
                                                    @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<PromptTemplate> info = new PageInfo<>(mapper.selectList(keyword));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<PromptTemplate> get(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }

    @GetMapping("/code/{code}")
    public Result<PromptTemplate> getByCode(@PathVariable String code) {
        return Result.success(mapper.selectByCode(code));
    }

    @PostMapping
    public Result<Void> create(@RequestBody PromptTemplate template) {
        if (template.getEnabled() == null) {
            template.setEnabled(1);
        }
        mapper.insert(template);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody PromptTemplate template) {
        template.setId(id);
        mapper.update(template);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }
}
