package com.oa.ai.service;

import com.oa.ai.model.KbDocument;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

final class KnowledgeScorer {

    private static final Set<String> GENERIC_BIGRAMS = Set.of(
            "怎么", "如何", "什么", "哪些", "是否", "可以", "能否", "需要",
            "时候", "系统", "员工", "公司", "问题", "查看", "进行", "我的"
    );

    private KnowledgeScorer() {
    }

    static double score(String input, KbDocument document) {
        String query = normalize(input);
        String question = normalize(document.question());
        if (query.isEmpty() || question.isEmpty()) {
            return 0;
        }

        boolean exactMatch = query.equals(question);
        boolean containmentMatch = query.contains(question) || question.contains(query);
        double keywordScore = keywordScore(query, document.keywords());
        Set<String> queryBigrams = meaningfulBigrams(query);
        Set<String> questionBigrams = meaningfulBigrams(question);

        // Generic wording such as "怎么" is not enough to establish business relevance.
        if (!exactMatch && keywordScore == 0 && !hasOverlap(queryBigrams, questionBigrams)) {
            return 0;
        }

        double score = 0;
        if (exactMatch) {
            score += 100;
        } else if (containmentMatch) {
            int shortLength = Math.min(codePointLength(query), codePointLength(question));
            int longLength = Math.max(codePointLength(query), codePointLength(question));
            score += 20 + 55.0 * shortLength / longLength;
        }

        score += bigramScore(queryBigrams, questionBigrams, 35, 10);
        score += keywordScore;
        score += bigramScore(queryBigrams, meaningfulBigrams(normalize(document.answer())), 5, 2);
        return score;
    }

    private static double keywordScore(String query, String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return 0;
        }
        double score = 0;
        for (String keyword : keywords.split("[,，;；\\s]+")) {
            String normalizedKeyword = normalize(keyword);
            if (normalizedKeyword.length() >= 2
                    && (query.contains(normalizedKeyword) || normalizedKeyword.contains(query))) {
                score += 20;
            }
        }
        return Math.min(score, 40);
    }

    private static double bigramScore(Set<String> queryBigrams, Set<String> candidateBigrams,
                                      double queryWeight, double candidateWeight) {
        if (queryBigrams.isEmpty() || candidateBigrams.isEmpty()) {
            return 0;
        }
        int overlap = 0;
        for (String bigram : queryBigrams) {
            if (candidateBigrams.contains(bigram)) {
                overlap++;
            }
        }
        return queryWeight * overlap / queryBigrams.size()
                + candidateWeight * overlap / candidateBigrams.size();
    }

    private static boolean hasOverlap(Set<String> left, Set<String> right) {
        for (String value : left) {
            if (right.contains(value)) {
                return true;
            }
        }
        return false;
    }

    private static Set<String> meaningfulBigrams(String text) {
        Set<String> result = bigrams(text);
        result.removeAll(GENERIC_BIGRAMS);
        return result;
    }

    private static Set<String> bigrams(String text) {
        int[] codePoints = text.codePoints().toArray();
        Set<String> result = new HashSet<>();
        for (int i = 0; i + 1 < codePoints.length; i++) {
            result.add(new String(codePoints, i, 2));
        }
        return result;
    }

    private static int codePointLength(String value) {
        return value.codePointCount(0, value.length());
    }

    private static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder normalized = new StringBuilder();
        value.toLowerCase(Locale.ROOT).codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(normalized::appendCodePoint);
        return normalized.toString();
    }
}
