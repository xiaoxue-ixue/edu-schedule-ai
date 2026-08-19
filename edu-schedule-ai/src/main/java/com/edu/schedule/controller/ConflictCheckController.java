package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.Clazz;
import com.edu.schedule.entity.Room;
import com.edu.schedule.entity.ScheduleConflict;
import com.edu.schedule.entity.ScheduleDetail;
import com.edu.schedule.mapper.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 排课冲突检测：教师/班级/教室三类时间撞车 + 教师不可用/教室不可用/教室容量 三类约束冲突。
 * 冲突原因直接写到排课明细行上。
 */
@RestController
@RequestMapping("/api/conflict-check")
public class ConflictCheckController {

    @Autowired private ScheduleDetailMapper detailMapper;
    @Autowired private ScheduleConflictMapper conflictMapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private TeacherAvailableTimeMapper teacherAvailableMapper;
    @Autowired private RoomAvailableTimeMapper roomAvailableMapper;

    /**
     * 执行冲突检测：遍历学期/任务下所有明细，回写 conflictFlag/conflictReason，并生成冲突记录
     */
    @PostMapping("/check")
    public Result<Map<String, Object>> check(@RequestBody Map<String, Object> body) {
        Long termId = body.get("termId") == null ? null : Long.valueOf(body.get("termId").toString());
        Long taskId = body.get("taskId") == null ? null : Long.valueOf(body.get("taskId").toString());
        if (termId == null && taskId == null) {
            throw new BusinessException("请至少指定学期或任务");
        }
        List<ScheduleDetail> details = detailMapper.selectByCondition(taskId, termId, null, null);
        if (taskId != null) {
            conflictMapper.deleteByTask(taskId);
        }
        int conflictCount = 0;
        List<Map<String, Object>> conflicts = new ArrayList<>();
        for (ScheduleDetail d : details) {
            List<String> reasons = detect(d, d.getId());
            int flag = reasons.isEmpty() ? 0 : 1;
            String reasonStr = reasons.isEmpty() ? null : String.join(";", reasons);
            detailMapper.updateConflictFlag(d.getId(), flag, reasonStr);
            if (!reasons.isEmpty()) {
                conflictCount++;
                for (String r : reasons) {
                    ScheduleConflict c = new ScheduleConflict();
                    c.setDetailId(d.getId());
                    c.setTaskId(d.getTaskId());
                    c.setTermId(d.getTermId());
                    c.setConflictType(mapType(r));
                    c.setDescription("课程" + d.getCourseId() + " 教师" + d.getTeacherId() + " 班级" + d.getClassId()
                            + " 教室" + d.getRoomId() + " 星期" + d.getDayOfWeek() + "第" + d.getSlotId() + "节: " + r);
                    c.setStatus("PENDING");
                    conflictMapper.insert(c);
                }
                Map<String, Object> ci = new LinkedHashMap<>();
                ci.put("detailId", d.getId());
                ci.put("reason", reasonStr);
                conflicts.add(ci);
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", details.size());
        result.put("conflictCount", conflictCount);
        result.put("conflicts", conflicts);
        return Result.success(result);
    }

    @GetMapping
    public Result<List<ScheduleConflict>> list(@RequestParam(required = false) Long taskId,
                                                @RequestParam(required = false) Long termId,
                                                @RequestParam(required = false) String status) {
        return Result.success(conflictMapper.selectByCondition(taskId, termId, status));
    }

    @GetMapping("/{id}")
    public Result<ScheduleConflict> get(@PathVariable Long id) {
        return Result.success(conflictMapper.selectById(id));
    }

    /**
     * 标记已处理，必须写明处理方式
     */
    @PutMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String handleMethod = body.get("handleMethod");
        if (handleMethod == null || handleMethod.isBlank()) {
            throw new BusinessException("标记已处理时必须写明处理方式");
        }
        conflictMapper.updateHandle(id, "HANDLED", handleMethod);
        return Result.success();
    }

    private List<String> detect(ScheduleDetail d, Long excludeId) {
        List<String> reasons = new ArrayList<>();
        if (detailMapper.countTeacherTimeConflict(d.getTermId(), d.getTeacherId(), d.getDayOfWeek(), d.getSlotId(), excludeId) > 0) {
            reasons.add("教师时间冲突");
        }
        if (detailMapper.countClassTimeConflict(d.getTermId(), d.getClassId(), d.getDayOfWeek(), d.getSlotId(), excludeId) > 0) {
            reasons.add("班级时间冲突");
        }
        if (detailMapper.countRoomTimeConflict(d.getTermId(), d.getRoomId(), d.getDayOfWeek(), d.getSlotId(), excludeId) > 0) {
            reasons.add("教室占用冲突");
        }
        // 无配置时视为全时段可用，避免全「不可用」的误报
        var taList = teacherAvailableMapper.selectList(d.getTeacherId());
        if (taList != null && !taList.isEmpty() && taList.stream()
                .noneMatch(a -> Objects.equals(a.getDayOfWeek(), d.getDayOfWeek()) && Objects.equals(a.getSlotId(), d.getSlotId()))) {
            reasons.add("教师不可用");
        }
        var raList = roomAvailableMapper.selectList(d.getRoomId());
        if (raList != null && !raList.isEmpty() && raList.stream()
                .noneMatch(a -> Objects.equals(a.getDayOfWeek(), d.getDayOfWeek()) && Objects.equals(a.getSlotId(), d.getSlotId()))) {
            reasons.add("教室不可用");
        }
        Room room = roomMapper.selectById(d.getRoomId());
        Clazz clazz = clazzMapper.selectById(d.getClassId());
        if (room != null && clazz != null && room.getCapacity() != null && clazz.getStudentCount() != null
                && room.getCapacity() < clazz.getStudentCount()) {
            reasons.add("教室容量不足");
        }
        return reasons;
    }

    private String mapType(String reason) {
        return switch (reason) {
            case "教师时间冲突" -> "TEACHER_TIME";
            case "班级时间冲突" -> "CLASS_TIME";
            case "教室占用冲突" -> "ROOM_TIME";
            case "教师不可用" -> "TEACHER_UNAVAILABLE";
            case "教室不可用" -> "ROOM_UNAVAILABLE";
            case "教室容量不足" -> "ROOM_CAPACITY";
            default -> "OTHER";
        };
    }
}
