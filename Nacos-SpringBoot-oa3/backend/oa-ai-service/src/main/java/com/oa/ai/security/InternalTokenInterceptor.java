package com.oa.ai.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.ai.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class InternalTokenInterceptor implements HandlerInterceptor {

    private final String internalToken;
    private final ObjectMapper objectMapper;

    public InternalTokenInterceptor(@Value("${oa.internal-token}") String internalToken,
                                    ObjectMapper objectMapper) {
        this.internalToken = internalToken;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws IOException {
        if (secureEquals(internalToken, request.getHeader("X-Internal-Token"))) {
            return true;
        }
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        objectMapper.writeValue(response.getWriter(),
                ApiResponse.error(HttpStatus.FORBIDDEN.value(), "内部调用凭证无效"));
        return false;
    }

    private boolean secureEquals(String expected, String actual) {
        if (expected == null || actual == null) {
            return false;
        }
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8));
    }
}
