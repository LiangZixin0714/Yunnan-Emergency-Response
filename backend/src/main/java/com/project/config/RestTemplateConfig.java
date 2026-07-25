package com.project.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestTemplateConfig {

    private final AiServiceConfig aiServiceConfig;

    public RestTemplateConfig(AiServiceConfig aiServiceConfig) {
        this.aiServiceConfig = aiServiceConfig;
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(aiServiceConfig.getConnectTimeout()));
        factory.setReadTimeout(Duration.ofMillis(aiServiceConfig.getReadTimeout()));

        RestTemplate restTemplate = new RestTemplate(factory);
        restTemplate.getInterceptors().add(apiKeyInterceptor());
        return restTemplate;
    }

    private ClientHttpRequestInterceptor apiKeyInterceptor() {
        return (request, body, execution) -> {
            request.getHeaders().add("X-API-Key", aiServiceConfig.getApiKey());
            return execution.execute(request, body);
        };
    }
}