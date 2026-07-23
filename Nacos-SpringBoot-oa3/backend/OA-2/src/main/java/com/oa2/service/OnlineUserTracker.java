package com.oa2.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.UUID;

@Component
public class OnlineUserTracker {

    private static final String EMP_KEY = "oa:online:emps";
    private static final String EMP_SESSION_USERS_KEY = "oa:online:emp:session_users";
    private static final long ONLINE_TIMEOUT_MILLIS = 5 * 60 * 1000L;

    @Value("${redis.host:127.0.0.1}")
    private String host;

    @Value("${redis.port:6379}")
    private int port;

    @Value("${redis.password:}")
    private String password;

    @Value("${redis.maxTotal:1000}")
    private int maxTotal;

    @Value("${redis.maxIdle:30}")
    private int maxIdle;

    private JedisPool jedisPool;

    @PostConstruct
    public void init() {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(maxTotal);
        config.setMaxIdle(maxIdle);
        if (password == null || password.trim().isEmpty()) {
            jedisPool = new JedisPool(config, host, port);
        } else {
            jedisPool = new JedisPool(config, host, port, 2000, password);
        }
    }

    @PreDestroy
    public void destroy() {
        if (jedisPool != null) {
            jedisPool.close();
        }
    }

    public void markEmployeeOnline(int employeeNumber) {
        String token = issueEmployeeToken(employeeNumber);
        markEmployeeTokenOnline(token);
    }

    public void markEmployeeOffline(int employeeNumber) {
        markAccountOffline(String.valueOf(employeeNumber));
    }

    public String issueEmployeeToken(int employeeNumber) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(EMP_SESSION_USERS_KEY, token, String.valueOf(employeeNumber));
            jedis.zadd(EMP_KEY, System.currentTimeMillis(), token);
        } catch (Exception ignored) {
        }
        return token;
    }

    public Integer getEmployeeNumberByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            cleanupExpired(jedis);
            String value = jedis.hget(EMP_SESSION_USERS_KEY, token);
            if (value != null && jedis.zscore(EMP_KEY, token) == null) {
                jedis.hdel(EMP_SESSION_USERS_KEY, token);
                return null;
            }
            return value == null ? null : Integer.valueOf(value);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void markEmployeeTokenOnline(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.hexists(EMP_SESSION_USERS_KEY, token)) {
                jedis.zadd(EMP_KEY, System.currentTimeMillis(), token);
            }
        } catch (Exception ignored) {
        }
    }

    public void markEmployeeTokenOffline(String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zrem(EMP_KEY, token);
            jedis.hdel(EMP_SESSION_USERS_KEY, token);
        } catch (Exception ignored) {
        }
    }

    private void markAccountOffline(String employeeNumber) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (String token : jedis.hkeys(EMP_SESSION_USERS_KEY)) {
                if (employeeNumber.equals(jedis.hget(EMP_SESSION_USERS_KEY, token))) {
                    jedis.zrem(EMP_KEY, token);
                    jedis.hdel(EMP_SESSION_USERS_KEY, token);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void cleanupExpired(Jedis jedis) {
        long cutoff = System.currentTimeMillis() - ONLINE_TIMEOUT_MILLIS;
        for (String token : jedis.zrangeByScore(EMP_KEY, 0, cutoff)) {
            jedis.hdel(EMP_SESSION_USERS_KEY, token);
        }
        jedis.zremrangeByScore(EMP_KEY, 0, cutoff);
    }
}
