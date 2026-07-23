package com.oa2.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa2.dao.EmpDao;
import com.oa2.pojo.Emp;
import com.oa2.service.OnlineUserTracker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.servlet.HandlerInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    @Autowired
    private EmpDao empDao;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        HttpSession session = request.getSession();
        Emp emp = resolveEmployee(request, session);
        if (emp == null) {
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

    private Emp resolveEmployee(HttpServletRequest request, HttpSession session) {
        String token = request.getHeader("X-Emp-Token");
        if (token != null && !token.trim().isEmpty()) {
            Integer number = onlineUserTracker.getEmployeeNumberByToken(token);
            if (number != null) {
                Emp emp = empDao.selectByEmpNumber(number);
                if (emp != null) {
                    session.setAttribute("emp", emp);
                    onlineUserTracker.markEmployeeTokenOnline(token);
                    return emp;
                }
            }
        }
        return (Emp) session.getAttribute("emp");
    }
}
