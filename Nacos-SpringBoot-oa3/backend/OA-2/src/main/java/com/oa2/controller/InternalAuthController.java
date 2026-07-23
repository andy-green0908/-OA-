package com.oa2.controller;

import com.oa2.service.OnlineUserTracker;
import com.oa2.util.RESP;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/internal/auth")
public class InternalAuthController {

    private final OnlineUserTracker onlineUserTracker;
    private final String internalToken;

    public InternalAuthController(OnlineUserTracker onlineUserTracker,
                                  @Value("${oa.internal-token}") String internalToken) {
        this.onlineUserTracker = onlineUserTracker;
        this.internalToken = internalToken;
    }

    @GetMapping("/employee")
    public RESP validateEmployee(
            @RequestHeader(value = "X-Internal-Token", required = false) String requestInternalToken,
            @RequestHeader(value = "X-Emp-Token", required = false) String employeeToken) {
        if (!secureEquals(internalToken, requestInternalToken)) {
            return RESP.error(403, "内部调用凭证无效");
        }
        Integer employeeNumber = onlineUserTracker.getEmployeeNumberByToken(employeeToken);
        if (employeeNumber == null) {
            return RESP.error(401, "员工登录已过期");
        }
        onlineUserTracker.markEmployeeTokenOnline(employeeToken);
        return RESP.ok(employeeNumber);
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
