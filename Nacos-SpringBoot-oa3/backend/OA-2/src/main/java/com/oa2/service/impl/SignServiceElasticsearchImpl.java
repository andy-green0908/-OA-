package com.oa2.service.impl;

import com.github.pagehelper.PageInfo;
import com.oa2.dao.EmpDao;
import com.oa2.pojo.Emp;
import com.oa2.pojo.Sign;
import com.oa2.repository.SignElasticsearchRepository;
import com.oa2.service.SignService;
import com.oa2.util.DU;
import com.oa2.util.LocationUtil;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Service
public class SignServiceElasticsearchImpl implements SignService {

    @Autowired
    private SignElasticsearchRepository signRepository;

    @Autowired
    private EmpDao empDao;

    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

    // 获取当前员工签到记录
    @Override
    @Transactional(readOnly = true)
    public RESP empSignList(HttpSession session) {

        System.out.println("获取当前员工签到记录");
        // 1. 获取当前员工数据
        Emp emp = activeSessionEmp(session);
        if (emp == null) {
            return RESP.error("用户未登录或员工已不存在");
        }
        // 2. 获取员工的当天的日期
        String today = DU.getNowSortString();

        ensureTodayTasks(emp, today);
        List<Sign> list = signRepository.findByNumberAndDateOnly(emp.getNumber(), today);
        // 补充员工信息（姓名、部门）
        list = enrichSignRecords(list);

        return RESP.ok(list);
    }

    @Override
    @Transactional(readOnly = true)
    public RESP selectByPagehelper(int currentPage, int pageSize, HttpSession session) {
        return selectByPage(currentPage, pageSize, session);
    }

    //分页查询员工已签到记录
    @Override
    @Transactional(readOnly = true)
    public RESP selectByPage(int currentPage, int pageSize, HttpSession session) {
        // 1. 查询当前员工信息
        Emp emp = activeSessionEmp(session);
        if (emp == null) {
            return RESP.error("用户未登录或员工已不存在");
        }
        // 2. 使用 Elasticsearch 分页查询
        Pageable pageable = PageRequest.of(currentPage - 1, pageSize);
        Page<Sign> page = signRepository.findByNumberOrderByTimestampDesc(emp.getNumber(), pageable);

        //测试Page集合中数据 例如： 员工编号: 145, 签到时间: 2025-07-02 09:43:25:913, 状态: 已签到 ......
        List<Sign> list = page.getContent();

        // 补充员工信息(补充对应的部门信息)
        list = enrichSignRecords(list);
        // 获取总条数
        long total = signRepository.countByNumber(emp.getNumber());

        return RESP.ok(list,currentPage, (int) total);
    }


    @Override
    @Transactional(readOnly = true)
    public RESP updateState(Sign sign, HttpSession session, String coordinates) {
        try {
            Emp emp = activeSessionEmp(session);
            if (emp == null) {
                return RESP.error("用户未登录或员工已不存在");
            }

            // 设置员工编号
            sign.setNumber(emp.getNumber());
            String today = DU.getNowSortString();
            List<Sign> todayRecords = signRepository.findByNumberAndDateOnly(emp.getNumber(), today);
            Sign existingSign = findRecordByType(todayRecords, sign.getType());
            if (existingSign != null) {
                // 检查是否已经签到过
                if ("已签到".equals(existingSign.getState())) {
                    return RESP.error("今日已" + (sign.getType().equals("a") ? "签到" : "签退") + "，不可重复操作");
                }
                // 更新签到状态
                existingSign.setState("已签到");
                existingSign.setSignDate(DU.formatDateToString(new Date()));
                
                // 解析地理位置
                if (coordinates != null && !coordinates.isEmpty()) {
                    // 验证坐标格式
                    if (LocationUtil.isValidCoordinates(coordinates)) {
                        try {
                            String address = LocationUtil.getAddressFromCoordinates(coordinates);
                            existingSign.setSign_address(address);
                        } catch (Exception e) {
                            System.err.println("地址解析异常：" + e.getMessage());
                            existingSign.setSign_address("位置解析失败");
                        }
                    } else {
                        existingSign.setSign_address("坐标格式错误");
                    }
                } else {
                    existingSign.setSign_address("未获取到位置信息");
                }
                // 更新时间戳
                existingSign.setTimestamp(System.currentTimeMillis());
                existingSign.setDateOnly(today);
                // 补充员工信息
                existingSign.setName(emp.getName());
                existingSign.setDept_name(emp.getDept_name());

                upsertAttendance(existingSign, today);
                return RESP.ok((sign.getType().equals("a") ? "签到" : "签退") + "成功");
            } else {
                Sign freshSign = createSignRecord(emp.getNumber(), DU.formatDateToString(new Date()), "已签到", sign.getType(), today);
                freshSign.setName(emp.getName());
                freshSign.setDept_name(emp.getDept_name());
                freshSign.setSign_address(resolveAddress(coordinates));
                upsertAttendance(freshSign, today);
                return RESP.ok((sign.getType().equals("a") ? "签到" : "签退") + "成功");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return RESP.error("签到失败：" + e.getMessage());
        }
    }

    /**
     * 创建签到记录
     */
    private Sign createSignRecord(int empNumber, String signDate, String state, String type) {
        return createSignRecord(empNumber, signDate, state, type, DU.getNowSortString());
    }

    private Sign createSignRecord(int empNumber, String signDate, String state, String type, String dateOnly) {
        Sign sign = new Sign();
        sign.setId(attendanceId(empNumber, dateOnly, type));
        sign.setSignDate(signDate);
        sign.setNumber(empNumber);
        sign.setState(state);
        sign.setType(type);
        sign.setTimestamp(System.currentTimeMillis());
        sign.setDateOnly(dateOnly);
        sign.setTag(0);
        return sign;
    }

    private void ensureTodayTasks(Emp emp, String today) {
        List<Sign> todayRecords = signRepository.findByNumberAndDateOnly(emp.getNumber(), today);
        if (findRecordByType(todayRecords, "a") == null) {
            Sign morningSign = createSignRecord(emp.getNumber(), DU.getNowAM(), "未签到", "a", today);
            morningSign.setName(emp.getName());
            morningSign.setDept_name(emp.getDept_name());
            upsertAttendance(morningSign, today);
        }
        if (findRecordByType(todayRecords, "p") == null) {
            Sign afternoonSign = createSignRecord(emp.getNumber(), DU.getNowPM(), "未签到", "p", today);
            afternoonSign.setName(emp.getName());
            afternoonSign.setDept_name(emp.getDept_name());
            upsertAttendance(afternoonSign, today);
        }
    }

    private Sign findRecordByType(List<Sign> records, String type) {
        if (records == null || type == null) {
            return null;
        }
        for (Sign record : records) {
            if (type.equals(record.getType())) {
                return record;
            }
        }
        return null;
    }

    private void upsertAttendance(Sign sign, String dateOnly) {
        if (sign == null) {
            return;
        }
        List<Sign> sameSlotRecords = signRepository.findByNumberAndDateOnly(sign.getNumber(), dateOnly);
        sameSlotRecords.removeIf(record -> !sign.getType().equals(record.getType()));
        if (!sameSlotRecords.isEmpty()) {
            signRepository.deleteAll(sameSlotRecords);
        }
        sign.setId(attendanceId(sign.getNumber(), dateOnly, sign.getType()));
        sign.setDateOnly(dateOnly);
        signRepository.save(sign);
    }

    private String resolveAddress(String coordinates) {
        if (coordinates == null || coordinates.isEmpty()) {
            return "未获取到位置信息";
        }
        if (!LocationUtil.isValidCoordinates(coordinates)) {
            return "坐标格式错误";
        }
        try {
            return LocationUtil.getAddressFromCoordinates(coordinates);
        } catch (Exception e) {
            return "位置解析失败";
        }
    }

    private String attendanceId(int number, String dateOnly, String type) {
        return number + ":" + dateOnly + ":" + type;
    }

    private Emp activeSessionEmp(HttpSession session) {
        Emp sessionEmp = (Emp) session.getAttribute("emp");
        if (sessionEmp == null) {
            return null;
        }
        Emp activeEmp = empDao.selectByEmpNumber(sessionEmp.getNumber());
        if (activeEmp == null) {
            session.removeAttribute("emp");
            return null;
        }
        session.setAttribute("emp", activeEmp);
        return activeEmp;
    }

    /**
     * 补充签到记录的员工信息（姓名、部门）
     */
    private List<Sign> enrichSignRecords(List<Sign> signs) {
        for (Sign sign : signs) {
            try {
                // 根据员工编号查询员工信息
                Emp emp = empDao.selectByEmpNumber(sign.getNumber());
                if (emp != null) {
                    sign.setName(emp.getName());
                    sign.setDept_name(emp.getDept_name());
                }
            } catch (Exception e) {
                // 如果查询失败，设置默认值
                sign.setName("未知员工");
                sign.setDept_name("未知部门");
            }
        }
        return signs;
    }
} 
