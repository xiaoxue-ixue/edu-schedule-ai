package com.edu.schedule.mapper;

import com.edu.schedule.entity.ScheduleDetail;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleDetailMapper {

    @Select("<script>" +
            "SELECT d.*, c.course_name AS courseName, t.name AS teacherName, " +
            "cl.class_name AS className, r.room_name AS roomName, s.slot_name AS slotName, s.start_time AS startTime, s.end_time AS endTime " +
            "FROM schedule_detail d " +
            "LEFT JOIN edu_course c ON c.id = d.course_id " +
            "LEFT JOIN edu_teacher t ON t.id = d.teacher_id " +
            "LEFT JOIN edu_clazz cl ON cl.id = d.class_id " +
            "LEFT JOIN edu_room r ON r.id = d.room_id " +
            "LEFT JOIN edu_time_slot s ON s.slot_no = d.slot_id " +
            "WHERE 1=1 " +
            "<if test='taskId != null'> AND d.task_id = #{taskId}</if>" +
            "<if test='termId != null'> AND d.term_id = #{termId}</if>" +
            "<if test='teacherId != null'> AND d.teacher_id = #{teacherId}</if>" +
            "<if test='classId != null'> AND d.class_id = #{classId}</if>" +
            " ORDER BY d.day_of_week ASC, d.slot_id ASC" +
            "</script>")
    List<ScheduleDetail> selectByCondition(@Param("taskId") Long taskId, @Param("termId") Long termId,
                                           @Param("teacherId") Long teacherId, @Param("classId") Long classId);

    @Select("SELECT * FROM schedule_detail WHERE task_id = #{taskId} ORDER BY day_of_week ASC, slot_id ASC")
    List<ScheduleDetail> selectListByTask(Long taskId);

    @Delete("DELETE FROM schedule_detail")
    int clearAll();

    @Select("SELECT d.*, c.course_name AS courseName, t.name AS teacherName, " +
            "cl.class_name AS className, r.room_name AS roomName, s.slot_name AS slotName, s.start_time AS startTime, s.end_time AS endTime " +
            "FROM schedule_detail d " +
            "LEFT JOIN edu_course c ON c.id = d.course_id " +
            "LEFT JOIN edu_teacher t ON t.id = d.teacher_id " +
            "LEFT JOIN edu_clazz cl ON cl.id = d.class_id " +
            "LEFT JOIN edu_room r ON r.id = d.room_id " +
            "LEFT JOIN edu_time_slot s ON s.slot_no = d.slot_id " +
            "WHERE d.term_id = #{termId} AND d.teacher_id = #{teacherId} " +
            "ORDER BY d.day_of_week ASC, d.slot_id ASC")
    List<ScheduleDetail> selectByTeacherAndTerm(@Param("teacherId") Long teacherId, @Param("termId") Long termId);

    @Select("SELECT d.*, c.course_name AS courseName, t.name AS teacherName, " +
            "cl.class_name AS className, r.room_name AS roomName, s.slot_name AS slotName, s.start_time AS startTime, s.end_time AS endTime " +
            "FROM schedule_detail d " +
            "LEFT JOIN edu_course c ON c.id = d.course_id " +
            "LEFT JOIN edu_teacher t ON t.id = d.teacher_id " +
            "LEFT JOIN edu_clazz cl ON cl.id = d.class_id " +
            "LEFT JOIN edu_room r ON r.id = d.room_id " +
            "LEFT JOIN edu_time_slot s ON s.slot_no = d.slot_id " +
            "WHERE d.term_id = #{termId} AND d.class_id = #{classId} " +
            "ORDER BY d.day_of_week ASC, d.slot_id ASC")
    List<ScheduleDetail> selectByClassAndTerm(@Param("classId") Long classId, @Param("termId") Long termId);

    @Select("SELECT * FROM schedule_detail WHERE id = #{id}")
    ScheduleDetail selectById(Long id);

    @Insert("INSERT INTO schedule_detail(task_id,term_id,course_id,teacher_id,class_id,room_id,day_of_week,slot_id,week_pattern,source,conflict_flag,conflict_reason,status,create_time,update_time) " +
            "VALUES(#{taskId},#{termId},#{courseId},#{teacherId},#{classId},#{roomId},#{dayOfWeek},#{slotId},#{weekPattern},#{source},#{conflictFlag},#{conflictReason},#{status},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleDetail detail);

    @Update("<script>" +
            "UPDATE schedule_detail SET update_time=NOW()" +
            "<if test='courseId != null'>,course_id=#{courseId}</if>" +
            "<if test='teacherId != null'>,teacher_id=#{teacherId}</if>" +
            "<if test='classId != null'>,class_id=#{classId}</if>" +
            "<if test='roomId != null'>,room_id=#{roomId}</if>" +
            "<if test='dayOfWeek != null'>,day_of_week=#{dayOfWeek}</if>" +
            "<if test='slotId != null'>,slot_id=#{slotId}</if>" +
            "<if test='weekPattern != null'>,week_pattern=#{weekPattern}</if>" +
            "<if test='source != null'>,source=#{source}</if>" +
            "<if test='status != null'>,status=#{status}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(ScheduleDetail detail);

    @Update("UPDATE schedule_detail SET conflict_flag=#{flag}, conflict_reason=#{reason}, update_time=NOW() WHERE id=#{id}")
    int updateConflictFlag(@Param("id") Long id, @Param("flag") Integer flag, @Param("reason") String reason);

    @Delete("DELETE FROM schedule_detail WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM schedule_detail WHERE task_id = #{taskId}")
    int deleteByTask(Long taskId);

    // ===== 冲突检测：同学期同时间点是否已被占用（排除指定明细，编辑场景） =====

    @Select("<script>" +
            "SELECT COUNT(*) FROM schedule_detail WHERE term_id=#{termId} AND teacher_id=#{teacherId} AND day_of_week=#{dayOfWeek} AND slot_id=#{slotId} " +
            "<if test='excludeDetailId != null'> AND id &lt;&gt; #{excludeDetailId}</if>" +
            "</script>")
    int countTeacherTimeConflict(@Param("termId") Long termId, @Param("teacherId") Long teacherId,
                                 @Param("dayOfWeek") Integer dayOfWeek, @Param("slotId") Long slotId,
                                 @Param("excludeDetailId") Long excludeDetailId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM schedule_detail WHERE term_id=#{termId} AND class_id=#{classId} AND day_of_week=#{dayOfWeek} AND slot_id=#{slotId} " +
            "<if test='excludeDetailId != null'> AND id &lt;&gt; #{excludeDetailId}</if>" +
            "</script>")
    int countClassTimeConflict(@Param("termId") Long termId, @Param("classId") Long classId,
                               @Param("dayOfWeek") Integer dayOfWeek, @Param("slotId") Long slotId,
                               @Param("excludeDetailId") Long excludeDetailId);

    @Select("<script>" +
            "SELECT COUNT(*) FROM schedule_detail WHERE term_id=#{termId} AND room_id=#{roomId} AND day_of_week=#{dayOfWeek} AND slot_id=#{slotId} " +
            "<if test='excludeDetailId != null'> AND id &lt;&gt; #{excludeDetailId}</if>" +
            "</script>")
    int countRoomTimeConflict(@Param("termId") Long termId, @Param("roomId") Long roomId,
                              @Param("dayOfWeek") Integer dayOfWeek, @Param("slotId") Long slotId,
                              @Param("excludeDetailId") Long excludeDetailId);

    @Select("SELECT COUNT(*) FROM schedule_detail WHERE term_id = #{termId}")
    long countByTerm(Long termId);
}