package com.oa.ai.service;

import com.oa.ai.model.KbDocument;
import com.oa.ai.model.VectorMatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class VectorKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(VectorKnowledgeService.class);
    private static final String KB_ID_METADATA = "kbId";
    private static final String INDEX_VERSION_METADATA = "indexVersion";

    private final VectorStore vectorStore;
    private final double similarityThreshold;
    private final String embeddingModel;
    private final AtomicReference<String> activeVersion = new AtomicReference<>();
    private final AtomicBoolean available = new AtomicBoolean(false);
    private final AtomicInteger indexedDocumentCount = new AtomicInteger(0);

    public VectorKnowledgeService(VectorStore vectorStore,
                                  @Value("${oa.ai.vector-similarity-threshold:0.62}")
                                  double similarityThreshold,
                                  @Value("${spring.ai.ollama.embedding.options.model:bge-m3}")
                                  String embeddingModel) {
        this.vectorStore = vectorStore;
        this.similarityThreshold = Math.max(0.0, Math.min(1.0, similarityThreshold));
        this.embeddingModel = embeddingModel;
    }

    public synchronized boolean rebuild(List<KbDocument> documents) {
        String nextVersion = UUID.randomUUID().toString();
        List<Document> vectorDocuments = documents.stream()
                .map(document -> toVectorDocument(document, nextVersion))
                .toList();
        try {
            if (!vectorDocuments.isEmpty()) {
                vectorStore.add(vectorDocuments);
            }
            activeVersion.set(nextVersion);
            indexedDocumentCount.set(vectorDocuments.size());
            markAvailable();
            removeOldVersions(nextVersion);
            log.info("Qdrant 向量索引构建完成，共 {} 条知识，版本 {}",
                    vectorDocuments.size(), nextVersion);
            return true;
        } catch (RuntimeException exception) {
            markUnavailable("Qdrant 向量索引构建失败，继续使用关键词检索", exception);
            return false;
        }
    }

    public List<VectorMatch> search(String query, int limit) {
        String version = activeVersion.get();
        if (version == null || query == null || query.isBlank()) {
            return List.of();
        }
        try {
            var versionFilter = new FilterExpressionBuilder()
                    .eq(INDEX_VERSION_METADATA, version)
                    .build();
            List<Document> matches = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(query)
                    .topK(Math.max(1, limit))
                    .similarityThreshold(similarityThreshold)
                    .filterExpression(versionFilter)
                    .build());
            markAvailable();
            return matches.stream()
                    .map(this::toVectorMatch)
                    .filter(match -> match.documentId() > 0)
                    .toList();
        } catch (RuntimeException exception) {
            markUnavailable("Qdrant 向量检索失败，已降级为关键词检索", exception);
            return List.of();
        }
    }

    public boolean ready() {
        return available.get() && activeVersion.get() != null;
    }

    public int indexedDocumentCount() {
        return indexedDocumentCount.get();
    }

    public String embeddingModel() {
        return embeddingModel;
    }

    private Document toVectorDocument(KbDocument document, String version) {
        String indexText = "问题：" + document.question()
                + "\n关键词：" + (document.keywords() == null ? "" : document.keywords())
                + "\n答案：" + document.answer();
        return Document.builder()
                .text(indexText)
                .metadata(KB_ID_METADATA, document.id())
                .metadata(INDEX_VERSION_METADATA, version)
                .build();
    }

    private VectorMatch toVectorMatch(Document document) {
        Object id = document.getMetadata().get(KB_ID_METADATA);
        int documentId;
        if (id instanceof Number number) {
            documentId = number.intValue();
        } else {
            try {
                documentId = Integer.parseInt(String.valueOf(id));
            } catch (NumberFormatException exception) {
                documentId = -1;
            }
        }
        return new VectorMatch(documentId,
                document.getScore() == null ? 0.0 : document.getScore());
    }

    private void removeOldVersions(String currentVersion) {
        try {
            var oldVersionFilter = new FilterExpressionBuilder()
                    .ne(INDEX_VERSION_METADATA, currentVersion)
                    .build();
            vectorStore.delete(oldVersionFilter);
        } catch (RuntimeException exception) {
            log.warn("Qdrant 旧向量版本清理失败，不影响当前版本使用: {}", exception.getMessage());
        }
    }

    private void markAvailable() {
        if (available.compareAndSet(false, true)) {
            log.info("Qdrant 向量服务已就绪");
        }
    }

    private void markUnavailable(String message, RuntimeException exception) {
        if (available.getAndSet(false)) {
            log.warn("{}: {}", message, exception.getMessage());
        } else {
            log.debug("{}: {}", message, exception.getMessage());
        }
    }
}
