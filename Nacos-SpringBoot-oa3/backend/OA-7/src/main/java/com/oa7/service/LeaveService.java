package com.oa7.service;

import com.oa7.util.RESP;

public interface LeaveService {
    RESP list(int currentPage, int pageSize, String status, String keyword);

    RESP approve(String id);

    RESP reject(String id);
}
