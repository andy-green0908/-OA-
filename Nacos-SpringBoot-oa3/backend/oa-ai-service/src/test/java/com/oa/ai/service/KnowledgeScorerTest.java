package com.oa.ai.service;

import com.oa.ai.model.KbDocument;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class KnowledgeScorerTest {

    private final KbDocument attendance = new KbDocument(
            1,
            "怎么签到打卡",
            "进入签到页面后点击签到按钮。",
            "签到,打卡,考勤",
            true,
            null
    );

    @Test
    void exactQuestionGetsHighestScore() {
        double score = KnowledgeScorer.score("怎么签到打卡", attendance);

        assertThat(score).isGreaterThan(100);
    }

    @Test
    void keywordCanMatchARephrasedQuestion() {
        double score = KnowledgeScorer.score("我应该如何进行打卡？", attendance);

        assertThat(score).isGreaterThanOrEqualTo(8);
    }

    @Test
    void unrelatedQuestionDoesNotMatch() {
        double score = KnowledgeScorer.score("如何修改银行卡", attendance);

        assertThat(score).isLessThan(8);
    }

    @Test
    void genericQuestionWordDoesNotMatchAnUnrelatedShortQuestion() {
        KbDocument leave = new KbDocument(
                12,
                "怎么请假",
                "进入请假页面填写申请。",
                "请假,休假,请假流程",
                true,
                null
        );

        double score = KnowledgeScorer.score("怎么制造量子火箭？", leave);

        assertThat(score).isZero();
    }
}
