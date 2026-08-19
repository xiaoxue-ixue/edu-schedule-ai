package com.edu.schedule.controller;

import com.edu.schedule.common.Result;
import com.edu.schedule.entity.RunLog;
import com.edu.schedule.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 教务首页：排课任务、AI 调用、冲突与质量分析等运行数据
 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired private ScheduleTaskMapper taskMapper;
    @Autowired private AiCallLogMapper callLogMapper;
    @Autowired private ScheduleConflictMapper conflictMapper;
    @Autowired private ScheduleDetailMapper detailMapper;
    @Autowired private TeachingEvaluationMapper evaluationMapper;
    @Autowired private AiQualityAnalysisMapper analysisMapper;
    @Autowired private RunLogMapper runLogMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("taskTotal", taskMapper.countAll());
        m.put("taskSuccess", taskMapper.countSuccess());
        m.put("aiCallTotal", callLogMapper.countAll());
        m.put("aiCallFail", callLogMapper.countFail());
        m.put("conflictPending", conflictMapper.countPending());
        m.put("scheduleDetailTotal", detailMapper.selectByCondition(null, null, null, null).size());
        m.put("evaluationTotal", evaluationMapper.countAll());
        m.put("qualityAnalysisTotal", analysisMapper.selectByCondition(null, null, null).size());
        m.put("runLogTotal", runLogMapper.selectList(null).size());
        return Result.success(m);
    }

    /**
     * 首页「最近动态」时间线：合并排课任务、AI 质量分析、运行日志、公告的最近 8 条
     */
    @GetMapping("/timeline")
    public Result<List<Map<String, Object>>> timeline() {
        List<Map<String, Object>> list = new ArrayList<>();

        // 1. 最近的排课任务
        try {
            taskMapper.selectList(null).stream().limit(2).forEach(t -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("time", t.getUpdateTime() == null ? "" : t.getUpdateTime().toString().replace("T", " ").substring(0, 16));
                m.put("type", "primary");
                String txt = "任务「" + (t.getTaskName() == null ? "" : t.getTaskName()) + "」";
                if ("SUCCESS".equals(t.getStatus())) txt += " 已完成排课";
                else if ("RUNNING".equals(t.getStatus())) txt += " 正在执行排课";
                else if ("FAIL".equals(t.getStatus())) txt += " 执行失败";
                else txt += " 创建完成";
                m.put("content", txt);
                list.add(m);
            });
        } catch (Exception ignore) {}

        // 2. 最近的质量分析
        try {
            analysisMapper.selectByCondition(null, null, null).stream().limit(2).forEach(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("time", a.getCreateTime() == null ? "" : a.getCreateTime().toString().replace("T", " ").substring(0, 16));
                m.put("type", "success");
                m.put("content", "AI 教学质量分析「" + a.getTargetName() + "」报告生成完毕，等级：" + a.getQualityLevel());
                list.add(m);
            });
        } catch (Exception ignore) {}

        // 3. 最近的运行日志（RunLog 字段：module / type / content）
        try {
            List<RunLog> logs = runLogMapper.selectList(null);
            if (logs != null) {
                logs.stream().limit(2).forEach(r -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("time", r.getCreateTime() == null ? "" : r.getCreateTime().toString().replace("T", " ").substring(0, 16));
                    m.put("type", "info");
                    String cat = r.getModule() == null ? "系统" : r.getModule();
                    String act = r.getType() == null ? "" : r.getType();
                    String detail = r.getContent() == null ? "" : (r.getContent().length() > 30 ? r.getContent().substring(0, 30) : r.getContent());
                    m.put("content", "[" + cat + "] " + act + (detail.isBlank() ? "" : "：" + detail));
                    list.add(m);
                });
            }
        } catch (Exception ignore) {}

        // 4. 最近的公告
        try {
            List<Map<String, Object>> anns = jdbcTemplate.queryForList(
                    "SELECT title, DATE_FORMAT(create_time, '%Y-%m-%d %H:%i') AS time FROM announcement ORDER BY id DESC LIMIT 2");
            anns.forEach(a -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("time", a.get("time") == null ? "" : a.get("time").toString());
                m.put("type", "warning");
                m.put("content", "发布公告：" + (a.get("title") == null ? "" : a.get("title")));
                list.add(m);
            });
        } catch (Exception ignore) {}

        // 按时间倒序，取前 8 条
        list.sort((a, b) -> {
            String at = a.get("time") == null ? "" : a.get("time").toString();
            String bt = b.get("time") == null ? "" : b.get("time").toString();
            return bt.compareTo(at);
        });
        return Result.success(list.stream().limit(8).collect(Collectors.toList()));
    }
}