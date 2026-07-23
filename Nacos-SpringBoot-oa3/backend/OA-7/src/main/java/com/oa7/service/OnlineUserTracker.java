package com.oa7.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.params.SetParams;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Component
public class OnlineUserTracker {

    private static final String ADMIN_KEY = "oa:online:admins";
    private static final String ADMIN_SESSION_USERS_KEY = "oa:online:admin:session_users";
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

    public void markAdminOnline(int adminId) {
        String token = issueAdminToken(adminId);
        markAdminTokenOnline(token);
    }

    public void markAdminOffline(int adminId) {
        markAccountOffline(ADMIN_KEY, ADMIN_SESSION_USERS_KEY, String.valueOf(adminId));
    }

    public int onlineAdminCount() {
        return countOnlineAccounts(ADMIN_KEY, ADMIN_SESSION_USERS_KEY);
    }

    public int onlineEmployeeCount() {
        return countOnlineAccounts(EMP_KEY, EMP_SESSION_USERS_KEY);
    }

    public int onlineUserCount() {
        return onlineAdminCount() + onlineEmployeeCount();
    }

    public String issueAdminToken(int adminId) {
        return issueToken(ADMIN_KEY, ADMIN_SESSION_USERS_KEY, String.valueOf(adminId));
    }

    public Integer getAdminIdByToken(String token) {
        String value = getAccountByToken(ADMIN_SESSION_USERS_KEY, token);
        return value == null ? null : Integer.valueOf(value);
    }

    public void markAdminTokenOnline(String token) {
        markTokenOnline(ADMIN_KEY, ADMIN_SESSION_USERS_KEY, token);
    }

    public void markAdminTokenOffline(String token) {
        markTokenOffline(ADMIN_KEY, ADMIN_SESSION_USERS_KEY, token);
    }

    private String issueToken(String onlineKey, String sessionUsersKey, String accountId) {
        String token = UUID.randomUUID().toString().replace("-", "");
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.hset(sessionUsersKey, token, accountId);
            jedis.zadd(onlineKey, System.currentTimeMillis(), token);
        } catch (Exception ignored) {
        }
        return token;
    }

    private void markTokenOnline(String onlineKey, String sessionUsersKey, String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            if (jedis.hexists(sessionUsersKey, token)) {
                jedis.zadd(onlineKey, System.currentTimeMillis(), token);
            }
        } catch (Exception ignored) {
        }
    }

    private void markTokenOffline(String onlineKey, String sessionUsersKey, String token) {
        if (token == null || token.trim().isEmpty()) {
            return;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.zrem(onlineKey, token);
            jedis.hdel(sessionUsersKey, token);
        } catch (Exception ignored) {
        }
    }

    private void markAccountOffline(String onlineKey, String sessionUsersKey, String accountId) {
        try (Jedis jedis = jedisPool.getResource()) {
            for (String token : jedis.hkeys(sessionUsersKey)) {
                if (accountId.equals(jedis.hget(sessionUsersKey, token))) {
                    jedis.zrem(onlineKey, token);
                    jedis.hdel(sessionUsersKey, token);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private String getAccountByToken(String sessionUsersKey, String token) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        try (Jedis jedis = jedisPool.getResource()) {
            cleanupExpired(jedis, ADMIN_KEY, ADMIN_SESSION_USERS_KEY);
            cleanupExpired(jedis, EMP_KEY, EMP_SESSION_USERS_KEY);
            String value = jedis.hget(sessionUsersKey, token);
            if (value != null && jedis.zscore(resolveOnlineKey(sessionUsersKey), token) == null) {
                jedis.hdel(sessionUsersKey, token);
                return null;
            }
            return value;
        } catch (Exception ignored) {
            return null;
        }
    }

    private int countOnlineAccounts(String onlineKey, String sessionUsersKey) {
        try (Jedis jedis = jedisPool.getResource()) {
            cleanupExpired(jedis, onlineKey, sessionUsersKey);
            Set<String> accountIds = new HashSet<>();
            for (String token : jedis.zrange(onlineKey, 0, -1)) {
                String accountId = jedis.hget(sessionUsersKey, token);
                if (accountId == null) {
                    jedis.zrem(onlineKey, token);
                } else {
                    accountIds.add(accountId);
                }
            }
            return accountIds.size();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private void cleanupExpired(Jedis jedis, String onlineKey, String sessionUsersKey) {
        long cutoff = System.currentTimeMillis() - ONLINE_TIMEOUT_MILLIS;
        Set<String> expiredTokens = jedis.zrangeByScore(onlineKey, 0, cutoff);
        for (String token : expiredTokens) {
            jedis.hdel(sessionUsersKey, token);
        }
        jedis.zremrangeByScore(onlineKey, 0, cutoff);
    }

    private String resolveOnlineKey(String sessionUsersKey) {
        return ADMIN_SESSION_USERS_KEY.equals(sessionUsersKey) ? ADMIN_KEY : EMP_KEY;
    }

    public boolean tryLock(String key, String value, int seconds) {
        try (Jedis jedis = jedisPool.getResource()) {
            String result = jedis.set(key, value, SetParams.setParams().nx().ex(seconds));
            return "OK".equals(result);
        } catch (Exception ignored) {
            return false;
        }
    }

    public void unlock(String key, String value) {
        try (Jedis jedis = jedisPool.getResource()) {
            if (value.equals(jedis.get(key))) {
                jedis.del(key);
            }
        } catch (Exception ignored) {
        }
    }
}
