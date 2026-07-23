package com.oa7.service;

import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpSessionAttributeListener;
import jakarta.servlet.http.HttpSessionBindingEvent;
import jakarta.servlet.http.HttpSessionEvent;
import jakarta.servlet.http.HttpSessionListener;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AdminSessionCounter implements HttpSessionAttributeListener, HttpSessionListener {

    private final Set<String> adminSessions = ConcurrentHashMap.newKeySet();

    @Override
    public void attributeAdded(HttpSessionBindingEvent event) {
        if ("admin".equals(event.getName())) {
            adminSessions.add(event.getSession().getId());
        }
    }

    @Override
    public void attributeRemoved(HttpSessionBindingEvent event) {
        if ("admin".equals(event.getName())) {
            adminSessions.remove(event.getSession().getId());
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        adminSessions.remove(event.getSession().getId());
    }

    public int onlineAdmins() {
        return adminSessions.size();
    }
}
