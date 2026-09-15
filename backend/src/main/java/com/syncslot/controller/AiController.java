package com.syncslot.controller;

import com.syncslot.dto.AiExplainRequest;
import com.syncslot.dto.AiExplainResponse;
import com.syncslot.dto.AiSummaryRequest;
import com.syncslot.dto.AiSummaryResponse;
import com.syncslot.service.AiExplainService;
import com.syncslot.service.AiSummaryService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AI narration endpoints. They only explain/summarise decisions made by the Java
 * scheduling engine; they never influence them.
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiExplainService aiExplainService;
    private final AiSummaryService aiSummaryService;

    public AiController(AiExplainService aiExplainService, AiSummaryService aiSummaryService) {
        this.aiExplainService = aiExplainService;
        this.aiSummaryService = aiSummaryService;
    }

    @PostMapping("/explain")
    public AiExplainResponse explain(@RequestBody AiExplainRequest request) {
        return aiExplainService.explain(request);
    }

    @PostMapping("/summary")
    public AiSummaryResponse summary(@RequestBody AiSummaryRequest request) {
        return aiSummaryService.summarize(request);
    }
}
