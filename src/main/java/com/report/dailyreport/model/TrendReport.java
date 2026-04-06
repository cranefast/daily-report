package com.report.dailyreport.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TrendReport(
        LocalDate reportDate,
        Instant generatedAt,
        String markdown,
        List<AnalyzedArticle> articles,
        NotificationChannel channel,
        boolean dryRun
) {
    public TrendReport {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
