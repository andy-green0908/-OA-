package com.oa7.service;

import com.oa7.pojo.Department;
import com.oa7.util.RESP;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:43
 * @Author: oa5
 * @Description: ...
 */
public interface DeptService {

    RESP list(int currentPage, int pageSize);

    RESP add(Department department, int currentPage, int pageSize);

    RESP update(Department department, int currentPage, int pageSize);

    RESP delete(int deptId, int currentPage, int pageSize);

    void evictCache();
}
