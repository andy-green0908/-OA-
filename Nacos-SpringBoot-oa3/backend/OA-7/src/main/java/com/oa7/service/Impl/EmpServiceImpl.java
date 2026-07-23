package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.service.DeptService;
import com.oa7.service.EmpService;
import com.oa7.dao.EmpDao;
import com.oa7.pojo.Emp;
import com.oa7.pojo.Sign;
import com.oa7.repository.SignElasticsearchRepository;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * @name: chenle
 * @Date: 2021/12/3 14:57
 * @Author: IAO
 * @Description: ...
 */
@Service
public class EmpServiceImpl implements EmpService {

    @Autowired
    private EmpDao empDao;

    @Autowired
    private DeptService deptService;

    @Autowired
    private SignElasticsearchRepository signRepository;

    @Override
    @Transactional(readOnly = true)
    public RESP list(int currentPage, int pageSize) {
        return listInternal(currentPage, pageSize, null);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP list(int currentPage, int pageSize, String keyword) {
        return listInternal(currentPage, pageSize, keyword);
    }

    private RESP listInternal(int currentPage, int pageSize, String keyword) {
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        PageHelper.startPage(currentPage, pageSize);
        List<Emp> list = empDao.selectByPageHelperKeyword(normalize(keyword));
        PageInfo<Emp> pageInfo = new PageInfo<>(list);
        return RESP.ok(pageInfo.getList(), currentPage, (int) pageInfo.getTotal());
    }

    @Override
    public String add(Emp emp) {
        if (!validEmp(emp)) {
            return "false";
        }
        int rows = empDao.addEmp(emp);
        if (rows > 0) {
            deptService.evictCache();
            return "true";
        }
        return "false";
    }

    @Override
    public RESP update(Emp emp, int currentPage, int pageSize) {
        return update(emp, currentPage, pageSize, null);
    }

    @Override
    public RESP update(Emp emp, int currentPage, int pageSize, String keyword) {
        if (emp == null || emp.getNumber() <= 0 || !validEmp(emp)) {
            return RESP.error(400, "员工参数不完整");
        }
        int rows = empDao.updateEmp(emp);
        if (rows <= 0) {
            return RESP.error("更新员工信息失败");
        }
        deptService.evictCache();
        return list(currentPage, pageSize, keyword);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delete(Emp emp) {
        if (emp == null || emp.getNumber() <= 0) {
            return "false";
        }
        List<Sign> esSigns = signRepository.findByNumber(emp.getNumber());
        empDao.deleteEmpSignByNumber(emp);
        int rows = empDao.deleteEmp(emp);
        if (rows > 0) {
            if (!esSigns.isEmpty()) {
                signRepository.deleteAll(esSigns);
            }
            deptService.evictCache();
            return "true";
        }
        return "false";
    }

    @Override
    @Transactional(readOnly = true)
    public RESP departments() {
        return RESP.ok(empDao.getDeptData());
    }

    @Override
    @Transactional(readOnly = true)
    public RESP duties() {
        return RESP.ok(empDao.getDutyData());
    }

    private boolean validEmp(Emp emp) {
        return emp != null
                && emp.getName() != null && !emp.getName().trim().isEmpty()
                && emp.getBirthday() != null && !emp.getBirthday().trim().isEmpty()
                && emp.getAddress() != null && !emp.getAddress().trim().isEmpty()
                && emp.getDept_id() > 0
                && emp.getDuty_id() > 0;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
