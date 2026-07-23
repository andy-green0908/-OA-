package com.oa7.controller;

import com.oa7.service.AdmService;
import com.oa7.service.OnlineUserTracker;
import com.oa7.pojo.Admin;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;

/**
 * 管理员端 - 管理员认证控制器
 */
@RestController
@RequestMapping("/auth")
@CrossOrigin
public class AdmController {

    @Autowired
    private AdmService admService;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    @PostMapping("/login")
    public RESP login(@RequestBody Admin admin,
                      HttpSession session,
                      @RequestHeader(value = "X-Admin-Token", required = false) String oldToken) {
        RESP response = admService.login(admin, session);
        if (response.getCode() == 200 && oldToken != null && !oldToken.trim().isEmpty()) {
            onlineUserTracker.markAdminTokenOffline(oldToken);
        }
        return response;
    }

    @PostMapping("/register")
    public String register(@RequestBody Admin admin) {
        return admService.register(admin);
    }

    @GetMapping("/profile")
    public RESP profile(HttpSession session) {
        Admin admin = (Admin) session.getAttribute("admin");
        return RESP.ok(admin);
    }

    @PostMapping("/logout")
    public RESP logout(HttpSession session, @RequestHeader(value = "X-Admin-Token", required = false) String token) {
        if (token != null && !token.trim().isEmpty()) {
            onlineUserTracker.markAdminTokenOffline(token);
        }
        Admin admin = (Admin) session.getAttribute("admin");
        if ((token == null || token.trim().isEmpty()) && admin != null) {
            onlineUserTracker.markAdminOffline(admin.getId());
        }
        session.removeAttribute("admin");
        return RESP.ok("退出登录成功");
    }
}
