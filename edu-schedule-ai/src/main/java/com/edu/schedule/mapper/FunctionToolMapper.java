package com.edu.schedule.mapper;

import com.edu.schedule.entity.FunctionTool;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface FunctionToolMapper {
    @Select("<script>SELECT * FROM function_tool WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND tool_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<FunctionTool> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM function_tool WHERE id=#{id}")
    FunctionTool selectById(Long id);

    @Insert("INSERT INTO function_tool(tool_code, tool_name, description, category, enabled, create_time) VALUES(#{toolCode}, #{toolName}, #{description}, #{category}, #{enabled}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(FunctionTool entity);

    @Update("<script>UPDATE function_tool SET update_time=NOW()<if test='toolCode!=null'>, tool_code=#{toolCode}</if><if test='toolName!=null'>, tool_name=#{toolName}</if><if test='description!=null'>, description=#{description}</if><if test='category!=null'>, category=#{category}</if><if test='enabled!=null'>, enabled=#{enabled}</if> WHERE id=#{id}</script>")
    int update(FunctionTool entity);

    @Delete("DELETE FROM function_tool WHERE id=#{id}")
    int deleteById(Long id);
}
