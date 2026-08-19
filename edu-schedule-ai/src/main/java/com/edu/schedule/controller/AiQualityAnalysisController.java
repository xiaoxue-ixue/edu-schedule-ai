package com.edu.schedule.controller;

import com.edu.schedule.common.Result;
import com.edu.schedule.ai.AiQualityAnalysisService;
import com.edu.schedule.entity.AiQualityAnalysis;
import com.edu.schedule.mapper.AiQualityAnalysisMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai-quality-analysis")
public class AiQualityAnalysisController {

    @Autowired
    private AiQualityAnalysisService service;

    @Autowired
    private AiQualityAnalysisMapper mapper;

    @PostMapping("/generate")
    public Result<AiQualityAnalysis> generate(@RequestBody Map<String, Object> body) {
        Long termId = Long.valueOf(body.get("termId").toString());
        String dimension = body.getOrDefault("dimension", "COURSE").toString();
        Long targetId = body.get("targetId") == null ? null : Long.valueOf(body.get("targetId").toString());
        return Result.success(service.generate(termId, dimension, targetId));
    }

    @GetMapping
    public Result<List<AiQualityAnalysis>> list(@RequestParam(required = false) Long termId,
                                                @RequestParam(required = false) String dimension,
                                                @RequestParam(required = false) String qualityLevel) {
        return Result.success(service.list(termId, dimension, qualityLevel));
    }

    @GetMapping("/{id}")
    public Result<AiQualityAnalysis> get(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }
}