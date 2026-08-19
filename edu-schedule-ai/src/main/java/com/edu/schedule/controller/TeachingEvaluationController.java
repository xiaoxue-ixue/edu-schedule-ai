package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.TeachingEvaluation;
import com.edu.schedule.mapper.TeachingEvaluationMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@RestController
@RequestMapping("/api/teaching-evaluation")
public class TeachingEvaluationController {

    @Autowired
    private TeachingEvaluationMapper mapper;

    @GetMapping
    public Result<PageResult<TeachingEvaluation>> page(@RequestParam(defaultValue = "1") Integer page,
                                                       @RequestParam(defaultValue = "10") Integer size,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) Long teacherId,
                                                       @RequestParam(required = false) Long courseId,
                                                       @RequestParam(required = false) Long classId,
                                                       @RequestParam(required = false) Long termId,
                                                       @RequestParam(required = false) String evaluatorType) {
        PageHelper.startPage(page, size);
        PageInfo<TeachingEvaluation> info = new PageInfo<>(mapper.selectByCondition(keyword, teacherId, courseId, classId, termId, evaluatorType));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/all")
    public Result<List<TeachingEvaluation>> all(@RequestParam(required = false) Long teacherId,
                                                @RequestParam(required = false) Long courseId) {
        return Result.success(mapper.selectByCondition(null, teacherId, courseId, null, null, null));
    }

    @PostMapping
    public Result<Void> create(@RequestBody TeachingEvaluation evaluation) {
        evaluation.setOverallScore(calcOverall(evaluation));
        mapper.insert(evaluation);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }

    private BigDecimal calcOverall(TeachingEvaluation e) {
        BigDecimal sum = BigDecimal.ZERO;
        int n = 0;
        if (e.getDimAttitude() != null) { sum = sum.add(e.getDimAttitude()); n++; }
        if (e.getDimContent() != null) { sum = sum.add(e.getDimContent()); n++; }
        if (e.getDimMethod() != null) { sum = sum.add(e.getDimMethod()); n++; }
        if (e.getDimEffect() != null) { sum = sum.add(e.getDimEffect()); n++; }
        if (n == 0) {
            return BigDecimal.ZERO;
        }
        return sum.divide(BigDecimal.valueOf(n), 1, RoundingMode.HALF_UP);
    }
}