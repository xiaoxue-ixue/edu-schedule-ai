package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.Clazz;
import com.edu.schedule.entity.Room;
import com.edu.schedule.entity.ScheduleDetail;
import com.edu.schedule.entity.TeacherAvailableTime;
import com.edu.schedule.entity.RoomAvailableTime;
import com.edu.schedule.mapper.ClazzMapper;
import com.edu.schedule.mapper.RoomMapper;
import com.edu.schedule.mapper.ScheduleDetailMapper;
import com.edu.schedule.mapper.TeacherAvailableTimeMapper;
import com.edu.schedule.mapper.RoomAvailableTimeMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 排课明细管理：查看 Agent 排出的课表，手工补课和改课均含教师/班级/教室三维占用校验。
 */
@RestController
@RequestMapping("/api/schedule-detail")
public class ScheduleDetailController {

    @Autowired private ScheduleDetailMapper mapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private TeacherAvailableTimeMapper teacherAvailableMapper;
    @Autowired private RoomAvailableTimeMapper roomAvailableMapper;

    @GetMapping
    public Result<List<ScheduleDetail>> list(@RequestParam(required = false) Long taskId,
                                             @RequestParam(required = false) Long termId,
                                             @RequestParam(required = false) Long teacherId,
                                             @RequestParam(required = false) Long classId) {
        return Result.success(mapper.selectByCondition(taskId, termId, teacherId, classId));
    }

    @GetMapping("/{id}")
    public Result<ScheduleDetail> get(@PathVariable Long id) {
        return Result.success(mapper.selectById(id));
    }

    /**
     * 手工补课：写入前做三维占用 + 教师可用 + 教室可用 + 容量校验，冲突原因写入明细
     */
    @PostMapping
    public Result<Void> create(@RequestBody ScheduleDetail detail) {
        detail.setSource("MANUAL");
        String reason = checkConflicts(detail, null);
        detail.setConflictFlag(reason == null ? 0 : 1);
        detail.setConflictReason(reason);
        if (detail.getStatus() == null) {
            detail.setStatus(1);
        }
        mapper.insert(detail);
        return Result.success();
    }

    /**
     * 改课：编辑现有明细，校验时排除自身
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody ScheduleDetail detail) {
        detail.setId(id);
        String reason = checkConflicts(detail, id);
        detail.setConflictFlag(reason == null ? 0 : 1);
        detail.setConflictReason(reason);
        mapper.update(detail);
        mapper.updateConflictFlag(id, reason == null ? 0 : 1, reason);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        mapper.deleteById(id);
        return Result.success();
    }

    /**
     * 三维占用 + 四层约束校验，返回冲突原因（无冲突返回 null）
     */
    private String checkConflicts(ScheduleDetail d, Long excludeId) {
        List<String> reasons = new ArrayList<>();
        if (mapper.countTeacherTimeConflict(d.getTermId(), d.getTeacherId(), d.getDayOfWeek(), d.getSlotId(), excludeId) > 0) {
            reasons.add("教师时间冲突");
        }
        if (mapper.countClassTimeConflict(d.getTermId(), d.getClassId(), d.getDayOfWeek(), d.getSlotId(), excludeId) > 0) {
            reasons.add("班级时间冲突");
        }
        if (mapper.countRoomTimeConflict(d.getTermId(), d.getRoomId(), d.getDayOfWeek(), d.getSlotId(), excludeId) > 0) {
            reasons.add("教室占用冲突");
        }
        boolean teacherOk = true;
        List<TeacherAvailableTime> ta = teacherAvailableMapper.selectList(d.getTeacherId());
        if (ta != null && !ta.isEmpty()) {
            teacherOk = ta.stream()
                    .anyMatch(a -> Objects.equals(a.getDayOfWeek(), d.getDayOfWeek()) && Objects.equals(a.getSlotId(), d.getSlotId()));
        }
        if (!teacherOk) {
            reasons.add("教师不可用");
        }
        boolean roomOk = true;
        List<RoomAvailableTime> ra = roomAvailableMapper.selectList(d.getRoomId());
        if (ra != null && !ra.isEmpty()) {
            roomOk = ra.stream()
                    .anyMatch(a -> Objects.equals(a.getDayOfWeek(), d.getDayOfWeek()) && Objects.equals(a.getSlotId(), d.getSlotId()));
        }
        if (!roomOk) {
            reasons.add("教室不可用");
        }
        Room room = roomMapper.selectById(d.getRoomId());
        Clazz clazz = clazzMapper.selectById(d.getClassId());
        if (room != null && clazz != null && room.getCapacity() != null && clazz.getStudentCount() != null
                && room.getCapacity() < clazz.getStudentCount()) {
            reasons.add("教室容量不足");
        }
        return reasons.isEmpty() ? null : String.join(";", reasons);
    }
    @DeleteMapping("/clear")
    public Result<Void> clear() {
        mapper.clearAll();
        return Result.success();
    }
}
