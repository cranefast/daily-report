package com.report.dailyreport.model;

public record AnalyzedArticle(
        RankedArticle rankedArticle,
        ArticleAnalysis analysis
) {
}
