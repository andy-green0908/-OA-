package com.oa7.service;

import com.oa7.pojo.Emp;
import com.oa7.util.RESP;


/**
 * @name: chenle
 * @Date: 2021/12/3 14:44
 * @Author: IAO
 * @Description: ...
 */
public interface EmpService {

    RESP list(int currentPage, int pageSize);

    RESP list(int currentPage, int pageSize, String keyword);

    String add(Emp emp);

    RESP update(Emp emp, int currentPage, int pageSize);

    RESP update(Emp emp, int currentPage, int pageSize, String keyword);

    String delete(Emp emp);

    RESP departments();

    RESP duties();
}
