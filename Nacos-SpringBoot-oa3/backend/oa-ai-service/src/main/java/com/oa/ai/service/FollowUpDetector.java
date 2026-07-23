package com.oa.ai.service;

import java.util.List;

final class FollowUpDetector {

    private static final List<String> FOLLOW_UP_PREFIXES = List.of(
            "那", "那么", "这个", "这种", "它", "上述", "刚才", "前面",
            "还需要", "还要", "然后", "之后", "这样", "此时", "这种情况"
    );

    private FollowUpDetector() {
    }

    static boolean isFollowUp(String question) {
        if (question == null) {
            return false;
        }
        String normalized = question.strip();
        if (normalized.isEmpty() || normalized.codePointCount(0, normalized.length()) > 80) {
            return false;
        }
        return FOLLOW_UP_PREFIXES.stream().anyMatch(normalized::startsWith);
    }

    static String withPreviousTopic(String question, String previousGroundedQuestion) {
        if (previousGroundedQuestion == null || previousGroundedQuestion.isBlank()
                || !isFollowUp(question)) {
            return question;
        }
        return previousGroundedQuestion + " " + question;
    }
}
