package com.oa7.controller;

import com.oa7.service.OnlineUserTracker;
import com.oa7.util.RESP;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/system")
@CrossOrigin
public class SystemController {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private OnlineUserTracker onlineUserTracker;

    @GetMapping("/health")
    public RESP health() {
        boolean dataService = checkDataService();
        Map<String, Object> health = new HashMap<>();
        health.put("gatewayConnected", true);
        health.put("dataService", dataService);
        int onlineAdmins = onlineUserTracker.onlineAdminCount();
        int onlineEmployees = onlineUserTracker.onlineEmployeeCount();
        health.put("onlineAdmins", onlineAdmins);
        health.put("onlineEmployees", onlineEmployees);
        health.put("onlineUsers", onlineAdmins + onlineEmployees);
        health.put("availability", dataService ? 100.0 : 0.0);
        health.put("status", dataService ? "HEALTHY" : "DEGRADED");
        return RESP.ok(health);
    }

    private boolean checkDataService() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("select 1")) {
            statement.execute();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
