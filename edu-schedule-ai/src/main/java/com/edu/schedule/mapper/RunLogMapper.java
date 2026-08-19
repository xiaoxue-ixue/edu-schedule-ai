package com.edu.schedule.mapper;

import com.edu.schedule.entity.RunLog;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RunLogMapper {
    @Select("<script>SELECT * FROM run_log WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND content LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<RunLog> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM run_log WHERE id=#{id}")
    RunLog selectById(Long id);

    @Insert("INSERT INTO run_log(module, type, content, operator_id, create_time) VALUES(#{module}, #{type}, #{content}, #{operatorId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RunLog entity);
}
