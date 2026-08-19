package com.edu.schedule.mapper;

import com.edu.schedule.entity.User;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface UserMapper {

    @Select("SELECT * FROM sys_user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT * FROM sys_user WHERE id = #{id}")
    User selectById(Long id);

    @Select("<script>" +
            "SELECT u.*, c.class_name AS className, t.name AS teacherName FROM sys_user u " +
            "LEFT JOIN edu_clazz c ON u.class_id = c.id " +
            "LEFT JOIN edu_teacher t ON u.teacher_id = t.id " +
            "WHERE 1=1 " +
            "<if test='keyword != null and keyword != \"\"'>" +
            " AND (u.username LIKE CONCAT('%',#{keyword},'%') OR u.real_name LIKE CONCAT('%',#{keyword},'%'))" +
            "</if>" +
            "<if test='role != null and role != \"\"'>" +
            " AND u.role = #{role}" +
            "</if>" +
            " ORDER BY u.id DESC" +
            "</script>")
    List<User> selectList(@Param("keyword") String keyword, @Param("role") String role);

    @Insert("INSERT INTO sys_user(username,password,real_name,role,phone,email,teacher_id,class_id,status,create_time,update_time) " +
            "VALUES(#{username},#{password},#{realName},#{role},#{phone},#{email},#{teacherId},#{classId},#{status},NOW(),NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Update("<script>" +
            "UPDATE sys_user SET update_time=NOW()" +
            "<if test='realName != null'>,real_name=#{realName}</if>" +
            "<if test='role != null'>,role=#{role}</if>" +
            "<if test='phone != null'>,phone=#{phone}</if>" +
            "<if test='email != null'>,email=#{email}</if>" +
            "<if test='teacherId != null'>,teacher_id=#{teacherId}</if>" +
            "<if test='classId != null'>,class_id=#{classId}</if>" +
            "<if test='status != null'>,status=#{status}</if>" +
            "<if test='password != null and password != \"\"'>,password=#{password}</if>" +
            " WHERE id=#{id}" +
            "</script>")
    int update(User user);

    @Delete("DELETE FROM sys_user WHERE id = #{id}")
    int deleteById(Long id);

    @Update("UPDATE sys_user SET password=#{password}, update_time=NOW() WHERE id=#{id}")
    int updatePassword(@Param("id") Long id, @Param("password") String password);

    @Select("SELECT COUNT(*) FROM sys_user")
    long countAll();
}
