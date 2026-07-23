package com.oa7.service.Impl;

import com.oa7.service.AdmService;
import com.oa7.service.OnlineUserTracker;
import com.oa7.dao.AdmDao;
import com.oa7.pojo.Admin;
import com.oa7.util.RESP;
import com.liuvei.common.SysFun;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

/**
 * @name: chenle
 * @Date: 2021/12/3 14:47
 * @Author: IAO
 * @Description: ...
 */
@Service
public class AdmServiceImpl implements AdmService {

    @Autowired
    private AdmDao admDao;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    @Override
    @Transactional(readOnly = true)
    public RESP login(Admin admin, HttpSession session) {
        if (admin == null || admin.getName() == null || admin.getPwd() == null) {
            return RESP.error(401, "登录失败，请检查用户名和密码");
        }
        Admin dbAdmin = admDao.selectByName(admin);
        if (dbAdmin == null) {
            return RESP.error(401, "登录失败，请检查用户名和密码");
        }
        String md5Pwd = SysFun.md5(admin.getPwd());
        if (md5Pwd.equals(dbAdmin.getPwd()) || admin.getPwd().equals(dbAdmin.getPwd())) {
            dbAdmin.setPwd(null);
            session.setAttribute("admin", dbAdmin);
            String token = onlineUserTracker.issueAdminToken(dbAdmin.getId());
            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("admin", dbAdmin);
            return RESP.ok(data);
        }
        return RESP.error(401, "登录失败，请检查用户名和密码");
    }

    @Override
    public String register(Admin admin) {
        if (admin == null || admin.getName() == null || admin.getPwd() == null) {
            return "false";
        }
        if (admDao.selectByName(admin) != null) {
            return "false";
        }
        admin.setPwd(SysFun.md5(admin.getPwd()));
        return admDao.insertAdm(admin) > 0 ? "true" : "false";
    }
}
