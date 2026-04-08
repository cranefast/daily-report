package com.report.dailyreport.model;

import java.util.List;

public record CategoryTrendSection(
        ReportCategory category,
        CategoryInsight insight,
        List<ArticleHighlight> articles
) {
    public CategoryTrendSection {
        articles = articles == null ? List.of() : List.copyOf(articles);
    }
}
