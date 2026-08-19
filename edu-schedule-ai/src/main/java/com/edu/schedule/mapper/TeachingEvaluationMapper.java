package com.edu.schedule.mapper;

import com.edu.schedule.entity.TeachingEvaluation;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TeachingEvaluationMapper {

    @Select("<script>" +
            "SELECT t.*, " +
            "  c.course_name  AS courseName, " +
            "  tc.name        AS teacherName, " +
            "  cl.class_name  AS className, " +
            "  tm.term_name   AS termName " +
            "FROM teaching_evaluation t " +
            "LEFT JOIN edu_course  c  ON t.course_id  = c.id " +
            "LEFT JOIN edu_teacher tc ON t.teacher_id = tc.id " +
            "LEFT JOIN edu_clazz   cl ON t.class_id   = cl.id " +
            "LEFT JOIN edu_term    tm ON t.term_id    = tm.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            "  AND (t.feedback LIKE CONCAT('%',#{keyword},'%') OR c.course_name LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='teacherId != null'> AND t.teacher_id = #{teacherId}</if>" +
            "<if test='courseId != null'> AND t.course_id = #{courseId}</if>" +
            "<if test='classId != null'> AND t.class_id = #{classId}</if>" +
            "<if test='termId != null'> AND t.term_id = #{termId}</if>" +
            "<if test='evaluatorType != null and evaluatorType != \"\"'> AND t.evaluator_type = #{evaluatorType}</if>" +
            " ORDER BY t.id DESC" +
            "</script>")
    List<TeachingEvaluation> selectByCondition(@Param("keyword") String keyword,
                                               @Param("teacherId") Long teacherId,
                                               @Param("courseId") Long courseId,
                                               @Param("classId") Long classId,
                                               @Param("termId") Long termId,
                                               @Param("evaluatorType") String evaluatorType);

    @Select("SELECT t.*, c.course_name AS courseName, tc.name AS teacherName, " +
            "cl.class_name AS className, tm.term_name AS termName " +
            "FROM teaching_evaluation t " +
            "LEFT JOIN edu_course c ON t.course_id = c.id " +
            "LEFT JOIN edu_teacher tc ON t.teacher_id = tc.id " +
            "LEFT JOIN edu_clazz cl ON t.class_id = cl.id " +
            "LEFT JOIN edu_term tm ON t.term_id = tm.id " +
            "WHERE t.teacher_id = #{teacherId} ORDER BY t.id DESC")
    List<TeachingEvaluation> selectByTeacher(Long teacherId);

    @Select("SELECT * FROM teaching_evaluation WHERE term_id = #{termId}")
    List<TeachingEvaluation> selectByTerm(Long termId);

    @Insert("INSERT INTO teaching_evaluation(course_id,teacher_id,class_id,term_id,evaluator_id,evaluator_type," +
            "dim_attitude,dim_content,dim_method,dim_effect,overall_score,feedback,suggestion,submit_time) " +
            "VALUES(#{courseId},#{teacherId},#{classId},#{termId},#{evaluatorId},#{evaluatorType}," +
            "#{dimAttitude},#{dimContent},#{dimMethod},#{dimEffect},#{overallScore},#{feedback},#{suggestion},NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TeachingEvaluation evaluation);

    @Select("SELECT COUNT(*) FROM teaching_evaluation")
    long countAll();

    @Delete("DELETE FROM teaching_evaluation WHERE id = #{id}")
    int deleteById(Long id);
}