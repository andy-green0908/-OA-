package com.oa7.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa7.dao.AdmDao;
import com.oa7.pojo.Admin;
import com.oa7.service.OnlineUserTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

/**
 * @name: chenle
 * @Date: 2021/12/24 16:40
 * @Author: IAO
 * @Description: 登录拦截器
 */
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    @Autowired
    private AdmDao admDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Admin admin = resolveAdmin(request, session);
        if (admin == null) {
            // 前后端分离架构：返回JSON错误响应而不是重定向
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            
            Map<String, Object> result = new HashMap<>();
            result.put("code", 401);
            result.put("message", "未登录或登录已过期，请重新登录");
            result.put("data", null);
            
            ObjectMapper objectMapper = new ObjectMapper();
            PrintWriter writer = response.getWriter();
            writer.write(objectMapper.writeValueAsString(result));
            writer.flush();
            writer.close();
            
            return false;
        }
        return true;
    }

    private Admin resolveAdmin(HttpServletRequest request, HttpSession session) {
        String token = request.getHeader("X-Admin-Token");
        if (token != null && !token.trim().isEmpty()) {
            Integer adminId = onlineUserTracker.getAdminIdByToken(token);
            if (adminId != null) {
                Admin admin = admDao.selectById(adminId);
                if (admin != null) {
                    admin.setPwd(null);
                    session.setAttribute("admin", admin);
                    onlineUserTracker.markAdminTokenOnline(token);
                    return admin;
                }
            }
        }
        return (Admin) session.getAttribute("admin");
    }
}
