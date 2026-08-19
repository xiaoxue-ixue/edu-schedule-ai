package com.edu.schedule.mapper;

import com.edu.schedule.entity.TimeSlot;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TimeSlotMapper {
    @Select("<script>SELECT * FROM edu_time_slot WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND slot_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<TimeSlot> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_time_slot WHERE id=#{id}")
    TimeSlot selectById(Long id);

    @Insert("INSERT INTO edu_time_slot(slot_no, slot_name, start_time, end_time, create_time) VALUES(#{slotNo}, #{slotName}, #{startTime}, #{endTime}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(TimeSlot entity);

    @Update("<script>UPDATE edu_time_slot SET update_time=NOW()<if test='slotNo!=null'>, slot_no=#{slotNo}</if><if test='slotName!=null'>, slot_name=#{slotName}</if><if test='startTime!=null'>, start_time=#{startTime}</if><if test='endTime!=null'>, end_time=#{endTime}</if> WHERE id=#{id}</script>")
    int update(TimeSlot entity);

    @Delete("DELETE FROM edu_time_slot WHERE id=#{id}")
    int deleteById(Long id);
}
