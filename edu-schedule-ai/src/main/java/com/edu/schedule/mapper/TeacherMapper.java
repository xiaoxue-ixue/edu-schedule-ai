package com.edu.schedule.mapper;

import com.edu.schedule.entity.Teacher;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TeacherMapper {
    @Select("<script>SELECT * FROM edu_teacher WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<Teacher> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_teacher WHERE id=#{id}")
    Teacher selectById(Long id);

    @Insert("INSERT INTO edu_teacher(teacher_no, name, title, campus_id, phone, create_time) VALUES(#{teacherNo}, #{name}, #{title}, #{campusId}, #{phone}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Teacher entity);

    @Update("<script>UPDATE edu_teacher SET update_time=NOW()<if test='teacherNo!=null'>, teacher_no=#{teacherNo}</if><if test='name!=null'>, name=#{name}</if><if test='title!=null'>, title=#{title}</if><if test='campusId!=null'>, campus_id=#{campusId}</if><if test='phone!=null'>, phone=#{phone}</if> WHERE id=#{id}</script>")
    int update(Teacher entity);

    @Delete("DELETE FROM edu_teacher WHERE id=#{id}")
    int deleteById(Long id);
}
