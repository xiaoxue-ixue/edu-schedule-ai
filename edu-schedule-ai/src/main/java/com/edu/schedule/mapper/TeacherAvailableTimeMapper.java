package com.edu.schedule.mapper;

import com.edu.schedule.entity.TeacherAvailableTime;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TeacherAvailableTimeMapper {
    @Select("SELECT * FROM edu_teacher_available_time WHERE (#{teacherId} IS NULL OR teacher_id = #{teacherId}) ORDER BY id DESC")
    List<TeacherAvailableTime> selectList(@Param("teacherId") Long teacherId);

    @Select("SELECT * FROM edu_teacher_available_time WHERE id=#{id}")
    TeacherAvailableTime selectById(Long id);

    @Insert("INSERT INTO edu_teacher_available_time(teacher_id, day_of_week, slot_id, create_time) VALUES(#{teacherId}, #{dayOfWeek}, #{slotId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TeacherAvailableTime entity);

    @Delete("DELETE FROM edu_teacher_available_time WHERE id=#{id}")
    int deleteById(Long id);
}
