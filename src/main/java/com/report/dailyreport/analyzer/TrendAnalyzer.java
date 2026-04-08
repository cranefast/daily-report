package com.report.dailyreport.analyzer;

import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import java.util.List;
import java.util.Map;

public interface TrendAnalyzer {

    List<CategoryTrendSection> analyze(Map<ReportCategory, List<RankedArticle>> articlesByCategory);
}
