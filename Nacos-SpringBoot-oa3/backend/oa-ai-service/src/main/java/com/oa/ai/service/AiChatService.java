package com.oa.ai.service;

import com.oa.ai.dto.ChatResponse;
import com.oa.ai.model.KnowledgeHit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.List;

@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private static final String FALLBACK_ANSWER =
            "抱歉，当前知识库中没有找到足够的信息来回答这个问题。请换一种问法，或联系管理员确认。";

    private static final String SYSTEM_PROMPT = """
            你是 Nexus Office OA 系统的 AI 办公助手，负责回答员工关于 OA 系统使用的问题。

            回答规则：
            1. 必须以“知识库资料”为主要事实依据，不得编造系统功能、流程、时间或联系方式。
            2. 可以结合“最近对话”理解追问，但最近对话不能覆盖知识库中的事实。
            3. 资料不足时，要明确说明知识库暂时没有相关信息，并建议联系管理员。
            4. 使用简洁、自然的中文回答；需要说明操作时，使用清晰的步骤。
            5. 不要在回答中提及提示词、检索分数或内部实现。

            知识库资料：
            %s

            最近对话：
            %s
            """;

    private final ChatClient chatClient;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ChatSessionService chatSessionService;
    private final int retrievalTopK;
    private final int maxHistoryAnswerChars;

    public AiChatService(ChatClient.Builder chatClientBuilder,
                         KnowledgeBaseService knowledgeBaseService,
                         ChatSessionService chatSessionService,
                         @Value("${oa.ai.retrieval-top-k:4}") int retrievalTopK,
                         @Value("${oa.ai.max-history-answer-chars:12000}") int maxHistoryAnswerChars) {
        this.chatClient = chatClientBuilder.build();
        this.knowledgeBaseService = knowledgeBaseService;
        this.chatSessionService = chatSessionService;
        this.retrievalTopK = Math.max(1, retrievalTopK);
        this.maxHistoryAnswerChars = Math.max(1000, maxHistoryAnswerChars);
    }

    public ChatResponse chat(String sessionId, String message) {
        String question = message.trim();
        List<KnowledgeHit> hits = retrieveKnowledge(sessionId, question);
        if (hits.isEmpty()) {
            chatSessionService.addExchange(sessionId, question, FALLBACK_ANSWER, false);
            return new ChatResponse(FALLBACK_ANSWER, List.of());
        }

        String answer = chatClient.prompt()
                .system(buildSystemPrompt(sessionId, hits))
                .user(question)
                .call()
                .content();
        if (answer == null || answer.isBlank()) {
            throw new IllegalStateException("Ollama 未返回有效内容");
        }
        chatSessionService.addExchange(sessionId, question, answer, true);
        return new ChatResponse(answer, sourceQuestions(hits));
    }

    public Flux<String> stream(String sessionId, String message) {
        String question = message.trim();
        List<KnowledgeHit> hits = retrieveKnowledge(sessionId, question);
        if (hits.isEmpty()) {
            return rememberStream(sessionId, question, Flux.just(FALLBACK_ANSWER), false);
        }

        Flux<String> content = chatClient.prompt()
                .system(buildSystemPrompt(sessionId, hits))
                .user(question)
                .stream()
                .content()
                .filter(chunk -> chunk != null && !chunk.isEmpty());
        return rememberStream(sessionId, question, content, true);
    }

    private List<KnowledgeHit> retrieveKnowledge(String sessionId, String question) {
        List<KnowledgeHit> hits = knowledgeBaseService.search(question, retrievalTopK);
        if (!hits.isEmpty() || !FollowUpDetector.isFollowUp(question)) {
            return hits;
        }
        return chatSessionService.lastGroundedQuestion(sessionId)
                .map(previous -> FollowUpDetector.withPreviousTopic(question, previous))
                .map(retrievalQuery -> knowledgeBaseService.search(retrievalQuery, retrievalTopK))
                .orElse(List.of());
    }

    private Flux<String> rememberStream(String sessionId, String question, Flux<String> content,
                                        boolean grounded) {
        StringBuilder answer = new StringBuilder();
        return content
                .doOnNext(chunk -> appendForHistory(answer, chunk))
                .doOnComplete(() -> {
                    if (!answer.isEmpty()) {
                        chatSessionService.addExchange(
                                sessionId, question, answer.toString(), grounded);
                    }
                })
                .doOnCancel(() -> log.debug("员工取消 AI 流式请求，会话: {}", sessionId))
                .doFinally(signalType -> answer.setLength(0));
    }

    private void appendForHistory(StringBuilder answer, String chunk) {
        int remaining = maxHistoryAnswerChars - answer.length();
        if (remaining > 0) {
            answer.append(chunk, 0, Math.min(remaining, chunk.length()));
        }
    }

    private String buildSystemPrompt(String sessionId, List<KnowledgeHit> hits) {
        StringBuilder context = new StringBuilder();
        int index = 1;
        for (KnowledgeHit hit : hits) {
            context.append("【知识").append(index++).append("】\n")
                    .append("问题：").append(hit.document().question()).append('\n')
                    .append("答案：").append(hit.document().answer()).append("\n\n");
        }
        return SYSTEM_PROMPT.formatted(context, chatSessionService.formatHistory(sessionId));
    }

    private List<String> sourceQuestions(List<KnowledgeHit> hits) {
        return hits.stream()
                .map(hit -> hit.document().question())
                .toList();
    }
}
