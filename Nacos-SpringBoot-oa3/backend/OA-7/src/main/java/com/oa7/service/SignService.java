package com.oa7.service;

import com.oa7.pojo.Sign;
import com.oa7.util.RESP;

import jakarta.servlet.http.HttpSession;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:44
 * @Author: IAO
 * @Description: ...
 */
public interface SignService {

    RESP signed(int currentPage, int pageSize);

    RESP unsigned(int currentPage, int pageSize);

    RESP unsigned(int currentPage, int pageSize, String date, String signType);

    RESP unsigned(int currentPage, int pageSize, String date, String signType, String keyword);

    RESP todaySigned(int currentPage, int pageSize);

    RESP todayUnsigned(int currentPage, int pageSize);

    RESP todayUnsigned(int currentPage, int pageSize, String signType);

    RESP todayUnsigned(int currentPage, int pageSize, String signType, String keyword);

    RESP approve(String id);

    RESP dailyStatistics(int currentPage, int pageSize);

    RESP dailyDetails(String date);

    RESP statisticsChart();
}
