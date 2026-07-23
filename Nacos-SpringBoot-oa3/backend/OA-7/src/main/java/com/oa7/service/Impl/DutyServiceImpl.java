package com.oa7.service.Impl;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.oa7.service.DutyService;
import com.oa7.dao.DutyDao;
import com.oa7.pojo.Duty;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * @name: chenle
 * @Date: 2021/12/3 16:07
 * @Author: IAO
 * @Description: ...
 */
@Service
public class DutyServiceImpl implements DutyService {

    @Autowired
    private DutyDao dutyDao;

    @Override
    @Transactional(readOnly = true)
    public RESP list(int currentPage, int pageSize) {
        currentPage = Math.max(currentPage, 1);
        pageSize = Math.max(pageSize, 1);
        PageHelper.startPage(currentPage, pageSize);
        List<Duty> list = dutyDao.selectByPageHelper();
        PageInfo<Duty> pageInfo = new PageInfo<>(list);
        return RESP.ok(pageInfo.getList(), currentPage, (int) pageInfo.getTotal());
    }

    @Override
    public String add(Duty duty) {
        if (duty == null || duty.getDuty_name() == null || duty.getDuty_name().trim().isEmpty()) {
            return "false";
        }
        duty.setDuty_name(duty.getDuty_name().trim());
        if (dutyDao.selectByName(duty) != null) {
            return "false";
        }
        int rows = dutyDao.addDuty(duty);
        if (rows > 0) {
            return "true";
        }
        return "false";
    }

    @Override
    public String update(Duty duty) {
        if (duty == null || duty.getDuty_id() <= 0
                || duty.getDuty_name() == null || duty.getDuty_name().trim().isEmpty()) {
            return "false";
        }
        duty.setDuty_name(duty.getDuty_name().trim());
        Duty sameName = dutyDao.selectByName(duty);
        if (sameName != null && sameName.getDuty_id() != duty.getDuty_id()) {
            return "false";
        }
        int rows = dutyDao.updateDutyNameById(duty);
        if (rows > 0) {
            return "true";
        }
        return "false";
    }

    @Override
    public String delete(int dutyId) {
        if (dutyId <= 0) {
            return "false";
        }
        if (dutyDao.countEmployeesByDutyId(dutyId) > 0) {
            return "in_use";
        }
        int rows = dutyDao.deleteDutyById(dutyId);
        if (rows > 0) {
            return "true";
        }
        return "false";
    }
}
