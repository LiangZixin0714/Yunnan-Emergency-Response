package com.project.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "amap")
public class AmapConfig {

    private String apiKey;
    private String geocodeUrl;
    private int connectTimeout = 3000;
    private int readTimeout = 5000;

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getGeocodeUrl() {
        return geocodeUrl;
    }

    public void setGeocodeUrl(String geocodeUrl) {
        this.geocodeUrl = geocodeUrl;
    }

    public int getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }
}