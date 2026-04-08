package com.report.dailyreport.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record TrendReport(
        LocalDate reportDate,
        Instant generatedAt,
        String markdown,
        List<CategoryTrendSection> sections,
        NotificationChannel channel,
        boolean dryRun
) {
    public TrendReport {
        sections = sections == null ? List.of() : List.copyOf(sections);
    }
}
