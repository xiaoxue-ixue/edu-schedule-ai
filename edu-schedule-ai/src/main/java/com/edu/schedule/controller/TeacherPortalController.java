package com.edu.schedule.controller;

import com.edu.schedule.common.BusinessException;
import com.edu.schedule.common.Result;
import com.edu.schedule.entity.ScheduleDetail;
import com.edu.schedule.entity.TeacherAvailableTime;
import com.edu.schedule.entity.TeachingEvaluation;
import com.edu.schedule.entity.User;
import com.edu.schedule.mapper.ScheduleDetailMapper;
import com.edu.schedule.mapper.TeacherAvailableTimeMapper;
import com.edu.schedule.mapper.TeachingEvaluationMapper;
import com.edu.schedule.mapper.TermMapper;
import com.edu.schedule.mapper.UserMapper;
import com.edu.schedule.utils.UserContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/**
 * 教师端：我的课表 / 我收到的评价 / 我的可用时间
 */
@RestController
@RequestMapping("/api/teacher-portal")
public class TeacherPortalController {

    @Autowired private UserMapper userMapper;
    @Autowired private ScheduleDetailMapper detailMapper;
    @Autowired private TeachingEvaluationMapper evaluationMapper;
    @Autowired private TeacherAvailableTimeMapper availableMapper;
    @Autowired private TermMapper termMapper;

    @GetMapping("/my-schedule")
    public Result<List<ScheduleDetail>> mySchedule(@RequestParam(required = false) Long termId) {
        Long teacherId = currentTeacherId();
        Long t = termId != null ? termId : defaultTermId();
        return Result.success(detailMapper.selectByCondition(null, t, teacherId, null));
    }

    @GetMapping("/my-evaluation")
    public Result<Map<String, Object>> myEvaluation() {
        Long teacherId = currentTeacherId();
        List<TeachingEvaluation> list = evaluationMapper.selectByTeacher(teacherId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", list);
        result.put("count", list.size());
        result.put("avgAttitude", avg(list.stream().map(TeachingEvaluation::getDimAttitude).toList()));
        result.put("avgContent", avg(list.stream().map(TeachingEvaluation::getDimContent).toList()));
        result.put("avgMethod", avg(list.stream().map(TeachingEvaluation::getDimMethod).toList()));
        result.put("avgEffect", avg(list.stream().map(TeachingEvaluation::getDimEffect).toList()));
        result.put("avgOverall", avg(list.stream().map(TeachingEvaluation::getOverallScore).toList()));
        return Result.success(result);
    }

    @GetMapping("/my-available-time")
    public Result<List<TeacherAvailableTime>> myAvailableTime() {
        Long teacherId = currentTeacherId();
        return Result.success(availableMapper.selectList(teacherId));
    }

    private Long currentTeacherId() {
        UserContext.LoginUser loginUser = UserContext.require();
        User user = userMapper.selectById(loginUser.getUserId());
        if (user == null || user.getTeacherId() == null) {
            throw new BusinessException("当前账号未关联教师信息");
        }
        return user.getTeacherId();
    }

    private Long defaultTermId() {
        return termMapper.selectList(null).stream().findFirst()
                .map(t -> t.getId()).orElseThrow(() -> new BusinessException("未配置学期"));
    }

    private BigDecimal avg(List<BigDecimal> list) {
        List<BigDecimal> nonNull = list.stream().filter(Objects::nonNull).toList();
        if (nonNull.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal sum = nonNull.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        return sum.divide(BigDecimal.valueOf(nonNull.size()), 1, RoundingMode.HALF_UP);
    }
}
