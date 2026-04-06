package com.report.dailyreport.model;

import java.util.List;

public record ArticleAnalysis(
        String summary,
        String detailedExplanation,
        String importanceReason,
        String industryImpact,
        String practicalImplications,
        List<String> followUpPoints,
        boolean generatedByAi
) {
    public ArticleAnalysis {
        followUpPoints = followUpPoints == null ? List.of() : List.copyOf(followUpPoints);
    }
}
