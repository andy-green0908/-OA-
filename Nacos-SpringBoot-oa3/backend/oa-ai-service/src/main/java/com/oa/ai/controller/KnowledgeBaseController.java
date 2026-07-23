package com.oa.ai.controller;

import com.oa.ai.common.ApiResponse;
import com.oa.ai.service.KnowledgeBaseService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kb")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(KnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    @PostMapping("/reload")
    public ApiResponse<Integer> reload() {
        return ApiResponse.ok(knowledgeBaseService.reload());
    }
}
