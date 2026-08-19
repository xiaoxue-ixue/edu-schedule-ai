package com.edu.schedule.mapper;

import com.edu.schedule.entity.ScheduleRule;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface ScheduleRuleMapper {
    @Select("<script>SELECT * FROM schedule_rule WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND rule_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<ScheduleRule> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM schedule_rule WHERE id=#{id}")
    ScheduleRule selectById(Long id);

    @Insert("INSERT INTO schedule_rule(rule_name, rule_type, priority, trigger_condition, action, enabled, create_time) VALUES(#{ruleName}, #{ruleType}, #{priority}, #{triggerCondition}, #{action}, #{enabled}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(ScheduleRule entity);

    @Update("<script>UPDATE schedule_rule SET update_time=NOW()<if test='ruleName!=null'>, rule_name=#{ruleName}</if><if test='ruleType!=null'>, rule_type=#{ruleType}</if><if test='priority!=null'>, priority=#{priority}</if><if test='triggerCondition!=null'>, trigger_condition=#{triggerCondition}</if><if test='action!=null'>, action=#{action}</if><if test='enabled!=null'>, enabled=#{enabled}</if> WHERE id=#{id}</script>")
    int update(ScheduleRule entity);

    @Delete("DELETE FROM schedule_rule WHERE id=#{id}")
    int deleteById(Long id);
}
