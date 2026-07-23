package com.oa.ai.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ChatSessionServiceTest {

    @Test
    void fallbackExchangeDoesNotReplaceLastGroundedQuestion() {
        ChatSessionService service = new ChatSessionService(6, 1000, Duration.ofMinutes(30));
        service.addExchange("session-1", "可以异地签到吗？", "建议提前报备。", true);
        service.addExchange("session-1", "怎么制造量子火箭？", "知识库没有相关信息。", false);

        assertThat(service.lastGroundedQuestion("session-1"))
                .contains("可以异地签到吗？");
    }

    @Test
    void clearedSessionHasNoGroundedQuestion() {
        ChatSessionService service = new ChatSessionService(6, 1000, Duration.ofMinutes(30));
        service.addExchange("session-1", "怎么签到？", "点击签到按钮。", true);

        service.clear("session-1");

        assertThat(service.lastGroundedQuestion("session-1")).isEmpty();
    }
}
