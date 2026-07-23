package com.oa7.service;

import com.oa7.pojo.Duty;
import com.oa7.util.RESP;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:43
 * @Author: IAO
 * @Description: ...
 */
public interface DutyService {

    RESP list(int currentPage, int pageSize);

    String add(Duty duty);

    String update(Duty duty);

    String delete(int dutyId);
}
