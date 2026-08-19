package com.edu.schedule.mapper;

import com.edu.schedule.entity.ScheduleTask;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ScheduleTaskMapper {

    @Select("<script>" +
            "SELECT t.*, tm.term_name AS termName FROM schedule_task t " +
            "LEFT JOIN edu_term tm ON tm.id = t.term_id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND t.task_name LIKE CONCAT('%',#{keyword},'%')" +
            "</if>" +
            " ORDER BY t.id DESC" +
            "</script>")
    List<ScheduleTask> selectList(@Param("keyword") String keyword);

    @Select("SELECT t.*, tm.term_name AS termName FROM schedule_task t " +
            "LEFT JOIN edu_term tm ON tm.id = t.term_id WHERE t.id = #{id}")
    ScheduleTask selectById(Long id);

    @Insert("INSERT INTO schedule_task(task_name,term_id,model_config_id,prompt_template_code,objective,course_scope,rule_strategy,status,result,total_rounds,total_courses,success_courses,create_time,update_time) " +
            "VALUES(#{taskName},#{termId},#{modelConfigId},#{promptTemplateCode},#{objective},#{courseScope},#{ruleStrategy},#{status},#{result},#{totalRounds},#{totalCourses},#{successCourses},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleTask task);

    @Update("<script>" +
            "UPDATE schedule_task SET update_time=NOW()" +
            "<if test='taskName != null'>,task_name=#{taskName}</if>" +
            "<if test='termId != null'>,term_id=#{termId}</if>" +
            "<if test='modelConfigId != null'>,model_config_id=#{modelConfigId}</if>" +
            "<if test='promptTemplateCode != null'>,prompt_template_code=#{promptTemplateCode}</if>" +
            "<if test='objective != null'>,objective=#{objective}</if>" +
            "<if test='courseScope != null'>,course_scope=#{courseScope}</if>" +
            "<if test='ruleStrategy != null'>,rule_strategy=#{ruleStrategy}</if>" +
            "<if test='status != null'>,status=#{status}</if>" +
            "<if test='result != null'>,result=#{result}</if>" +
            "<if test='totalRounds != null'>,total_rounds=#{totalRounds}</if>" +
            "<if test='totalCourses != null'>,total_courses=#{totalCourses}</if>" +
            "<if test='successCourses != null'>,success_courses=#{successCourses}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(ScheduleTask task);

    @Update("UPDATE schedule_task SET status=#{status}, result=#{result}, total_rounds=#{totalRounds}, success_courses=#{successCourses}, update_time=NOW() WHERE id=#{id}")
    int updateResult(@Param("id") Long id, @Param("status") String status, @Param("result") String result,
                     @Param("totalRounds") Integer totalRounds, @Param("successCourses") Integer successCourses);

    @Delete("DELETE FROM schedule_task WHERE id = #{id}")
    int deleteById(Long id);

    @Select("SELECT COUNT(*) FROM schedule_task")
    long countAll();

    @Select("SELECT COUNT(*) FROM schedule_task WHERE status = 'SUCCESS'")
    long countSuccess();
}
