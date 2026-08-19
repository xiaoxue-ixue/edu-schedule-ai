package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.ai.knowledge.KnowledgeService;
import com.edu.schedule.entity.Knowledge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    @Autowired
    private KnowledgeService knowledgeService;

    @GetMapping
    public Result<PageResult<Knowledge>> page(@RequestParam(defaultValue = "1") Integer page,
                                               @RequestParam(defaultValue = "10") Integer size,
                                               @RequestParam(required = false) String keyword) {
        return Result.success(knowledgeService.page(page, size, keyword));
    }

    @GetMapping("/{id}")
    public Result<Knowledge> get(@PathVariable Long id) {
        return Result.success(knowledgeService.getById(id));
    }

    @PostMapping
    public Result<Void> create(@RequestBody Knowledge knowledge) {
        knowledgeService.create(knowledge);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Knowledge knowledge) {
        knowledge.setId(id);
        knowledgeService.update(knowledge);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        knowledgeService.delete(id);
        return Result.success();
    }

    /**
     * 知识库检索预览（管理员可测）
     */
    @GetMapping("/search")
    public Result<List<Map<String, Object>>> search(@RequestParam String keyword,
                                                     @RequestParam(defaultValue = "5") Integer topN) {
        return Result.success(knowledgeService.search(keyword, topN));
    }
}
