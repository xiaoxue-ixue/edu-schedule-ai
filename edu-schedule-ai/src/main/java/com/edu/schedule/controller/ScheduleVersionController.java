package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.Result;
import com.edu.schedule.common.ScheduleConstants;
import com.edu.schedule.entity.ScheduleDetail;
import com.edu.schedule.entity.ScheduleVersion;
import com.edu.schedule.mapper.ScheduleConflictMapper;
import com.edu.schedule.mapper.ScheduleDetailMapper;
import com.edu.schedule.mapper.ScheduleVersionDetailMapper;
import com.edu.schedule.mapper.ScheduleVersionMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/schedule-version")
public class ScheduleVersionController {

    @Autowired private ScheduleVersionMapper versionMapper;
    @Autowired private ScheduleVersionDetailMapper versionDetailMapper;
    @Autowired private ScheduleDetailMapper detailMapper;
    @Autowired private ScheduleConflictMapper conflictMapper;
    @Autowired private JdbcTemplate jdbcTemplate;

    @GetMapping
    public Result<List<ScheduleVersion>> list(
            @RequestParam(required = false) Long termId,
            @RequestParam(required = false) String status) {
        List<ScheduleVersion> all = versionMapper.selectAll();
        List<ScheduleVersion> list = new ArrayList<>();
        for (ScheduleVersion v : all) {
            if (termId != null && !termId.equals(v.getTermId())) continue;
            if (status != null && !status.isEmpty() && !status.equals(v.getStatus())) continue;
            list.add(v);
        }
        return Result.success(list);
    }

    @GetMapping("/{id}")
    public Result<ScheduleVersion> get(@PathVariable Long id) {
        return Result.success(versionMapper.selectById(id));
    }

    /** 查看课表详情：单数 detail，对齐前端 */
    @GetMapping("/{id}/detail")
    public Result<List<Map<String, Object>>> detail(@PathVariable Long id) {
        List<Map<String, Object>> list = versionDetailMapper.selectByVersion(id);
        if (list == null || list.isEmpty()) {
            ScheduleVersion v = versionMapper.selectById(id);
            if (v != null && v.getTaskId() != null) {
                list = jdbcTemplate.queryForList(
                        "SELECT d.id, " +
                                " d.day_of_week AS weekday, " +
                                " COALESCE(s.slot_name, CONCAT('第', d.slot_id, '节')) AS sectionStart, " +
                                " '' AS sectionEnd, " +
                                " CASE WHEN d.week_pattern='EVERY' OR d.week_pattern IS NULL THEN 'ALL' WHEN d.week_pattern='ODD' THEN 'ODD' ELSE 'EVEN' END AS weekType, " +
                                " c.course_name AS courseName, " +
                                " tc.name AS teacherName, " +
                                " cl.class_name AS className, " +
                                " r.room_name AS roomName, " +
                                " cam.campus_name AS campusName " +
                                " FROM schedule_detail d " +
                                " LEFT JOIN edu_course c ON d.course_id=c.id " +
                                " LEFT JOIN edu_teacher tc ON d.teacher_id=tc.id " +
                                " LEFT JOIN edu_clazz cl ON d.class_id=cl.id " +
                                " LEFT JOIN edu_room r ON d.room_id=r.id " +
                                " LEFT JOIN edu_campus cam ON r.campus_id=cam.id " +
                                " LEFT JOIN edu_time_slot s ON s.slot_no = d.slot_id " +
                                " WHERE d.task_id=?", v.getTaskId());
            }
        }
        return Result.success(list == null ? new ArrayList<>() : list);
    }

    /** 创建版本（只创建壳，状态 DRAFT，不检查冲突，不复制快照） */
    @PostMapping
    public Result<Void> create(@RequestBody Map<String, Object> body) {
        Long termId = body.get("termId") == null ? null : Long.valueOf(body.get("termId").toString());
        Long taskId = body.get("taskId") == null ? null : Long.valueOf(body.get("taskId").toString());
        String versionName = body.get("versionName") == null ? "版本-" + System.currentTimeMillis() : body.get("versionName").toString();
        String description = body.get("description") == null ? null : body.get("description").toString();
        String status = body.get("status") == null ? ScheduleConstants.VERSION_DRAFT : body.get("status").toString();

        ScheduleVersion version = new ScheduleVersion();
        version.setVersionName(versionName);
        version.setTermId(termId);
        version.setTaskId(taskId);
        version.setStatus(status);
        version.setRemark(description);
        version.setCreateTime(new Date());
        versionMapper.insert(version);
        return Result.success();
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        ScheduleVersion v = versionMapper.selectById(id);
        if (v == null) throw new BusinessException("版本不存在");
        if (body.get("versionName") != null) v.setVersionName(body.get("versionName").toString());
        if (body.get("remark") != null || body.get("description") != null) {
            String r = body.get("remark") != null ? body.get("remark").toString() : (body.get("description") == null ? null : body.get("description").toString());
            v.setRemark(r);
        }
        if (body.get("status") != null) v.setStatus(body.get("status").toString());
        versionMapper.updateById(v);
        return Result.success();
    }

    /** 发布版本：检查冲突 + 复制明细快照（如果还没复制）+ 置 PUBLISHED */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        ScheduleVersion v = versionMapper.selectById(id);
        if (v == null) throw new BusinessException("版本不存在");

        // 1. 检查待处理冲突
        if (conflictMapper.countPending() > 0) {
            throw new BusinessException("存在待处理冲突，请先在【排课冲突检测】中处理后再发布");
        }

        // 2. 如果快照表为空 → 从排课明细复制一份
        List<Map<String, Object>> exist = versionDetailMapper.selectByVersion(id);
        if ((exist == null || exist.isEmpty()) && v.getTaskId() != null) {
            List<ScheduleDetail> details = detailMapper.selectByCondition(v.getTaskId(), v.getTermId(), null, null);
            for (ScheduleDetail d : details) {
                Map<String, Object> m = new HashMap<>();
                m.put("versionId", v.getId());
                m.put("courseId", d.getCourseId());
                m.put("teacherId", d.getTeacherId());
                m.put("classId", d.getClassId());
                m.put("roomId", d.getRoomId());
                m.put("dayOfWeek", d.getDayOfWeek());
                m.put("slotId", d.getSlotId());
                m.put("weekPattern", d.getWeekPattern());
                try { versionDetailMapper.insert(m); } catch (Exception ignore) {}
            }
        }

        // 3. 置状态 + 设置发布时间
        v.setStatus(ScheduleConstants.VERSION_PUBLISHED);
        versionMapper.updateById(v);
        return Result.success();
    }

    /** 下线版本 */
    @PostMapping("/{id}/offline")
    public Result<Void> offline(@PathVariable Long id) {
        ScheduleVersion v = versionMapper.selectById(id);
        if (v == null) throw new BusinessException("版本不存在");
        v.setStatus(ScheduleConstants.VERSION_DRAFT);
        versionMapper.updateById(v);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        versionDetailMapper.deleteByVersion(id);
        versionMapper.deleteById(id);
        return Result.success();
    }
}