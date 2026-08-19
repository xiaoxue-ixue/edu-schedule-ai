package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.Result;
import com.edu.schedule.common.ScheduleConstants;
import com.edu.schedule.entity.Announcement;
import com.edu.schedule.entity.ScheduleVersion;
import com.edu.schedule.entity.TeachingEvaluation;
import com.edu.schedule.entity.User;
import com.edu.schedule.mapper.*;
import com.edu.schedule.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学生端：课表首页 / 教务公告 / 教学评教
 *
 * 评教提交后，评价人、评价类型、所属学期、提交时间全部由后端赋值，
 * 前端传什么都会被覆盖，避免伪造他人评价或把评价挂到别的学期上。
 */
@RestController
@RequestMapping("/api/student")
public class StudentPortalController {

    @Autowired private UserMapper userMapper;
    @Autowired private ScheduleVersionMapper versionMapper;
    @Autowired private ScheduleVersionDetailMapper versionDetailMapper;
    @Autowired private AnnouncementMapper announcementMapper;
    @Autowired private TeachingEvaluationMapper evaluationMapper;
    @Autowired private TermMapper termMapper;

    /**
     * 课表首页：查询已发布版本。
     * ★ 如果 classId 为空，自动从当前登录学生获取所属班级 ID，
     *   学生登录后无需手动输入班级ID即可查看自己班级的课表。
     */
    @GetMapping("/schedule")
    public Result<List<Map<String, Object>>> schedule(@RequestParam(required = false) Long classId,
                                                      @RequestParam(required = false) Long teacherId,
                                                      @RequestParam(required = false) Long termId) {
        // ★ 自动获取当前登录学生的 classId
        if (classId == null) {
            try {
                UserContext.LoginUser loginUser = UserContext.get();
                if (loginUser != null && "student".equals(loginUser.getRole())) {
                    User user = userMapper.selectById(loginUser.getUserId());
                    if (user != null && user.getClassId() != null) {
                        classId = user.getClassId();
                    }
                }
            } catch (Exception ignore) {
            }
        }
        List<ScheduleVersion> versions = versionMapper.selectByTerm(termId);
        ScheduleVersion published = versions.stream()
                .filter(v -> ScheduleConstants.VERSION_PUBLISHED.equals(v.getStatus()))
                .findFirst().orElse(null);
        if (published == null) {
            return Result.success(Collections.emptyList());
        }
        List<Map<String, Object>> details = versionDetailMapper.selectByVersion(published.getId());
        // ★ final 局部变量，供 lambda 捕获（classId 在上方 if 块中可能被重新赋值，不符合 effectively final）
        final Long _classId = classId;
        final Long _teacherId = teacherId;
        List<Map<String, Object>> filtered = details.stream()
                .filter(d -> _classId == null || _classId.equals(d.get("classId")))
                .filter(d -> _teacherId == null || _teacherId.equals(d.get("teacherId")))
                .collect(Collectors.toList());
        return Result.success(filtered);
    }

    /** 学生个人信息（含班级名称） */
    @GetMapping("/my-info")
    public Result<Map<String, Object>> myInfo() {
        UserContext.LoginUser loginUser = UserContext.require();
        User user = userMapper.selectById(loginUser.getUserId());
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", user.getId());
        info.put("username", user.getUsername());
        info.put("realName", user.getRealName());
        info.put("role", user.getRole());
        info.put("classId", user.getClassId());
        return Result.success(info);
    }

    @GetMapping("/announcement")
    public Result<List<Announcement>> announcement() {
        return Result.success(announcementMapper.selectList(null, null, null).stream()
                .filter(a -> a.getStatus() != null && a.getStatus() == 1)
                .collect(Collectors.toList()));
    }

    /**
     * 提交评教：后端强制赋值 evaluatorId / evaluatorType / termId / submitTime
     */
    @PostMapping("/evaluation")
    public Result<Void> submitEvaluation(@RequestBody TeachingEvaluation evaluation) {
        UserContext.LoginUser loginUser = UserContext.require();
        // 后端赋值，覆盖前端传入
        evaluation.setEvaluatorId(loginUser.getUserId());
        evaluation.setEvaluatorType("STUDENT");
        evaluation.setTermId(defaultTermId());
        evaluation.setOverallScore(calcOverall(evaluation));
        evaluationMapper.insert(evaluation);
        return Result.success();
    }

    private Long defaultTermId() {
        return termMapper.selectList(null).stream().findFirst()
                .map(t -> t.getId()).orElseThrow(() -> new BusinessException("未配置学期"));
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
