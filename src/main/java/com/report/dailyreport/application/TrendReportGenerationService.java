package com.report.dailyreport.application;

import com.report.dailyreport.analyzer.TrendAnalyzer;
import com.report.dailyreport.collector.RssCollector;
import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.filter.DeduplicationService;
import com.report.dailyreport.formatter.MarkdownReportFormatter;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.TrendReport;
import com.report.dailyreport.ranker.ImportanceRanker;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendReportGenerationService {

    private final RssCollector rssCollector;
    private final DeduplicationService deduplicationService;
    private final ImportanceRanker importanceRanker;
    private final TrendAnalyzer trendAnalyzer;
    private final MarkdownReportFormatter markdownReportFormatter;
    private final ReportProperties reportProperties;

    public Optional<TrendReport> generate() {
        List<CollectedArticle> collectedArticles = rssCollector.collectAll();
        if (collectedArticles.isEmpty()) {
            log.warn("No articles were collected. Report generation stopped.");
            return Optional.empty();
        }

        List<CollectedArticle> deduplicatedArticles = deduplicationService.deduplicate(collectedArticles);
        List<RankedArticle> rankedArticles = importanceRanker.rank(deduplicatedArticles, collectedArticles);
        Map<ReportCategory, List<RankedArticle>> selectedArticles = selectTopArticlesByCategory(rankedArticles);

        long totalSelected = selectedArticles.values().stream().mapToLong(List::size).sum();
        if (totalSelected == 0) {
            log.warn("No ranked articles available after filtering.");
            return Optional.empty();
        }

        List<CategoryTrendSection> categorySections = trendAnalyzer.analyze(selectedArticles);
        return Optional.of(markdownReportFormatter.format(categorySections));
    }

    private Map<ReportCategory, List<RankedArticle>> selectTopArticlesByCategory(List<RankedArticle> rankedArticles) {
        int topNPerCategory = reportProperties.effectiveTopN();
        int maxArticlesPerSource = reportProperties.effectiveMaxArticlesPerSource();
        EnumMap<ReportCategory, List<RankedArticle>> selected = new EnumMap<>(ReportCategory.class);

        for (ReportCategory category : ReportCategory.values()) {
            List<RankedArticle> topArticles = new java.util.ArrayList<>();
            Map<String, Integer> articlesPerSource = new HashMap<>();

            for (RankedArticle rankedArticle : rankedArticles) {
                if (rankedArticle.article().category() != category) {
                    continue;
                }

                String sourceName = rankedArticle.article().sourceName();
                int currentCount = articlesPerSource.getOrDefault(sourceName, 0);
                if (currentCount >= maxArticlesPerSource) {
                    continue;
                }

                topArticles.add(rankedArticle);
                articlesPerSource.put(sourceName, currentCount + 1);
                if (topArticles.size() >= topNPerCategory) {
                    break;
                }
            }
            selected.put(category, topArticles);

            if (topArticles.isEmpty()) {
                log.warn("No ranked articles available for category={}", category);
                continue;
            }
            if (topArticles.size() < topNPerCategory) {
                log.warn("Only {} ranked articles available for category={}. target={}",
                        topArticles.size(), category, topNPerCategory);
            }
        }

        return selected;
    }
}
