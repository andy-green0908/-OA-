package com.oa2.dao;

import com.oa2.pojo.LeaveRequest;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LeaveDao {

    @Select("select id, number, name, dept_name, " +
            "date_format(start_date, '%Y-%m-%d %H:%i:%s') as startDate, " +
            "date_format(end_date, '%Y-%m-%d %H:%i:%s') as endDate, " +
            "reason, status from day.`leave` where number=#{number} order by start_date desc")
    List<LeaveRequest> selectByNumber(@Param("number") int number);

    @Select("select id, number, name, dept_name, " +
            "date_format(start_date, '%Y-%m-%d %H:%i:%s') as startDate, " +
            "date_format(end_date, '%Y-%m-%d %H:%i:%s') as endDate, " +
            "reason, status from day.`leave` where id=#{id} and number=#{number}")
    LeaveRequest selectByIdAndNumber(@Param("id") String id, @Param("number") int number);

    @Insert("insert into day.`leave` (id, number, name, dept_name, start_date, end_date, reason, status) " +
            "values (#{id}, #{number}, #{name}, #{dept_name}, #{startDate}, #{endDate}, #{reason}, #{status})")
    int insert(LeaveRequest leaveRequest);

    @Update("update day.`leave` set status=#{status} where id=#{id} and number=#{number} and status='待审批'")
    int updateStatusByEmployee(@Param("id") String id,
                               @Param("number") int number,
                               @Param("status") String status);
}
