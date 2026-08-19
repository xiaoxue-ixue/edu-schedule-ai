package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.AiCallLog;
import com.edu.schedule.entity.AiToolCallLog;
import com.edu.schedule.ai.log.AiCallLogService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai-call-log")
public class AiCallLogController {

    @Autowired
    private AiCallLogService service;

    @GetMapping
    public Result<PageResult<AiCallLog>> page(@RequestParam(defaultValue = "1") Integer page,
                                              @RequestParam(defaultValue = "10") Integer size,
                                              @RequestParam(required = false) String keyword,
                                              @RequestParam(required = false) Long taskId) {
        return Result.success(service.pageCallLogs(page, size, keyword, taskId));
    }

    @GetMapping("/{id}")
    public Result<AiCallLog> get(@PathVariable Long id) {
        return Result.success(service.getCallLog(id));
    }

    @GetMapping("/{id}/tools")
    public Result<List<AiToolCallLog>> tools(@PathVariable Long id) {
        return Result.success(service.listToolLogsByCallLog(id));
    }

    @GetMapping("/task/{taskId}/tools")
    public Result<List<AiToolCallLog>> toolsByTask(@PathVariable Long taskId) {
        return Result.success(service.listToolLogsByTask(taskId));
    }
}
