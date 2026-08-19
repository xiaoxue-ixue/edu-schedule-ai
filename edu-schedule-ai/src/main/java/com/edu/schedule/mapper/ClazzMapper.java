package com.edu.schedule.mapper;

import com.edu.schedule.entity.Clazz;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ClazzMapper {
    @Select("<script>SELECT * FROM edu_clazz WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND class_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<Clazz> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_clazz WHERE id=#{id}")
    Clazz selectById(Long id);

    @Insert("INSERT INTO edu_clazz(class_code, class_name, grade, student_count, campus_id, create_time) VALUES(#{classCode}, #{className}, #{grade}, #{studentCount}, #{campusId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Clazz entity);

    @Update("<script>UPDATE edu_clazz SET update_time=NOW()<if test='classCode!=null'>, class_code=#{classCode}</if><if test='className!=null'>, class_name=#{className}</if><if test='grade!=null'>, grade=#{grade}</if><if test='studentCount!=null'>, student_count=#{studentCount}</if><if test='campusId!=null'>, campus_id=#{campusId}</if> WHERE id=#{id}</script>")
    int update(Clazz entity);

    @Delete("DELETE FROM edu_clazz WHERE id=#{id}")
    int deleteById(Long id);
}
