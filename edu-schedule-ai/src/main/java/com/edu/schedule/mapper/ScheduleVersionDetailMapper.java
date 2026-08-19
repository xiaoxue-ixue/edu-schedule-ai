package com.edu.schedule.mapper;

import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface ScheduleVersionDetailMapper {

    @Insert("INSERT INTO schedule_version_detail(version_id,course_id,teacher_id,class_id,room_id,day_of_week,slot_id,week_pattern) " +
            "VALUES(#{versionId},#{courseId},#{teacherId},#{classId},#{roomId},#{dayOfWeek},#{slotId},#{weekPattern})")
    int insert(Map<String, Object> detail);

    // ✅ 去掉不存在的 section_start/section_end，用 slot_name 显示节次
    @Select("SELECT d.id, " +
            " d.class_id AS classId, " +
            " d.teacher_id AS teacherId, " +
            " d.course_id AS courseId, " +
            " d.room_id AS roomId, " +
            " d.day_of_week AS weekday, " +
            " d.slot_id AS slotId, " +
            " d.week_pattern AS weekPattern, " +
            " COALESCE(s.slot_name, CONCAT('第', d.slot_id, '节')) AS sectionStart, " +
            " COALESCE(s.slot_name, CONCAT('第', d.slot_id, '节')) AS slotName, " +
            " '' AS sectionEnd, " +
            " CASE WHEN d.week_pattern='EVERY' OR d.week_pattern IS NULL THEN 'ALL' WHEN d.week_pattern='ODD' THEN 'ODD' ELSE 'EVEN' END AS weekType, " +
            " c.course_name AS courseName, " +
            " tc.name AS teacherName, " +
            " cl.class_name AS className, " +
            " r.room_name AS roomName, " +
            " cam.campus_name AS campusName, s.start_time AS startTime, s.end_time AS endTime " +
            " FROM schedule_version_detail d " +
            " LEFT JOIN edu_course c ON d.course_id=c.id " +
            " LEFT JOIN edu_teacher tc ON d.teacher_id=tc.id " +
            " LEFT JOIN edu_clazz cl ON d.class_id=cl.id " +
            " LEFT JOIN edu_room r ON d.room_id=r.id " +
            " LEFT JOIN edu_campus cam ON r.campus_id=cam.id " +
            " LEFT JOIN edu_time_slot s ON s.slot_no = d.slot_id " +
            " WHERE d.version_id = #{versionId} " +
            " ORDER BY d.day_of_week ASC, d.slot_id ASC")
    List<Map<String, Object>> selectByVersion(Long versionId);

    @Delete("DELETE FROM schedule_version_detail WHERE version_id = #{versionId}")
    int deleteByVersion(Long versionId);
}