package com.edu.schedule.mapper;

import com.edu.schedule.entity.AiModelConfig;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiModelConfigMapper {

    @Select("<script>" +
            "SELECT * FROM ai_model_config WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (model_code LIKE CONCAT('%',#{keyword},'%') OR model_name LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            " ORDER BY is_default DESC, id DESC" +
            "</script>")
    List<AiModelConfig> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM ai_model_config WHERE id = #{id}")
    AiModelConfig selectById(Long id);

    @Select("SELECT * FROM ai_model_config WHERE model_code = #{modelCode}")
    AiModelConfig selectByCode(String modelCode);

    @Select("SELECT * FROM ai_model_config WHERE is_default = 1 AND enabled = 1 LIMIT 1")
    AiModelConfig selectDefault();

    @Select("SELECT * FROM ai_model_config WHERE enabled = 1 ORDER BY is_default DESC, id DESC")
    List<AiModelConfig> selectAllEnabled();

    @Insert("INSERT INTO ai_model_config(model_code,model_name,base_url,api_key,model,temperature,max_tokens,is_default,enabled,create_time,update_time) " +
            "VALUES(#{modelCode},#{modelName},#{baseUrl},#{apiKey},#{model},#{temperature},#{maxTokens},#{isDefault},#{enabled},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiModelConfig config);

    @Update("<script>" +
            "UPDATE ai_model_config SET update_time=NOW()" +
            "<if test='modelCode != null'>,model_code=#{modelCode}</if>" +
            "<if test='modelName != null'>,model_name=#{modelName}</if>" +
            "<if test='baseUrl != null'>,base_url=#{baseUrl}</if>" +
            "<if test='apiKey != null and apiKey != \"\"'>,api_key=#{apiKey}</if>" +
            "<if test='model != null'>,model=#{model}</if>" +
            "<if test='temperature != null'>,temperature=#{temperature}</if>" +
            "<if test='maxTokens != null'>,max_tokens=#{maxTokens}</if>" +
            "<if test='isDefault != null'>,is_default=#{isDefault}</if>" +
            "<if test='enabled != null'>,enabled=#{enabled}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(AiModelConfig config);

    @Delete("DELETE FROM ai_model_config WHERE id = #{id}")
    int deleteById(Long id);
}
