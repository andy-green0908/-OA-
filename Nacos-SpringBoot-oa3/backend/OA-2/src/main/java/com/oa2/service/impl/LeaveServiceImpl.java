package com.oa2.service.impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa2.dao.LeaveDao;
import com.oa2.pojo.Emp;
import com.oa2.pojo.LeaveRequest;
import com.oa2.service.LeaveService;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.UUID;

@Service
public class LeaveServiceImpl implements LeaveService {

    private static final String STATUS_PENDING = "待审批";
    private static final String STATUS_CANCELED = "已撤回";

    @Autowired
    private LeaveDao leaveDao;

    @Override
    @Transactional(readOnly = true)
    public RESP listMine(int currentPage, int pageSize, HttpSession session) {
        Emp emp = currentEmp(session);
        if (emp == null) {
            return RESP.error(401, "未登录或登录已过期");
        }
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        PageHelper.startPage(currentPage, pageSize);
        List<LeaveRequest> list = leaveDao.selectByNumber(emp.getNumber());
        PageInfo<LeaveRequest> pageInfo = new PageInfo<>(list);
        return RESP.ok(pageInfo.getList(), currentPage, (int) pageInfo.getTotal());
    }

    @Override
    public RESP apply(LeaveRequest leaveRequest, HttpSession session) {
        Emp emp = currentEmp(session);
        if (emp == null) {
            return RESP.error(401, "未登录或登录已过期");
        }
        if (!valid(leaveRequest)) {
            return RESP.error(400, "请假起止时间和事由不能为空");
        }
        if (!validDateRange(leaveRequest.getStartDate(), leaveRequest.getEndDate())) {
            return RESP.error(400, "请假结束时间必须晚于开始时间");
        }

        leaveRequest.setId(UUID.randomUUID().toString());
        leaveRequest.setNumber(emp.getNumber());
        leaveRequest.setName(emp.getName());
        leaveRequest.setDept_name(emp.getDept_name());
        leaveRequest.setReason(leaveRequest.getReason().trim());
        leaveRequest.setStatus(STATUS_PENDING);

        int rows = leaveDao.insert(leaveRequest);
        if (rows <= 0) {
            return RESP.error("提交请假申请失败");
        }
        return RESP.ok("提交成功");
    }

    @Override
    public RESP cancel(String id, HttpSession session) {
        Emp emp = currentEmp(session);
        if (emp == null) {
            return RESP.error(401, "未登录或登录已过期");
        }
        if (id == null || id.trim().isEmpty()) {
            return RESP.error(400, "请假申请 ID 不能为空");
        }
        LeaveRequest leaveRequest = leaveDao.selectByIdAndNumber(id, emp.getNumber());
        if (leaveRequest == null) {
            return RESP.error(404, "请假申请不存在");
        }
        int rows = leaveDao.updateStatusByEmployee(id, emp.getNumber(), STATUS_CANCELED);
        if (rows <= 0) {
            return RESP.error(400, "只有待审批申请可以撤回");
        }
        return RESP.ok("撤回成功");
    }

    private Emp currentEmp(HttpSession session) {
        return session == null ? null : (Emp) session.getAttribute("emp");
    }

    private boolean valid(LeaveRequest leaveRequest) {
        return leaveRequest != null
                && hasText(leaveRequest.getStartDate())
                && hasText(leaveRequest.getEndDate())
                && hasText(leaveRequest.getReason());
    }

    private boolean validDateRange(String startDate, String endDate) {
        try {
            SimpleDateFormat dateTimeFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return dateTimeFormat.parse(endDate).after(dateTimeFormat.parse(startDate));
        } catch (ParseException e) {
            return false;
        }
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
