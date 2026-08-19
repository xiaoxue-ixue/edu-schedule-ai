package com.edu.schedule.ai.tool;

import cn.hutool.json.JSONUtil;
import com.edu.schedule.ai.knowledge.KnowledgeService;
import com.edu.schedule.ai.log.AiCallLogService;
import com.edu.schedule.entity.*;
import com.edu.schedule.mapper.*;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 排课工具集（Function Calling）。
 * 2026-08-15 新增：每次工具调用后 sleep 1500ms，降低 TPM 消耗速率，避免 429 限流。
 */
@Component
public class ScheduleToolSet {

    @Autowired private TeacherMapper teacherMapper;
    @Autowired private RoomMapper roomMapper;
    @Autowired private ClazzMapper clazzMapper;
    @Autowired private CourseMapper courseMapper;
    @Autowired private ScheduleRuleMapper ruleMapper;
    @Autowired private TeacherAvailableTimeMapper teacherAvailableMapper;
    @Autowired private RoomAvailableTimeMapper roomAvailableMapper;
    @Autowired private ScheduleDetailMapper scheduleDetailMapper;
    @Autowired private ScheduleTaskMapper taskMapper;
    @Autowired private KnowledgeService knowledgeService;
    @Autowired private AiCallLogService aiCallLogService;

    /** 工具间节流毫秒数（防止 TPM 冲顶触发 429） */
    private static final int THROTTLE_MS = 1500;

    // ==================== 查询类 ====================

    @Tool(description = "查询所有可排课教师列表，返回教师ID、姓名、工号、职称及其可用时间(星期1-7与节次ID)。排课前必须先调用本工具拿到真实教师ID。")
    public String queryTeachers() {
        long start = System.currentTimeMillis();
        String result;
        try {
            List<Teacher> teachers = teacherMapper.selectList(null);
            List<Map<String, Object>> data = new ArrayList<>();
            for (Teacher t : teachers) {
                List<TeacherAvailableTime> avail = teacherAvailableMapper.selectList(t.getId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("teacherId", t.getId());
                m.put("teacherNo", t.getTeacherNo());
                m.put("name", t.getName());
                m.put("title", t.getTitle());
                m.put("availableTimes", avail.stream()
                        .map(a -> a.getDayOfWeek() + "-" + a.getSlotId())
                        .collect(Collectors.toList()));
                data.add(m);
            }
            result = JSONUtil.toJsonStr(Map.of("total", data.size(), "teachers", data));
            log("queryTeachers", "", result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            log("queryTeachers", "", result, false, start);
            return result;
        }
    }

    @Tool(description = "查询所有教室列表，返回教室ID、名称、容量、类型及其可用时间(星期-节次)。选教室前必须先调用本工具拿到真实教室ID。")
    public String queryRooms() {
        long start = System.currentTimeMillis();
        String result;
        try {
            List<Room> rooms = roomMapper.selectList(null);
            List<Map<String, Object>> data = new ArrayList<>();
            for (Room r : rooms) {
                List<RoomAvailableTime> avail = roomAvailableMapper.selectList(r.getId());
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("roomId", r.getId());
                m.put("roomName", r.getRoomName());
                m.put("capacity", r.getCapacity());
                m.put("roomType", r.getRoomType());
                m.put("availableTimes", avail.stream()
                        .map(a -> a.getDayOfWeek() + "-" + a.getSlotId())
                        .collect(Collectors.toList()));
                data.add(m);
            }
            result = JSONUtil.toJsonStr(Map.of("total", data.size(), "rooms", data));
            log("queryRooms", "", result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            log("queryRooms", "", result, false, start);
            return result;
        }
    }

    @Tool(description = "查询所有班级列表，返回班级ID、名称、人数。排课时需用班级人数校验教室容量。")
    public String queryClasses() {
        long start = System.currentTimeMillis();
        String result;
        try {
            List<Clazz> classes = clazzMapper.selectList(null);
            List<Map<String, Object>> data = classes.stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("classId", c.getId());
                m.put("className", c.getClassName());
                m.put("studentCount", c.getStudentCount());
                return m;
            }).collect(Collectors.toList());
            result = JSONUtil.toJsonStr(Map.of("total", data.size(), "classes", data));
            log("queryClasses", "", result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            log("queryClasses", "", result, false, start);
            return result;
        }
    }

    @Tool(description = "查询待排课程列表，返回课程ID、课程名、学时、主讲教师ID。可传 taskId 只返回该任务指定的课程；不传则返回全部课程。")
    public String queryCourses(
            @ToolParam(description = "排课任务ID（可选），传了就只返回该任务绑定的课程") Long taskId) {
        long start = System.currentTimeMillis();
        String result;
        try {
            List<Course> courses;
            if (taskId != null) {
                ScheduleTask task = taskMapper.selectById(taskId);
                if (task != null && task.getCourseScope() != null && task.getCourseScope().startsWith("课程ID:")) {
                    String ids = task.getCourseScope().substring("课程ID:".length());
                    List<Long> courseIds = Arrays.stream(ids.split(","))
                            .map(String::trim).filter(s -> !s.isEmpty())
                            .map(Long::parseLong).collect(Collectors.toList());
                    courses = courseMapper.selectByIds(courseIds);
                } else {
                    courses = courseMapper.selectList(null);
                }
            } else {
                courses = courseMapper.selectList(null);
            }
            List<Map<String, Object>> data = courses.stream().map(c -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("courseId", c.getId());
                m.put("courseName", c.getCourseName());
                m.put("hours", c.getHours());
                m.put("teacherId", c.getTeacherId());
                return m;
            }).collect(Collectors.toList());
            result = JSONUtil.toJsonStr(Map.of("total", data.size(), "courses", data));
            log("queryCourses", "taskId=" + taskId, result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            log("queryCourses", "taskId=" + taskId, result, false, start);
            return result;
        }
    }

    @Tool(description = "查询排课规则，返回硬规则与软规则及优先级。排课必须遵守所有硬规则。")
    public String queryScheduleRules() {
        long start = System.currentTimeMillis();
        String result;
        try {
            List<ScheduleRule> rules = ruleMapper.selectList(null);
            List<Map<String, Object>> data = rules.stream().map(r -> {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("ruleName", r.getRuleName());
                m.put("ruleType", r.getRuleType());
                m.put("priority", r.getPriority());
                m.put("triggerCondition", r.getTriggerCondition());
                m.put("action", r.getAction());
                return m;
            }).collect(Collectors.toList());
            result = JSONUtil.toJsonStr(Map.of("total", data.size(), "rules", data));
            log("queryScheduleRules", "", result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            log("queryScheduleRules", "", result, false, start);
            return result;
        }
    }

    // ==================== 检测类 ====================

    @Tool(description = "对指定 教师/班级/教室/星期/节次 做四层硬约束检测：教师时间冲突、班级时间冲突、教室占用冲突、教师不可用、教室不可用、教室容量不足。全部通过才允许排课。返回 pass=true 表示可排，pass=false 时 reasons 给出冲突原因。")
    public String checkScheduleConflict(
            @ToolParam(description = "学期ID") Long termId,
            @ToolParam(description = "教师ID") Long teacherId,
            @ToolParam(description = "班级ID") Long classId,
            @ToolParam(description = "教室ID") Long roomId,
            @ToolParam(description = "星期 1-7") Integer dayOfWeek,
            @ToolParam(description = "节次ID") Long slotId) {
        long start = System.currentTimeMillis();
        String args = JSONUtil.toJsonStr(Map.of("termId", termId, "teacherId", teacherId, "classId", classId,
                "roomId", roomId, "dayOfWeek", dayOfWeek, "slotId", slotId));
        String result;
        try {
            List<String> reasons = new ArrayList<>();
            if (scheduleDetailMapper.countTeacherTimeConflict(termId, teacherId, dayOfWeek, slotId, null) > 0) {
                reasons.add("教师时间冲突: 该教师在星期" + dayOfWeek + "第" + slotId + "节已有课");
            }
            if (scheduleDetailMapper.countClassTimeConflict(termId, classId, dayOfWeek, slotId, null) > 0) {
                reasons.add("班级时间冲突: 该班级在星期" + dayOfWeek + "第" + slotId + "节已有课");
            }
            if (scheduleDetailMapper.countRoomTimeConflict(termId, roomId, dayOfWeek, slotId, null) > 0) {
                reasons.add("教室占用冲突: 该教室在星期" + dayOfWeek + "第" + slotId + "节已被占用");
            }
            List<TeacherAvailableTime> teacherAvail = teacherAvailableMapper.selectList(teacherId);
            if (teacherAvail != null && !teacherAvail.isEmpty()) {
                boolean teacherOk = teacherAvail.stream()
                        .anyMatch(a -> Objects.equals(a.getDayOfWeek(), dayOfWeek) && Objects.equals(a.getSlotId(), slotId));
                if (!teacherOk) {
                    reasons.add("教师不可用: 教师在星期" + dayOfWeek + "第" + slotId + "节不可排课");
                }
            }
            List<RoomAvailableTime> roomAvail = roomAvailableMapper.selectList(roomId);
            if (roomAvail != null && !roomAvail.isEmpty()) {
                boolean roomOk = roomAvail.stream()
                        .anyMatch(a -> Objects.equals(a.getDayOfWeek(), dayOfWeek) && Objects.equals(a.getSlotId(), slotId));
                if (!roomOk) {
                    reasons.add("教室不可用: 教室在星期" + dayOfWeek + "第" + slotId + "节不可使用");
                }
            }
            Room room = roomMapper.selectById(roomId);
            Clazz clazz = clazzMapper.selectById(classId);
            if (room != null && clazz != null && room.getCapacity() != null && clazz.getStudentCount() != null
                    && room.getCapacity() < clazz.getStudentCount()) {
                reasons.add("教室容量不足: 教室容量" + room.getCapacity() + "小于班级人数" + clazz.getStudentCount());
            }

            Map<String, Object> res = new LinkedHashMap<>();
            res.put("pass", reasons.isEmpty());
            res.put("reasons", reasons);
            result = JSONUtil.toJsonStr(res);
            log("checkScheduleConflict", args, result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"pass\":false,\"error\":\"" + e.getMessage() + "\"}";
            log("checkScheduleConflict", args, result, false, start);
            return result;
        }
    }

    // ==================== 写入类（唯一可写数据库的工具） ====================

    @Tool(description = "将一节通过冲突检测的排课结果写入排课明细表。这是唯一能修改数据库的工具，且只能写排课明细表。调用前必须先用 checkScheduleConflict 确认 pass=true。参数 weekPattern 可不传，默认1-16周。")
    public String writeScheduleDetail(
            @ToolParam(description = "排课任务ID") Long taskId,
            @ToolParam(description = "学期ID") Long termId,
            @ToolParam(description = "课程ID") Long courseId,
            @ToolParam(description = "教师ID") Long teacherId,
            @ToolParam(description = "班级ID") Long classId,
            @ToolParam(description = "教室ID") Long roomId,
            @ToolParam(description = "星期 1-7") Integer dayOfWeek,
            @ToolParam(description = "节次ID") Long slotId,
            @ToolParam(description = "周次，如 1-16，可不传") String weekPattern) {
        long start = System.currentTimeMillis();
        String args = JSONUtil.toJsonStr(Map.of("taskId", taskId, "termId", termId, "courseId", courseId,
                "teacherId", teacherId, "classId", classId, "roomId", roomId, "dayOfWeek", dayOfWeek, "slotId", slotId));
        String result;
        try {
            List<String> missing = new ArrayList<>();
            if (taskId == null) missing.add("taskId");
            if (termId == null) missing.add("termId");
            if (courseId == null) missing.add("courseId");
            if (teacherId == null) missing.add("teacherId");
            if (classId == null) missing.add("classId");
            if (roomId == null) missing.add("roomId");
            if (dayOfWeek == null) missing.add("dayOfWeek");
            if (slotId == null) missing.add("slotId");
            if (!missing.isEmpty()) {
                result = "{\"success\":false,\"error\":\"缺少必填参数: " + String.join(",", missing) + "\"}";
                System.err.println("❌ [writeScheduleDetail] 参数不完整: args=" + args + " 缺失=" + missing);
                log("writeScheduleDetail", args, result, false, start);
                return result;
            }

            ScheduleDetail detail = new ScheduleDetail();
            detail.setTaskId(taskId);
            detail.setTermId(termId);
            detail.setCourseId(courseId);
            detail.setTeacherId(teacherId);
            detail.setClassId(classId);
            detail.setRoomId(roomId);
            detail.setDayOfWeek(dayOfWeek);
            detail.setSlotId(slotId);
            detail.setWeekPattern(weekPattern == null || weekPattern.isBlank() ? "1-16" : weekPattern);
            detail.setSource("AGENT");
            detail.setConflictFlag(0);
            detail.setStatus(1);

            System.err.println("📝 [writeScheduleDetail] 准备插入排课明细: taskId=" + taskId
                    + " termId=" + termId + " courseId=" + courseId + " teacherId=" + teacherId
                    + " classId=" + classId + " roomId=" + roomId + " 星期" + dayOfWeek + " 第" + slotId + "节");

            int rows = scheduleDetailMapper.insert(detail);
            Long genId = detail.getId();

            System.err.println("✅ [writeScheduleDetail] insert 执行结果: affectedRows=" + rows
                    + " 生成的主键 detailId=" + genId);

            if (rows <= 0) {
                result = "{\"success\":false,\"error\":\"INSERT 影响行数为 0，数据库未写入任何记录\"}";
                System.err.println("❌ [writeScheduleDetail] INSERT 返回 0 行！args=" + args);
                log("writeScheduleDetail", args, result, false, start);
                return result;
            }
            if (genId == null) {
                result = "{\"success\":false,\"error\":\"INSERT 成功但未获得自增主键，可能 MyBatis 配置问题\"}";
                System.err.println("❌ [writeScheduleDetail] 主键回写失败！args=" + args);
                log("writeScheduleDetail", args, result, false, start);
                return result;
            }

            ScheduleDetail verify = scheduleDetailMapper.selectById(genId);
            if (verify == null) {
                result = "{\"success\":false,\"error\":\"INSERT 返回成功但立刻回查无记录，可能事务回滚或写入了错误的数据源\"}";
                System.err.println("❌ [writeScheduleDetail] 写入后回查失败！detailId=" + genId + " 不存在！args=" + args);
                log("writeScheduleDetail", args, result, false, start);
                return result;
            }

            System.err.println("🔍 [writeScheduleDetail] 写入后回查验证通过: detailId=" + genId
                    + " taskId=" + verify.getTaskId() + " courseId=" + verify.getCourseId());

            result = JSONUtil.toJsonStr(Map.of("success", true, "detailId", genId,
                    "message", "排课明细已写入: 课程" + courseId + " 教师" + teacherId + " 班级" + classId + " 教室" + roomId + " 星期" + dayOfWeek + "第" + slotId + "节"));
            log("writeScheduleDetail", args, result, true, start);
            return result;
        } catch (Exception e) {
            String stackTrace = org.springframework.util.StringUtils.hasText(e.getMessage()) ? e.getMessage() : e.toString();
            result = "{\"success\":false,\"error\":\"" + stackTrace.replace("\"", "'") + "\"}";
            System.err.println("❌ [writeScheduleDetail] 抛出异常: " + e.getClass().getSimpleName() + ": " + stackTrace);
            e.printStackTrace(System.err);
            log("writeScheduleDetail", args, result, false, start);
            return result;
        }
    }

    // ==================== 知识库检索 ====================

    @Tool(description = "按关键词检索教务知识库(教务制度、排课规则说明、调课流程等)，返回最相关的切片。需要制度依据时调用。")
    public String searchEducationKnowledge(@ToolParam(description = "检索关键词") String keyword) {
        long start = System.currentTimeMillis();
        String args = JSONUtil.toJsonStr(Map.of("keyword", keyword));
        String result;
        try {
            List<Map<String, Object>> hits = knowledgeService.search(keyword, 5);
            result = JSONUtil.toJsonStr(Map.of("total", hits.size(), "hits", hits));
            log("searchEducationKnowledge", args, result, true, start);
            return result;
        } catch (Exception e) {
            result = "{\"error\":\"" + e.getMessage() + "\"}";
            log("searchEducationKnowledge", args, result, false, start);
            return result;
        }
    }

    /**
     * 记录工具调用日志，并做 1.5 秒节流，降低 TPM 消耗。
     * 每次工具执行完等 1.5 秒再把结果返回给模型，有效防止 Token 瞬时冲顶触发 429。
     */
    private void log(String toolName, String args, String result, boolean success, long start) {
        try {
            aiCallLogService.recordToolCall(toolName, args, result, success, System.currentTimeMillis() - start);
        } catch (Exception ignore) {
        }
        // ===== 工具间节流（1.5 秒） =====
        try {
            Thread.sleep(THROTTLE_MS);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }
}