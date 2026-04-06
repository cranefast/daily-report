package com.report.dailyreport.analyzer;

import com.report.dailyreport.model.ArticleAnalysis;
import com.report.dailyreport.model.RankedArticle;

public interface TrendAnalyzer {

    ArticleAnalysis analyze(RankedArticle article);
}
