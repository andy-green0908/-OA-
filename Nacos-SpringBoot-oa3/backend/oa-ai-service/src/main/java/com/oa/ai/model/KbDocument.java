package com.oa.ai.model;

import java.time.LocalDateTime;

public record KbDocument(
        int id,
        String question,
        String answer,
        String keywords,
        boolean hot,
        LocalDateTime createTime
) {
}
