package com.project.dto.ai;

import java.math.BigDecimal;

public class CitationItem {

    private String citationId;
    private String sourceText;
    private String sourceUrl;
    private BigDecimal relevanceScore;

    public String getCitationId() {
        return citationId;
    }

    public void setCitationId(String citationId) {
        this.citationId = citationId;
    }

    public String getSourceText() {
        return sourceText;
    }

    public void setSourceText(String sourceText) {
        this.sourceText = sourceText;
    }

    public String getSourceUrl() {
        return sourceUrl;
    }

    public void setSourceUrl(String sourceUrl) {
        this.sourceUrl = sourceUrl;
    }

    public BigDecimal getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(BigDecimal relevanceScore) {
        this.relevanceScore = relevanceScore;
    }
}