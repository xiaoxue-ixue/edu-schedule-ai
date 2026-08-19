package com.edu.schedule.mapper;

import com.edu.schedule.entity.Course;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CourseMapper {
    @Select("<script>SELECT * FROM edu_course WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND course_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<Course> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_course WHERE id=#{id}")
    Course selectById(Long id);

    @Insert("INSERT INTO edu_course(course_code, course_name, credit, hours, teacher_id, create_time) VALUES(#{courseCode}, #{courseName}, #{credit}, #{hours}, #{teacherId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Course entity);

    @Update("<script>UPDATE edu_course SET update_time=NOW()<if test='courseCode!=null'>, course_code=#{courseCode}</if><if test='courseName!=null'>, course_name=#{courseName}</if><if test='credit!=null'>, credit=#{credit}</if><if test='hours!=null'>, hours=#{hours}</if><if test='teacherId!=null'>, teacher_id=#{teacherId}</if> WHERE id=#{id}</script>")
    int update(Course entity);

    @Delete("DELETE FROM edu_course WHERE id=#{id}")
    int deleteById(Long id);

    @Select("<script>SELECT * FROM edu_course WHERE id IN " +
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>" +
            "</script>")
    List<Course> selectByIds(@Param("ids") List<Long> ids);
}
