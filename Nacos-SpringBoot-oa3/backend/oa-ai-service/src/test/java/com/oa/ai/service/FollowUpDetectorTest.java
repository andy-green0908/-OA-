package com.oa.ai.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FollowUpDetectorTest {

    @Test
    void explicitFollowUpUsesPreviousTopic() {
        String retrievalQuery = FollowUpDetector.withPreviousTopic(
                "那需要提前报备吗？", "可以异地签到吗？");

        assertThat(retrievalQuery).isEqualTo("可以异地签到吗？ 那需要提前报备吗？");
    }

    @Test
    void independentQuestionDoesNotUsePreviousTopic() {
        String retrievalQuery = FollowUpDetector.withPreviousTopic(
                "怎么制造量子火箭？", "可以异地签到吗？");

        assertThat(retrievalQuery).isEqualTo("怎么制造量子火箭？");
    }

    @Test
    void ambiguousNewQuestionIsNotAutomaticallyTreatedAsFollowUp() {
        assertThat(FollowUpDetector.isFollowUp("为什么天空是蓝色的？")).isFalse();
    }
}
