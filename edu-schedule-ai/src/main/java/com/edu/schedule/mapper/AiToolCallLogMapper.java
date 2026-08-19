package com.edu.schedule.mapper;

import com.edu.schedule.entity.AiToolCallLog;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiToolCallLogMapper {

    @Insert("INSERT INTO ai_tool_call_log(call_log_id,task_id,tool_name,tool_args,tool_result,success,duration_ms,call_time) " +
            "VALUES(#{callLogId},#{taskId},#{toolName},#{toolArgs},#{toolResult},#{success},#{durationMs},NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiToolCallLog log);

    @Select("SELECT * FROM ai_tool_call_log WHERE call_log_id = #{callLogId} ORDER BY id ASC")
    List<AiToolCallLog> selectByCallLogId(Long callLogId);

    @Select("SELECT * FROM ai_tool_call_log WHERE task_id = #{taskId} ORDER BY id ASC")
    List<AiToolCallLog> selectByTaskId(Long taskId);

    @Select("SELECT COUNT(*) FROM ai_tool_call_log WHERE task_id = #{taskId}")
    int countByTaskId(Long taskId);
}
