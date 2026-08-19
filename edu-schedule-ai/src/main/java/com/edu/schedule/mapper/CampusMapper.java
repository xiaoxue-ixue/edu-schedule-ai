package com.edu.schedule.mapper;

import com.edu.schedule.entity.Campus;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface CampusMapper {
    @Select("<script>SELECT * FROM edu_campus WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND campus_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<Campus> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_campus WHERE id=#{id}")
    Campus selectById(Long id);

    @Insert("INSERT INTO edu_campus(campus_code, campus_name, address, create_time) VALUES(#{campusCode}, #{campusName}, #{address}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Campus entity);

    @Update("<script>UPDATE edu_campus SET update_time=NOW()<if test='campusCode!=null'>, campus_code=#{campusCode}</if><if test='campusName!=null'>, campus_name=#{campusName}</if><if test='address!=null'>, address=#{address}</if> WHERE id=#{id}</script>")
    int update(Campus entity);

    @Delete("DELETE FROM edu_campus WHERE id=#{id}")
    int deleteById(Long id);
}
