package com.oa.ai.service;

import com.oa.ai.model.KbDocument;
import com.oa.ai.model.KnowledgeHit;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class KnowledgeBaseService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeBaseService.class);
    private static final double MIN_SCORE = 8.0;
    private static final String SELECT_ENABLED_DOCS = """
            SELECT id, question, answer, keywords, hot, create_time
            FROM kb_doc
            WHERE enabled = 1
            ORDER BY id
            """;

    private final JdbcTemplate jdbcTemplate;
    private final VectorKnowledgeService vectorKnowledgeService;
    private final AtomicReference<List<KbDocument>> documents = new AtomicReference<>(List.of());

    public KnowledgeBaseService(JdbcTemplate jdbcTemplate,
                                VectorKnowledgeService vectorKnowledgeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorKnowledgeService = vectorKnowledgeService;
    }

    @PostConstruct
    public void loadOnStartup() {
        try {
            reload();
        } catch (RuntimeException exception) {
            log.warn("知识库启动加载失败，服务会保持运行: {}", exception.getMessage());
        }
    }

    public synchronized int reload() {
        int retainedCount = documents.get().size();
        try {
            List<KbDocument> loaded = jdbcTemplate.query(SELECT_ENABLED_DOCS, (resultSet, rowNum) -> {
                Timestamp createTime = resultSet.getTimestamp("create_time");
                return new KbDocument(
                        resultSet.getInt("id"),
                        resultSet.getString("question"),
                        resultSet.getString("answer"),
                        resultSet.getString("keywords"),
                        resultSet.getBoolean("hot"),
                        createTime == null ? null : createTime.toLocalDateTime()
                );
            });
            boolean vectorReady = vectorKnowledgeService.rebuild(loaded);
            documents.set(List.copyOf(loaded));
            log.info("知识库加载完成，共 {} 条启用知识，向量索引状态: {}",
                    loaded.size(), vectorReady ? "就绪" : "关键词降级");
            return loaded.size();
        } catch (RuntimeException exception) {
            log.error("知识库重载失败，继续使用原有 {} 条知识", retainedCount, exception);
            throw exception;
        }
    }

    public List<KnowledgeHit> search(String query, int limit) {
        int resultLimit = Math.max(1, limit);
        int candidateLimit = Math.max(8, resultLimit * 2);
        List<KbDocument> snapshot = documents.get();
        List<KnowledgeHit> lexicalHits = lexicalSearch(snapshot, query, candidateLimit);
        Map<Integer, KbDocument> documentsById = new HashMap<>();
        snapshot.forEach(document -> documentsById.put(document.id(), document));

        Map<Integer, Double> fusedScores = new LinkedHashMap<>();
        List<com.oa.ai.model.VectorMatch> vectorMatches =
                vectorKnowledgeService.search(query, candidateLimit);
        for (int rank = 0; rank < vectorMatches.size(); rank++) {
            int documentId = vectorMatches.get(rank).documentId();
            if (documentsById.containsKey(documentId)) {
                fusedScores.merge(documentId, reciprocalRank(rank, 1.2), Double::sum);
            }
        }
        for (int rank = 0; rank < lexicalHits.size(); rank++) {
            int documentId = lexicalHits.get(rank).document().id();
            fusedScores.merge(documentId, reciprocalRank(rank, 1.0), Double::sum);
        }

        return fusedScores.entrySet().stream()
                .map(entry -> new KnowledgeHit(documentsById.get(entry.getKey()), entry.getValue()))
                .filter(hit -> hit.document() != null)
                .sorted(Comparator.comparingDouble(KnowledgeHit::score).reversed())
                .limit(resultLimit)
                .toList();
    }

    private List<KnowledgeHit> lexicalSearch(List<KbDocument> snapshot, String query, int limit) {
        return snapshot.stream()
                .map(document -> new KnowledgeHit(document, KnowledgeScorer.score(query, document)))
                .filter(hit -> hit.score() >= MIN_SCORE)
                .sorted(Comparator.comparingDouble(KnowledgeHit::score).reversed())
                .limit(limit)
                .toList();
    }

    private double reciprocalRank(int zeroBasedRank, double weight) {
        return weight / (60.0 + zeroBasedRank + 1.0);
    }

    public List<String> hotQuestions(int limit) {
        return documents.get().stream()
                .filter(KbDocument::hot)
                .map(KbDocument::question)
                .limit(Math.max(1, limit))
                .toList();
    }

    public int size() {
        return documents.get().size();
    }
}
