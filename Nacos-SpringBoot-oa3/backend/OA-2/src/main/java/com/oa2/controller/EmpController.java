package com.oa2.controller;

import com.oa2.service.OnlineUserTracker;
import com.oa2.service.EmpService;
import com.oa2.pojo.Emp;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * 员工端 - 员工管理控制器
 */
@RestController
@RequestMapping
@CrossOrigin
public class EmpController {

    @Autowired
    private EmpService empService;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    /**
     * 员工登录
     */
    @PostMapping("/login")
    public RESP login(@RequestBody Emp emp,
                      HttpSession session,
                      @RequestHeader(value = "X-Emp-Token", required = false) String oldToken) {
        RESP response = empService.emplogin(emp, session);
        if (response.getCode() == 200 && oldToken != null && !oldToken.trim().isEmpty()) {
            onlineUserTracker.markEmployeeTokenOffline(oldToken);
        }
        return response;
    }

    /**
     * 获取当前登录员工信息
     */
    @GetMapping("/profile")
    public RESP getProfile(HttpSession session) {
        Emp emp = (Emp) session.getAttribute("emp");
        return RESP.ok(emp);
    }

    /**
     * 更新员工密码
     */
    @PutMapping("/password")
    public String updatePassword(@RequestBody Emp emp, @RequestParam("oldPassword") String oldPassword) {
        return empService.updateEmpPwd(emp, oldPassword);
    }

    /**
     * 更新员工个人信息
     */
    @PutMapping("/profile")
    public RESP updateProfile(@RequestBody Emp emp, HttpSession session) {
        return empService.updateInfo(emp, session);
    }

    /**
     * 员工退出登录
     */
    @PostMapping("/logout")
    public RESP logout(HttpSession session, @RequestHeader(value = "X-Emp-Token", required = false) String token) {
        if (token != null && !token.trim().isEmpty()) {
            onlineUserTracker.markEmployeeTokenOffline(token);
        }
        Emp emp = (Emp) session.getAttribute("emp");
        if ((token == null || token.trim().isEmpty()) && emp != null) {
            onlineUserTracker.markEmployeeOffline(emp.getNumber());
        }
        session.removeAttribute("emp");
        return RESP.ok("退出登录成功");
    }
}
