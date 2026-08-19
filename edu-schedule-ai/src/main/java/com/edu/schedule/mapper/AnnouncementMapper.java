package com.edu.schedule.mapper;

import com.edu.schedule.entity.Announcement;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface AnnouncementMapper {

    @Select("<script>" +
            "SELECT * FROM announcement WHERE 1=1 " +
            "<if test='keyword!=null and keyword!=\"\"'> AND (title LIKE CONCAT('%',#{keyword},'%') OR content LIKE CONCAT('%',#{keyword},'%'))</if> " +
            "<if test='type!=null and type!=\"\"'> AND type = #{type}</if> " +
            "<if test='status!=null'> AND status = #{status}</if> " +
            "ORDER BY CASE priority WHEN 'HIGH' THEN 0 WHEN 'MEDIUM' THEN 1 ELSE 2 END, id DESC" +
            "</script>")
    List<Announcement> selectList(@Param("keyword") String keyword,
                                  @Param("type") String type,
                                  @Param("status") Integer status);

    @Select("SELECT * FROM announcement WHERE id=#{id}")
    Announcement selectById(Long id);

    @Insert("INSERT INTO announcement(title, content, status, publish_time, type, priority, create_by, create_time) " +
            "VALUES(#{title}, #{content}, #{status}, #{publishTime}, #{type}, #{priority}, #{createBy}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Announcement entity);

    @Update("<script>" +
            "UPDATE announcement SET update_time=NOW()" +
            "<if test='title!=null'>, title=#{title}</if>" +
            "<if test='content!=null'>, content=#{content}</if>" +
            "<if test='status!=null'>, status=#{status}</if>" +
            "<if test='publishTime!=null'>, publish_time=#{publishTime}</if>" +
            "<if test='type!=null'>, type=#{type}</if>" +
            "<if test='priority!=null'>, priority=#{priority}</if>" +
            "<if test='createBy!=null'>, create_by=#{createBy}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(Announcement entity);

    @Delete("DELETE FROM announcement WHERE id=#{id}")
    int deleteById(Long id);
}