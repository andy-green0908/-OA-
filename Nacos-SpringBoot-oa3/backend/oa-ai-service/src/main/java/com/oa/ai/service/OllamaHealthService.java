package com.oa.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class OllamaHealthService {

    private static final Logger log = LoggerFactory.getLogger(OllamaHealthService.class);

    private final RestClient restClient;
    private final String configuredModel;

    public OllamaHealthService(RestClient.Builder restClientBuilder,
                               @Value("${spring.ai.ollama.base-url}") String baseUrl,
                               @Value("${oa.ai.model}") String configuredModel) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(2_000);
        requestFactory.setReadTimeout(3_000);
        this.restClient = restClientBuilder
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
        this.configuredModel = configuredModel;
    }

    public OllamaStatus status() {
        try {
            JsonNode response = restClient.get()
                    .uri("/api/tags")
                    .retrieve()
                    .body(JsonNode.class);
            String availableModel = null;
            if (response != null) {
                availableModel = response.path("models").findValuesAsText("name").stream()
                        .filter(this::sameModel)
                        .findFirst()
                        .orElseGet(() -> response.path("models").findValuesAsText("model").stream()
                                .filter(this::sameModel)
                                .findFirst()
                                .orElse(null));
            }
            boolean modelReady = availableModel != null;
            return new OllamaStatus(true, modelReady, modelReady ? availableModel : configuredModel);
        } catch (Exception exception) {
            log.debug("Ollama 健康检查失败: {}", exception.getMessage());
            return new OllamaStatus(false, false, configuredModel);
        }
    }

    private boolean sameModel(String availableModel) {
        if (configuredModel.equals(availableModel)) {
            return true;
        }
        return !configuredModel.contains(":")
                && (configuredModel + ":latest").equals(availableModel);
    }

    public record OllamaStatus(boolean ollama, boolean modelReady, String model) {
    }
}
