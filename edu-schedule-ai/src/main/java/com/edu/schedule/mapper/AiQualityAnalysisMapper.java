package com.edu.schedule.mapper;

import com.edu.schedule.entity.AiQualityAnalysis;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AiQualityAnalysisMapper {

    @Select("<script>" +
            "SELECT * FROM ai_quality_analysis WHERE 1=1 " +
            "<if test='termId != null'> AND term_id = #{termId}</if>" +
            "<if test='dimension != null and dimension != \"\"'> AND dimension = #{dimension}</if>" +
            "<if test='qualityLevel != null and qualityLevel != \"\"'> AND quality_level = #{qualityLevel}</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<AiQualityAnalysis> selectByCondition(@Param("termId") Long termId, @Param("dimension") String dimension, @Param("qualityLevel") String qualityLevel);

    @Select("SELECT * FROM ai_quality_analysis WHERE id = #{id}")
    AiQualityAnalysis selectById(Long id);

    @Insert("INSERT INTO ai_quality_analysis(term_id,dimension,target_id,target_name,eval_count,avg_score,quality_level,summary,strengths,problems,suggestions,report,create_time) " +
            "VALUES(#{termId},#{dimension},#{targetId},#{targetName},#{evalCount},#{avgScore},#{qualityLevel},#{summary},#{strengths},#{problems},#{suggestions},#{report},NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AiQualityAnalysis analysis);

    @Delete("DELETE FROM ai_quality_analysis WHERE id = #{id}")
    int deleteById(Long id);
}