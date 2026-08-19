package com.edu.schedule.mapper;

import com.edu.schedule.entity.RoomAvailableTime;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface RoomAvailableTimeMapper {
    @Select("SELECT * FROM edu_room_available_time WHERE room_id=#{roomId} ORDER BY id DESC")
    List<RoomAvailableTime> selectList(@Param("roomId") Long roomId);

    @Select("SELECT * FROM edu_room_available_time WHERE id=#{id}")
    RoomAvailableTime selectById(Long id);

    @Insert("INSERT INTO edu_room_available_time(room_id, day_of_week, slot_id, create_time) VALUES(#{roomId}, #{dayOfWeek}, #{slotId}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RoomAvailableTime entity);

    @Delete("DELETE FROM edu_room_available_time WHERE id=#{id}")
    int deleteById(Long id);
}
