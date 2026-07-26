package com.project.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class AmapRestTemplateConfig {

    private final AmapConfig amapConfig;

    public AmapRestTemplateConfig(AmapConfig amapConfig) {
        this.amapConfig = amapConfig;
    }

    @Bean("amapRestTemplate")
    public RestTemplate amapRestTemplate(RestTemplateBuilder builder) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(amapConfig.getConnectTimeout()));
        factory.setReadTimeout(Duration.ofMillis(amapConfig.getReadTimeout()));
        return new RestTemplate(factory);
    }
}