package com.oa.ai.controller;

import com.oa.ai.common.ApiResponse;
import com.oa.ai.dto.ChatRequest;
import com.oa.ai.dto.ChatResponse;
import com.oa.ai.dto.HealthResponse;
import com.oa.ai.service.AiChatService;
import com.oa.ai.service.ChatSessionService;
import com.oa.ai.service.KnowledgeBaseService;
import com.oa.ai.service.OllamaHealthService;
import com.oa.ai.service.VectorKnowledgeService;
import com.oa.ai.security.EmployeeAuthInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final String STREAM_ERROR_MESSAGE =
            "AI 助手暂时不可用，请确认 Ollama 已启动且 qwen2.5:7b 模型已经安装。";

    private final AiChatService aiChatService;
    private final ChatSessionService chatSessionService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final OllamaHealthService ollamaHealthService;
    private final VectorKnowledgeService vectorKnowledgeService;
    private final Semaphore streamSemaphore;

    public ChatController(AiChatService aiChatService,
                          ChatSessionService chatSessionService,
                          KnowledgeBaseService knowledgeBaseService,
                          OllamaHealthService ollamaHealthService,
                          VectorKnowledgeService vectorKnowledgeService,
                          @Value("${oa.ai.max-concurrent-streams:20}") int maxConcurrentStreams) {
        this.aiChatService = aiChatService;
        this.chatSessionService = chatSessionService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.ollamaHealthService = ollamaHealthService;
        this.vectorKnowledgeService = vectorKnowledgeService;
        this.streamSemaphore = new Semaphore(Math.max(1, maxConcurrentStreams));
    }

    @GetMapping("/health")
    public ApiResponse<HealthResponse> health() {
        OllamaHealthService.OllamaStatus status = ollamaHealthService.status();
        int knowledgeCount = knowledgeBaseService.size();
        return ApiResponse.ok(new HealthResponse(
                status.ollama(),
                status.modelReady(),
                knowledgeCount > 0,
                vectorKnowledgeService.ready(),
                status.model(),
                vectorKnowledgeService.embeddingModel(),
                knowledgeCount,
                vectorKnowledgeService.indexedDocumentCount()
        ));
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<String>> suggestions() {
        return ApiResponse.ok(knowledgeBaseService.hotQuestions(6));
    }

    @PostMapping
    public ApiResponse<ChatResponse> chat(@Valid @RequestBody ChatRequest request,
                                          HttpServletRequest httpRequest) {
        return ApiResponse.ok(aiChatService.chat(employeeSessionId(httpRequest), request.message()));
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Map<String, String>>> stream(
            @Valid @RequestBody ChatRequest request, HttpServletRequest httpRequest) {
        if (!streamSemaphore.tryAcquire()) {
            return Flux.just(ServerSentEvent.<Map<String, String>>builder()
                    .data(Map.of("error", "AI 助手当前繁忙，请稍后再试。"))
                    .build());
        }
        return aiChatService.stream(employeeSessionId(httpRequest), request.message())
                .map(chunk -> ServerSentEvent.<Map<String, String>>builder()
                        .data(Map.of("c", chunk))
                        .build())
                .onErrorResume(exception -> {
                    log.warn("AI 流式回答失败: {}", exception.getMessage());
                    return Flux.just(ServerSentEvent.<Map<String, String>>builder()
                            .data(Map.of("error", STREAM_ERROR_MESSAGE))
                            .build());
                })
                .doFinally(signalType -> streamSemaphore.release());
    }

    @DeleteMapping("/session")
    public ApiResponse<Boolean> clearSession(HttpServletRequest request) {
        return ApiResponse.ok(chatSessionService.clear(employeeSessionId(request)));
    }

    private String employeeSessionId(HttpServletRequest request) {
        Object employeeNumber = request.getAttribute(
                EmployeeAuthInterceptor.EMPLOYEE_NUMBER_ATTRIBUTE);
        if (!(employeeNumber instanceof Integer number)) {
            throw new IllegalStateException("请求缺少已认证员工身份");
        }
        return "emp-" + number;
    }
}
