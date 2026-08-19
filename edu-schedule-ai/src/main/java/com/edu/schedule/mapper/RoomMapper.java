package com.edu.schedule.mapper;

import com.edu.schedule.entity.Room;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoomMapper {
    @Select("<script>SELECT * FROM edu_room WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND room_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<Room> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_room WHERE id=#{id}")
    Room selectById(Long id);

    @Insert("INSERT INTO edu_room(room_code, room_name, campus_id, capacity, room_type, create_time) VALUES(#{roomCode}, #{roomName}, #{campusId}, #{capacity}, #{roomType}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Room entity);

    @Update("<script>UPDATE edu_room SET update_time=NOW()<if test='roomCode!=null'>, room_code=#{roomCode}</if><if test='roomName!=null'>, room_name=#{roomName}</if><if test='campusId!=null'>, campus_id=#{campusId}</if><if test='capacity!=null'>, capacity=#{capacity}</if><if test='roomType!=null'>, room_type=#{roomType}</if> WHERE id=#{id}</script>")
    int update(Room entity);

    @Delete("DELETE FROM edu_room WHERE id=#{id}")
    int deleteById(Long id);
}
