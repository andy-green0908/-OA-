package com.oa.ai.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oa.ai.dto.ChatResponse;
import com.oa.ai.model.KnowledgeHit;
import org.junit.jupiter.api.Test;
import org.assertj.core.api.SoftAssertions;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = "spring.ai.vectorstore.qdrant.collection-name=nexus_office_kb_acceptance"
)
class LiveRagAcceptanceIT {

    @Autowired
    private KnowledgeBaseService knowledgeBaseService;

    @Autowired
    private VectorKnowledgeService vectorKnowledgeService;

    @Autowired
    private AiChatService aiChatService;

    @Autowired
    private ChatSessionService chatSessionService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private VectorStore vectorStore;

    @Test
    void mysqlKnowledgeAndQdrantIndexAreConsistent() {
        assertThat(knowledgeBaseService.size()).isPositive();
        assertThat(vectorKnowledgeService.ready()).isTrue();
        assertThat(vectorKnowledgeService.indexedDocumentCount())
                .isEqualTo(knowledgeBaseService.size());
        assertThat(vectorStore.getNativeClient()).isPresent();
    }

    @Test
    void retrievalAcceptanceCasesMatchExpectedKnowledge() throws IOException {
        SoftAssertions softly = new SoftAssertions();
        for (RetrievalCase acceptanceCase : loadCases()) {
            List<KnowledgeHit> hits = knowledgeBaseService.search(acceptanceCase.question(), 4);
            if (acceptanceCase.expectedQuestion() == null) {
                softly.assertThat(hits)
                        .as("[%s] 无关问题不应命中知识: %s",
                                acceptanceCase.type(), acceptanceCase.question())
                        .isEmpty();
                continue;
            }
            softly.assertThat(hits)
                    .as("[%s] 问题应命中知识: %s",
                            acceptanceCase.type(), acceptanceCase.question())
                    .isNotEmpty();
            if (!hits.isEmpty()) {
                softly.assertThat(hits.getFirst().document().question())
                        .as("[%s] 问题的第一条知识不正确: %s",
                                acceptanceCase.type(), acceptanceCase.question())
                        .isEqualTo(acceptanceCase.expectedQuestion());
            }
        }
        softly.assertAll();
    }

    @Test
    void semanticQuestionCompletesRealRagAnswer() {
        String sessionId = "live-rag-acceptance";
        try {
            ChatResponse response = aiChatService.chat(
                    sessionId, "人在公司外面还能完成出勤登记吗");

            assertThat(response.answer()).isNotBlank();
            assertThat(response.sources()).contains("可以异地签到吗");
        } finally {
            chatSessionService.clear(sessionId);
        }
    }

    private List<RetrievalCase> loadCases() throws IOException {
        ClassPathResource resource = new ClassPathResource("rag-acceptance-cases.json");
        try (InputStream input = resource.getInputStream()) {
            return objectMapper.readValue(input, new TypeReference<>() {
            });
        }
    }

    private record RetrievalCase(String type, String question, String expectedQuestion) {
    }
}
