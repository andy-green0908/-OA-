package com.oa.ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.ai.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Component
public class EmployeeAuthInterceptor implements HandlerInterceptor {

    public static final String EMPLOYEE_NUMBER_ATTRIBUTE =
            EmployeeAuthInterceptor.class.getName() + ".employeeNumber";

    private final EmployeeTokenValidator employeeTokenValidator;
    private final ObjectMapper objectMapper;

    public EmployeeAuthInterceptor(EmployeeTokenValidator employeeTokenValidator,
                                   ObjectMapper objectMapper) {
        this.employeeTokenValidator = employeeTokenValidator;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        String employeeToken = request.getHeader("X-Emp-Token");
        if (employeeToken == null || employeeToken.isBlank()) {
            writeError(response, HttpStatus.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
            return false;
        }

        try {
            Integer employeeNumber = employeeTokenValidator.validate(employeeToken);
            if (employeeNumber == null) {
                writeError(response, HttpStatus.UNAUTHORIZED, "未登录或登录已过期，请重新登录");
                return false;
            }
            request.setAttribute(EMPLOYEE_NUMBER_ATTRIBUTE, employeeNumber);
            return true;
        } catch (EmployeeTokenValidator.EmployeeAuthUnavailableException exception) {
            writeError(response, HttpStatus.SERVICE_UNAVAILABLE, "员工认证服务暂时不可用");
            return false;
        }
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String message)
            throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(status.value(), message));
    }
}
