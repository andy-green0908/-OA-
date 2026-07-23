package com.oa.ai.dto;

public record HealthResponse(
        boolean ollama,
        boolean modelReady,
        boolean ragReady,
        boolean vectorReady,
        String model,
        String embeddingModel,
        int knowledgeCount,
        int vectorCount
) {
}
