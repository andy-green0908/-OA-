package com.oa.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatSessionService {

    private static final String SESSION_KEY_PREFIX = "oa:ai:chat:session:";

    private final Map<String, SessionState> sessions = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final int maxExchanges;
    private final int maxSessions;
    private final long idleTimeoutNanos;

    @Autowired
    public ChatSessionService(StringRedisTemplate redisTemplate,
                              ObjectMapper objectMapper,
                              @Value("${oa.ai.max-history-exchanges:6}") int maxExchanges,
                              @Value("${oa.ai.max-sessions:1000}") int maxSessions,
                              @Value("${oa.ai.session-idle-timeout:30m}") Duration idleTimeout) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.maxExchanges = Math.max(1, maxExchanges);
        this.maxSessions = Math.max(1, maxSessions);
        this.idleTimeoutNanos = Math.max(Duration.ofSeconds(1).toNanos(), idleTimeout.toNanos());
    }

    ChatSessionService(int maxExchanges, int maxSessions, Duration idleTimeout) {
        this(null, new ObjectMapper(), maxExchanges, maxSessions, idleTimeout);
    }

    public void addExchange(String sessionId, String question, String answer, boolean grounded) {
        String key = sessionKey(sessionId);
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForList().rightPush(key, serialize(new Exchange(question, answer, grounded)));
                redisTemplate.opsForList().trim(key, -maxExchanges, -1);
                redisTemplate.expire(key, Duration.ofNanos(idleTimeoutNanos));
                return;
            } catch (Exception ignored) {
                // fallback to in-memory store
            }
        }

        SessionState session = getOrCreateSession(sessionId);
        Deque<Exchange> exchanges = session.exchanges();
        synchronized (exchanges) {
            exchanges.addLast(new Exchange(question, answer, grounded));
            while (exchanges.size() > maxExchanges) {
                exchanges.removeFirst();
            }
        }
        session.touch();
    }

    public Optional<String> lastGroundedQuestion(String sessionId) {
        String key = sessionKey(sessionId);
        if (redisTemplate != null) {
            try {
                List<String> payloads = redisTemplate.opsForList().range(key, 0, -1);
                if (payloads != null) {
                    for (int i = payloads.size() - 1; i >= 0; i--) {
                        Exchange exchange = deserialize(payloads.get(i));
                        if (exchange != null && exchange.grounded()) {
                            redisTemplate.expire(key, Duration.ofNanos(idleTimeoutNanos));
                            return Optional.ofNullable(exchange.question());
                        }
                    }
                }
                return Optional.empty();
            } catch (Exception ignored) {
                // fallback to in-memory store
            }
        }

        SessionState session = activeSession(sessionId);
        if (session == null) {
            return Optional.empty();
        }
        Deque<Exchange> exchanges = session.exchanges();
        synchronized (exchanges) {
            Iterator<Exchange> iterator = exchanges.descendingIterator();
            while (iterator.hasNext()) {
                Exchange exchange = iterator.next();
                if (exchange.grounded()) {
                    return Optional.of(exchange.question());
                }
            }
        }
        return Optional.empty();
    }

    public String formatHistory(String sessionId) {
        String key = sessionKey(sessionId);
        if (redisTemplate != null) {
            try {
                List<String> payloads = redisTemplate.opsForList().range(key, 0, -1);
                if (payloads == null || payloads.isEmpty()) {
                    return "（无历史对话）";
                }
                StringBuilder history = new StringBuilder();
                for (String payload : payloads) {
                    Exchange exchange = deserialize(payload);
                    if (exchange == null) {
                        continue;
                    }
                    history.append("用户：").append(exchange.question()).append('\n');
                    history.append("助手：").append(exchange.answer()).append('\n');
                }
                redisTemplate.expire(key, Duration.ofNanos(idleTimeoutNanos));
                return history.isEmpty() ? "（无历史对话）" : history.toString();
            } catch (Exception ignored) {
                // fallback to in-memory store
            }
        }

        SessionState session = activeSession(sessionId);
        if (session == null) {
            return "（无历史对话）";
        }
        Deque<Exchange> exchanges = session.exchanges();
        StringBuilder history = new StringBuilder();
        synchronized (exchanges) {
            for (Exchange exchange : exchanges) {
                history.append("用户：").append(exchange.question()).append('\n');
                history.append("助手：").append(exchange.answer()).append('\n');
            }
        }
        return history.isEmpty() ? "（无历史对话）" : history.toString();
    }

    public boolean clear(String sessionId) {
        String key = sessionKey(sessionId);
        if (redisTemplate != null) {
            try {
                return Boolean.TRUE.equals(redisTemplate.delete(key));
            } catch (Exception ignored) {
                // fallback to in-memory store
            }
        }
        return sessions.remove(sessionId) != null;
    }

    @Scheduled(fixedDelayString = "${oa.ai.session-cleanup-interval:5m}")
    public void cleanupExpiredSessions() {
        long now = System.nanoTime();
        sessions.forEach((sessionId, session) -> {
            if (session.isExpired(now, idleTimeoutNanos)) {
                sessions.remove(sessionId, session);
            }
        });
    }

    private SessionState activeSession(String sessionId) {
        SessionState session = sessions.get(sessionId);
        if (session == null) {
            return null;
        }
        long now = System.nanoTime();
        if (session.isExpired(now, idleTimeoutNanos)) {
            sessions.remove(sessionId, session);
            return null;
        }
        session.touch(now);
        return session;
    }

    private SessionState getOrCreateSession(String sessionId) {
        SessionState session = activeSession(sessionId);
        if (session != null) {
            return session;
        }
        synchronized (sessions) {
            session = activeSession(sessionId);
            if (session != null) {
                return session;
            }
            if (sessions.size() >= maxSessions) {
                sessions.entrySet().stream()
                        .min(Comparator.comparingLong(entry -> entry.getValue().lastAccessNanos()))
                        .ifPresent(entry -> sessions.remove(entry.getKey(), entry.getValue()));
            }
            SessionState created = new SessionState();
            sessions.put(sessionId, created);
            return created;
        }
    }

    private String sessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }

    private String serialize(Exchange exchange) throws JsonProcessingException {
        return objectMapper.writeValueAsString(exchange);
    }

    private Exchange deserialize(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(payload, Exchange.class);
        } catch (JsonProcessingException ignored) {
            return null;
        }
    }

    private static final class SessionState {

        private final Deque<Exchange> exchanges = new ArrayDeque<>();
        private volatile long lastAccessNanos = System.nanoTime();

        private Deque<Exchange> exchanges() {
            return exchanges;
        }

        private long lastAccessNanos() {
            return lastAccessNanos;
        }

        private void touch() {
            touch(System.nanoTime());
        }

        private void touch(long now) {
            lastAccessNanos = now;
        }

        private boolean isExpired(long now, long timeoutNanos) {
            return now - lastAccessNanos >= timeoutNanos;
        }
    }

    record Exchange(String question, String answer, boolean grounded) {
    }
}
