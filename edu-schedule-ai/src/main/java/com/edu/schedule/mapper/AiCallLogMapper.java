package com.edu.schedule.mapper;

import com.edu.schedule.entity.AiCallLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiCallLogMapper {

    @Select("<script>" +
            "SELECT * FROM ai_call_log WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (model_code LIKE CONCAT('%',#{keyword},'%') OR prompt LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='taskId != null'> AND task_id = #{taskId}</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<AiCallLog> selectList(@Param("keyword") String keyword, @Param("taskId") Long taskId);

    @Select("SELECT * FROM ai_call_log WHERE id = #{id}")
    AiCallLog selectById(Long id);

    @Insert("INSERT INTO ai_call_log(task_id,model_code,model,prompt,response,input_tokens,output_tokens,total_tokens,duration_ms,status,error_msg,call_time) " +
            "VALUES(#{taskId},#{modelCode},#{model},#{prompt},#{response},#{inputTokens},#{outputTokens},#{totalTokens},#{durationMs},#{status},#{errorMsg},NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiCallLog log);

    @Select("SELECT COUNT(*) FROM ai_call_log")
    long countAll();

    @Select("SELECT COUNT(*) FROM ai_call_log WHERE status = 'FAIL'")
    long countFail();
}
