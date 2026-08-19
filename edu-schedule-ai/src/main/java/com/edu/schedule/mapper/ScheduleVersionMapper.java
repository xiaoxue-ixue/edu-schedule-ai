package com.edu.schedule.mapper;

import com.edu.schedule.entity.ScheduleVersion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleVersionMapper {

    @Select("<script>" +
            "SELECT * FROM schedule_version WHERE 1=1 " +
            "<if test='termId != null'> AND term_id = #{termId}</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<ScheduleVersion> selectByTerm(Long termId);

    @Select("SELECT * FROM schedule_version ORDER BY id DESC")
    List<ScheduleVersion> selectAll();

    @Select("SELECT * FROM schedule_version WHERE id = #{id}")
    ScheduleVersion selectById(Long id);

    @Insert("INSERT INTO schedule_version(version_name,term_id,task_id,status,publish_time,remark,create_time,update_time) " +
            "VALUES(#{versionName},#{termId},#{taskId},#{status},NOW(),#{remark},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleVersion version);

    @Update("UPDATE schedule_version SET status=#{status}, update_time=NOW() WHERE id=#{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);

    // ✅ 新增：updateById，给 Controller 的 update() 用
    @Update("<script>" +
            "UPDATE schedule_version SET update_time=NOW()" +
            "<if test='versionName != null'>, version_name = #{versionName}</if>" +
            "<if test='termId != null'>, term_id = #{termId}</if>" +
            "<if test='taskId != null'>, task_id = #{taskId}</if>" +
            "<if test='status != null'>, status = #{status}</if>" +
            "<if test='remark != null'>, remark = #{remark}</if>" +
            "<if test='status == \"PUBLISHED\"'>, publish_time = NOW()</if>" +
            " WHERE id = #{id}" +
            "</script>")
    int updateById(ScheduleVersion version);

    @Delete("DELETE FROM schedule_version WHERE id = #{id}")
    int deleteById(Long id);
}