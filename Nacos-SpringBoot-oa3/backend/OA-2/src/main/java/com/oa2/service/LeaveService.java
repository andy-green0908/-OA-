package com.oa2.service;

import com.oa2.pojo.LeaveRequest;
import com.oa2.util.RESP;

import jakarta.servlet.http.HttpSession;

public interface LeaveService {
    RESP listMine(int currentPage, int pageSize, HttpSession session);

    RESP apply(LeaveRequest leaveRequest, HttpSession session);

    RESP cancel(String id, HttpSession session);
}
