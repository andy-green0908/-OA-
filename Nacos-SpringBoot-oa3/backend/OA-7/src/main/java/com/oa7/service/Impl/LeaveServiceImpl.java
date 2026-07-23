package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.dao.LeaveDao;
import com.oa7.pojo.LeaveRequest;
import com.oa7.service.LeaveService;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class LeaveServiceImpl implements LeaveService {

    private static final String STATUS_APPROVED = "已批准";
    private static final String STATUS_REJECTED = "已拒绝";

    @Autowired
    private LeaveDao leaveDao;

    @Override
    @Transactional(readOnly = true)
    public RESP list(int currentPage, int pageSize, String status, String keyword) {
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        PageHelper.startPage(currentPage, pageSize);
        List<LeaveRequest> list = leaveDao.selectPage(normalize(status), normalize(keyword));
        PageInfo<LeaveRequest> pageInfo = new PageInfo<>(list);
        return RESP.ok(pageInfo.getList(), currentPage, (int) pageInfo.getTotal());
    }

    @Override
    public RESP approve(String id) {
        return changeStatus(id, STATUS_APPROVED);
    }

    @Override
    public RESP reject(String id) {
        return changeStatus(id, STATUS_REJECTED);
    }

    private RESP changeStatus(String id, String status) {
        if (id == null || id.trim().isEmpty()) {
            return RESP.error(400, "请假申请 ID 不能为空");
        }
        if (leaveDao.selectById(id) == null) {
            return RESP.error(404, "请假申请不存在");
        }
        int rows = leaveDao.updateStatus(id, status);
        if (rows <= 0) {
            return RESP.error(400, "只有待审批申请可以审批");
        }
        return RESP.ok("审批成功");
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
