package com.report.dailyreport.model;

import java.time.Instant;
import java.util.List;

public record CollectedArticle(
        ReportCategory category,
        String sourceName,
        String sourceUrl,
        String title,
        String link,
        Instant publishedAt,
        String summary,
        double sourceReliability,
        List<String> sourceKeywords
) {
    public CollectedArticle {
        title = title == null ? "" : title.trim();
        summary = summary == null ? "" : summary.trim();
        sourceKeywords = sourceKeywords == null ? List.of() : List.copyOf(sourceKeywords);
    }
}
