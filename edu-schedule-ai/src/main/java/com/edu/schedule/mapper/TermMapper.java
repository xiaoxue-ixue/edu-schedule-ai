package com.edu.schedule.mapper;

import com.edu.schedule.entity.Term;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface TermMapper {
    @Select("<script>SELECT * FROM edu_term WHERE 1=1 <if test='keyword!=null and keyword!=\"\"'> AND term_name LIKE CONCAT('%',#{keyword},'%') </if> ORDER BY id DESC</script>")
    List<Term> selectList(@Param("keyword") String keyword);

    @Select("SELECT * FROM edu_term WHERE id=#{id}")
    Term selectById(Long id);

    @Insert("INSERT INTO edu_term(term_code, term_name, start_date, end_date, status, create_time) VALUES(#{termCode}, #{termName}, #{startDate}, #{endDate}, #{status}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Term entity);

    @Update("<script>UPDATE edu_term SET update_time=NOW()<if test='termCode!=null'>, term_code=#{termCode}</if><if test='termName!=null'>, term_name=#{termName}</if><if test='startDate!=null'>, start_date=#{startDate}</if><if test='endDate!=null'>, end_date=#{endDate}</if><if test='status!=null'>, status=#{status}</if> WHERE id=#{id}</script>")
    int update(Term entity);

    @Delete("DELETE FROM edu_term WHERE id=#{id}")
    int deleteById(Long id);
}
