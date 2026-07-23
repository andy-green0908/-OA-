package com.oa2.dao;

import com.oa2.pojo.Emp;
import com.oa2.pojo.Sign;
import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SignDao {

    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id " +
            "order by day.sign.id")
    List<Sign> selectAll();

    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id " +
            "where day.sign.number=#{emp.number}   " +
            "and signDate like concat(#{today},'%' )")
    List<Sign> selectEmpSign(@Param("emp") Emp emp , @Param("today") String today);


    @Insert("insert into day.sign(signDate,number,state,type,sign_address) values (#{signDate} ,#{number} ,#{state} ,#{type}, #{sign_address} )")
    int addSign(Sign sign);

    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id " +
            "where day.sign.number=#{number} " +
            "and day.sign.type=#{type} " +
            "and day.sign.signDate like concat(#{dateOnly},'%' ) " +
            "order by day.sign.id desc")
    List<Sign> selectByNumberDateType(@Param("number") int number, @Param("dateOnly") String dateOnly, @Param("type") String type);


    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.number=#{emp.number} " +
            "order by id DESC limit #{current},#{size} ")
    List<Sign> SelectByPage(@Param("emp") Emp emp , @Param("current") int current , @Param("size") int size);


    @Select("select sign.*,dept_name,name from day.sign " +
            "left join day.emp e on e.number = sign.number " +
            "left join day.department d on d.dept_id = e.dept_id  " +
            "where day.sign.number=#{emp.number} " +
            "order by id DESC")
    List<Sign> selectByPagehelper(@Param("emp") Emp emp);


    @Select("SELECT count(*) FROM `day`.sign where number=#{number}")
    int countSignByNumber(Emp emp);

    @Update("update day.sign set state=#{sign.state}  ,signDate=#{date},sign_address=#{sign.sign_address}   where number=#{sign.number}  and signDate=#{sign.signDate}   ")
    int updateState(@Param("sign") Sign sign , @Param("date") String date);

    @Update("update day.sign set state=#{sign.state}, signDate=#{sign.signDate}, sign_address=#{sign.sign_address} " +
            "where number=#{sign.number} and type=#{sign.type} and signDate like concat(#{dateOnly},'%' )")
    int updateByNumberDateType(@Param("sign") Sign sign, @Param("dateOnly") String dateOnly);


}
