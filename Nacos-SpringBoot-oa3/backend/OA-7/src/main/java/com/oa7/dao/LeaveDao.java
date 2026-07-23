package com.oa7.dao;

import com.oa7.pojo.LeaveRequest;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface LeaveDao {

    @Select("<script>" +
            "select id, number, name, dept_name, " +
            "date_format(start_date, '%Y-%m-%d %H:%i:%s') as startDate, " +
            "date_format(end_date, '%Y-%m-%d %H:%i:%s') as endDate, reason, status " +
            "from day.`leave` " +
            "<where>" +
            "<if test='status != null and status != \"\"'>and status = #{status}</if>" +
            "<if test='keyword != null and keyword != \"\"'>" +
            "and (name like concat('%', #{keyword}, '%') or cast(number as char) like concat('%', #{keyword}, '%'))" +
            "</if>" +
            "</where>" +
            "order by start_date desc" +
            "</script>")
    List<LeaveRequest> selectPage(@Param("status") String status, @Param("keyword") String keyword);

    @Select("select id, number, name, dept_name, " +
            "date_format(start_date, '%Y-%m-%d %H:%i:%s') as startDate, " +
            "date_format(end_date, '%Y-%m-%d %H:%i:%s') as endDate, reason, status " +
            "from day.`leave` where id = #{id}")
    LeaveRequest selectById(@Param("id") String id);

    @Update("update day.`leave` set status=#{status} where id=#{id} and status='待审批'")
    int updateStatus(@Param("id") String id, @Param("status") String status);
}
