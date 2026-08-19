package com.edu.schedule.mapper;

import com.edu.schedule.entity.Knowledge;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface KnowledgeMapper {

    @Select("<script>" +
            "SELECT * FROM education_knowledge WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (title LIKE CONCAT('%',#{keyword},'%') OR category LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            " ORDER BY id DESC" +
            "</script>")
    List<Knowledge> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM education_knowledge WHERE id = #{id}")
    Knowledge selectById(Long id);

    @Select("SELECT * FROM education_knowledge WHERE status = 1")
    List<Knowledge> selectAllEnabled();

    @Insert("INSERT INTO education_knowledge(title,category,content,slice_content,slice_count,status,create_time,update_time) " +
            "VALUES(#{title},#{category},#{content},#{sliceContent},#{sliceCount},#{status},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Knowledge knowledge);

    @Update("<script>" +
            "UPDATE education_knowledge SET update_time=NOW()" +
            "<if test='title != null'>,title=#{title}</if>" +
            "<if test='category != null'>,category=#{category}</if>" +
            "<if test='content != null'>,content=#{content}</if>" +
            "<if test='sliceContent != null'>,slice_content=#{sliceContent}</if>" +
            "<if test='sliceCount != null'>,slice_count=#{sliceCount}</if>" +
            "<if test='status != null'>,status=#{status}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(Knowledge knowledge);

    @Delete("DELETE FROM education_knowledge WHERE id = #{id}")
    int deleteById(Long id);
}
