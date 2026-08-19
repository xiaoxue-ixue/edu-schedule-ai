package com.edu.schedule.controller;

import com.edu.schedule.common.PageResult;
import com.edu.schedule.common.Result;
import com.edu.schedule.ai.agent.ScheduleAgentService;
import com.edu.schedule.common.ScheduleConstants;
import com.edu.schedule.entity.ScheduleTask;
import com.edu.schedule.mapper.ScheduleDetailMapper;
import com.edu.schedule.mapper.ScheduleTaskMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/schedule-task")
public class ScheduleTaskController {

    @Autowired
    private ScheduleTaskMapper mapper;

    @Autowired
    private ScheduleAgentService agentService;

    @Autowired
    private ScheduleDetailMapper detailMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result<PageResult<ScheduleTask>> page(@RequestParam(defaultValue = "1") Integer page,
                                                 @RequestParam(defaultValue = "10") Integer size,
                                                 @RequestParam(required = false) String keyword) {
        PageHelper.startPage(page, size);
        PageInfo<ScheduleTask> info = new PageInfo<>(mapper.selectList(keyword));
        return Result.success(PageResult.of(page, size, info.getTotal(), info.getList()));
    }

    @GetMapping("/{id}")
    public Result<ScheduleTask> get(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }

    /**
     * 前端「刷新任务状态」按钮调用：查询任务当前状态、已排课程数、结果摘要。
     * 由于 Agent 现在是异步后台执行，前端通过此接口轮询进度。
     */
    @GetMapping("/{id}/status")
    public Result<Map<String, Object>> getStatus(@PathVariable Long id) {
        ScheduleTask task = mapper.selectById(id);
        if (task == null) {
            return Result.fail("任务不存在");
        }
        int scheduled = detailMapper.selectListByTask(id).size();
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("id", task.getId());
        status.put("taskName", task.getTaskName());
        status.put("status", task.getStatus());
        status.put("statusText", statusText(task.getStatus()));
        status.put("totalRounds", task.getTotalRounds());
        status.put("successCourses", scheduled);
        status.put("totalCourses", task.getTotalCourses());
        status.put("result", task.getResult());
        return Result.success(status);
    }

    private String statusText(String s) {
        if (ScheduleConstants.TASK_PENDING.equals(s)) return "待执行";
        if (ScheduleConstants.TASK_RUNNING.equals(s)) return "执行中";
        if (ScheduleConstants.TASK_SUCCESS.equals(s)) return "已完成";
        if (ScheduleConstants.TASK_FAIL.equals(s)) return "失败";
        return s;
    }

    @PostMapping
    public Result<Void> create(@RequestBody ScheduleTask task) {
        if (task.getTermId() == null) {
            try {
                Long id = jdbcTemplate.queryForObject(
                        "SELECT id FROM edu_term ORDER BY status=1 DESC, id DESC LIMIT 1", Long.class);
                if (id != null) task.setTermId(id);
            } catch (Exception ignore) {
            }
        }
        if (task.getModelConfigId() == null) {
            try {
                Long id = jdbcTemplate.queryForObject(
                        "SELECT id FROM ai_model_config ORDER BY is_default DESC, id DESC LIMIT 1", Long.class);
                if (id != null) task.setModelConfigId(id);
            } catch (Exception ignore) {
            }
        }
        if (task.getPromptTemplateCode() == null || task.getPromptTemplateCode().isBlank()) {
            task.setPromptTemplateCode("SCHEDULE_DEFAULT");
        }
        if (task.getTaskName() == null || task.getTaskName().isBlank()) {
            String termName = "";
            if (task.getTermId() != null) {
                try {
                    String s = jdbcTemplate.queryForObject(
                            "SELECT term_name FROM edu_term WHERE id = ?", String.class, task.getTermId());
                    if (s != null) termName = s;
                } catch (Exception ignore) {
                }
            }
            String dateTag = LocalDateTime.now().format(DateTimeFormatter.ofPattern("MM-dd HH:mm"));
            String fallback = (termName.isBlank() ? "" : termName + " ")
                    + "AI 智能排课任务 " + dateTag;
            task.setTaskName(fallback);
        }
        if (task.getStatus() == null) {
            task.setStatus(ScheduleConstants.TASK_PENDING);
        }
        // ★ 根据 courseScope 计算 totalCourses，用于前端进度条显示
        if (task.getTotalCourses() == null || task.getTotalCourses() == 0) {
            if (task.getCourseScope() != null && task.getCourseScope().startsWith("课程ID:")) {
                String ids = task.getCourseScope().substring("课程ID:".length());
                int count = (int) java.util.Arrays.stream(ids.split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).count();
                task.setTotalCourses(count);
            } else {
                try {
                    Long count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM edu_course", Long.class);
                    task.setTotalCourses(count != null ? count.intValue() : 0);
                } catch (Exception ignore) {
                    task.setTotalCourses(0);
                }
            }
        }
        mapper.insert(task);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScheduleTask task) {
        task.setId(id);
        mapper.update(task);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }

    /**
     * 执行 Agent 排课（异步模式）。
     *
     * - 先做幂等校验：如果任务已在 RUNNING，拒绝重复提交
     * - 立刻把任务状态改成 RUNNING，success_courses 取当前已排数量（用于断点续排基线）
     * - 调用 agentService.executeAsync(id) 后台异步执行，1ms 内返回
     * - 返回 { status: "SUBMITTED", message: "后台执行中，请点击刷新按钮查看状态" }
     *
     * 彻底解决：前端 HTTP 超时（默认 300s/5min）导致前端显示失败，但后端还在继续排课的「脱钩」问题。
     */
    @PostMapping("/{id}/agent")
    public Result<Map<String, Object>> executeAgent(@PathVariable Long id) {
        ScheduleTask task = mapper.selectById(id);
        if (task == null) {
            return Result.fail("排课任务不存在");
        }

        // 幂等：防止用户疯狂点击按钮导致同时跑多个 Agent 线程写同一份 detail
        if (ScheduleConstants.TASK_RUNNING.equals(task.getStatus())) {
            int scheduled = detailMapper.selectListByTask(id).size();
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("status", "RUNNING");
            resp.put("taskId", id);
            resp.put("successCourses", scheduled);
            resp.put("message", "任务已在后台运行中，请勿重复提交。请点击【刷新任务状态】查看进度。");
            return Result.success(resp);
        }

        // 先写状态：RUNNING（保留已有 success_courses 基线，不清空）
        int currentCount = detailMapper.selectListByTask(id).size();
        mapper.updateResult(id, ScheduleConstants.TASK_RUNNING, null, task.getTotalRounds() == null ? 0 : task.getTotalRounds(), currentCount);

        // 立即提交给 @Async 线程池，HTTP 线程不用等
        agentService.executeAsync(id);

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("status", "SUBMITTED");
        resp.put("taskId", id);
        resp.put("message", "任务已提交，正在后台运行中。请点击【刷新任务状态】按钮查看进度，或直接去【排课明细】页面看已排的课程。");
        resp.put("tip", "排课过程中可以关闭当前页面或做其他操作，后端会继续运行。排完后任务状态会自动变成「已完成」或「失败」。");
        resp.put("baselineScheduled", currentCount);
        return Result.success(resp);
    }
}