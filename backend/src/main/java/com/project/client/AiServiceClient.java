package com.project.client;

import com.project.dto.ai.AnalyzeRequest;
import com.project.dto.ai.AnalyzeResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "ai-service", url = "${ai.service.url:http://localhost:8002}")
public interface AiServiceClient {

    @PostMapping("/ai/analyze")
    AnalyzeResponse analyze(@RequestBody AnalyzeRequest request);
}