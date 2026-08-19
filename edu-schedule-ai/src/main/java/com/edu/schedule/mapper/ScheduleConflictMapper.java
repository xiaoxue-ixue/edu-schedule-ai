package com.edu.schedule.mapper;

import com.edu.schedule.entity.ScheduleConflict;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleConflictMapper {

    @Select("<script>" +
            "SELECT * FROM schedule_conflict WHERE 1=1 " +
            "<if test='taskId != null'> AND task_id = #{taskId}</if>" +
            "<if test='termId != null'> AND term_id = #{termId}</if>" +
            "<if test='status != null and status != \"\"'> AND status = #{status}</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<ScheduleConflict> selectByCondition(@Param("taskId") Long taskId, @Param("termId") Long termId, @Param("status") String status);

    @Select("SELECT * FROM schedule_conflict WHERE task_id = #{taskId} ORDER BY id DESC")
    List<ScheduleConflict> selectListByTask(Long taskId);

    @Select("SELECT * FROM schedule_conflict WHERE id = #{id}")
    ScheduleConflict selectById(Long id);

    @Insert("INSERT INTO schedule_conflict(detail_id,task_id,term_id,conflict_type,description,status,create_time) " +
            "VALUES(#{detailId},#{taskId},#{termId},#{conflictType},#{description},#{status},NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleConflict conflict);

    @Update("UPDATE schedule_conflict SET status=#{status}, handle_method=#{handleMethod}, handle_time=NOW() WHERE id=#{id}")
    int updateHandle(@Param("id") Long id, @Param("status") String status, @Param("handleMethod") String handleMethod);

    @Delete("DELETE FROM schedule_conflict WHERE task_id = #{taskId}")
    int deleteByTask(Long taskId);

    @Delete("DELETE FROM schedule_conflict WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM schedule_conflict WHERE status = 'PENDING'")
    long countPending();
}
