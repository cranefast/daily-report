package com.report.dailyreport.model;

public record RankedArticle(
        CollectedArticle article,
        ScoreBreakdown scoreBreakdown
) {
}
