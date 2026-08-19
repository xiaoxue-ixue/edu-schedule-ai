package com.edu.schedule.mapper;

import com.edu.schedule.entity.PromptTemplate;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface PromptTemplateMapper {

    @Select("<script>" +
            "SELECT * FROM ai_prompt_template WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (code LIKE CONCAT('%',#{keyword},'%') OR name LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<PromptTemplate> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM ai_prompt_template WHERE id = #{id}")
    PromptTemplate selectById(Long id);

    @Select("SELECT * FROM ai_prompt_template WHERE code = #{code}")
    PromptTemplate selectByCode(String code);

    @Insert("INSERT INTO ai_prompt_template(code,name,category,content,variables,enabled,create_time,update_time) " +
            "VALUES(#{code},#{name},#{category},#{content},#{variables},#{enabled},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(PromptTemplate template);

    @Update("<script>" +
            "UPDATE ai_prompt_template SET update_time=NOW()" +
            "<if test='code != null'>,code=#{code}</if>" +
            "<if test='name != null'>,name=#{name}</if>" +
            "<if test='category != null'>,category=#{category}</if>" +
            "<if test='content != null'>,content=#{content}</if>" +
            "<if test='variables != null'>,variables=#{variables}</if>" +
            "<if test='enabled != null'>,enabled=#{enabled}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(PromptTemplate template);

    @Delete("DELETE FROM ai_prompt_template WHERE id = #{id}")
    int deleteById(Long id);
}
