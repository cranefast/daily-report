package com.report.dailyreport.application;

import com.report.dailyreport.analyzer.TrendAnalyzer;
import com.report.dailyreport.collector.RssCollector;
import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.filter.DeduplicationService;
import com.report.dailyreport.formatter.MarkdownReportFormatter;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import com.report.dailyreport.ranker.ImportanceRanker;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TrendReportGenerationServiceTest {

    @Test
    void returnsEmptyWhenNoArticlesAreCollected() {
        RssCollector rssCollector = mock(RssCollector.class);
        DeduplicationService deduplicationService = mock(DeduplicationService.class);
        ImportanceRanker importanceRanker = mock(ImportanceRanker.class);
        TrendAnalyzer trendAnalyzer = mock(TrendAnalyzer.class);
        MarkdownReportFormatter formatter = mock(MarkdownReportFormatter.class);
        ReportProperties reportProperties = new ReportProperties();
        TrendReportGenerationService service = new TrendReportGenerationService(
                rssCollector, deduplicationService, importanceRanker, trendAnalyzer, formatter, reportProperties
        );
        when(rssCollector.collectAll()).thenReturn(List.of());

        Optional<TrendReport> result = service.generate();

        assertThat(result).isEmpty();
    }

    @Test
    void formatsTopRankedArticlesPerCategory() {
        RssCollector rssCollector = mock(RssCollector.class);
        DeduplicationService deduplicationService = mock(DeduplicationService.class);
        ImportanceRanker importanceRanker = mock(ImportanceRanker.class);
        TrendAnalyzer trendAnalyzer = mock(TrendAnalyzer.class);
        MarkdownReportFormatter formatter = mock(MarkdownReportFormatter.class);
        ReportProperties reportProperties = new ReportProperties();
        reportProperties.setTopN(3);
        TrendReportGenerationService service = new TrendReportGenerationService(
                rssCollector, deduplicationService, importanceRanker, trendAnalyzer, formatter, reportProperties
        );

        List<CollectedArticle> collected = List.of(
                sampleArticle(ReportCategory.AI, "AI-1"),
                sampleArticle(ReportCategory.AI, "AI-2"),
                sampleArticle(ReportCategory.AI, "AI-3"),
                sampleArticle(ReportCategory.AI, "AI-4"),
                sampleArticle(ReportCategory.DEVELOPMENT, "DEV-1"),
                sampleArticle(ReportCategory.DEVELOPMENT, "DEV-2"),
                sampleArticle(ReportCategory.DEVELOPMENT, "DEV-3"),
                sampleArticle(ReportCategory.DEVELOPMENT, "DEV-4"),
                sampleArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-1"),
                sampleArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-2"),
                sampleArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-3"),
                sampleArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-4"),
                sampleArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-1"),
                sampleArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-2"),
                sampleArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-3"),
                sampleArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-4"),
                sampleArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-1"),
                sampleArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-2"),
                sampleArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-3"),
                sampleArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-4")
        );
        List<RankedArticle> ranked = List.of(
                sampleRankedArticle(ReportCategory.AI, "AI-1", 98),
                sampleRankedArticle(ReportCategory.DEVELOPMENT, "DEV-1", 97),
                sampleRankedArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-1", 96),
                sampleRankedArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-1", 95),
                sampleRankedArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-1", 94),
                sampleRankedArticle(ReportCategory.AI, "AI-2", 94),
                sampleRankedArticle(ReportCategory.DEVELOPMENT, "DEV-2", 93),
                sampleRankedArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-2", 92),
                sampleRankedArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-2", 91),
                sampleRankedArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-2", 90),
                sampleRankedArticle(ReportCategory.AI, "AI-3", 89),
                sampleRankedArticle(ReportCategory.DEVELOPMENT, "DEV-3", 88),
                sampleRankedArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-3", 87),
                sampleRankedArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-3", 86),
                sampleRankedArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-3", 85),
                sampleRankedArticle(ReportCategory.AI, "AI-4", 84),
                sampleRankedArticle(ReportCategory.DEVELOPMENT, "DEV-4", 83),
                sampleRankedArticle(ReportCategory.KOREA_REAL_ESTATE, "KR-RE-4", 82),
                sampleRankedArticle(ReportCategory.KOREA_ECONOMY, "KR-EC-4", 81),
                sampleRankedArticle(ReportCategory.GLOBAL_ECONOMY, "GL-EC-4", 80)
        );
        List<CategoryTrendSection> sections = List.of(
                new CategoryTrendSection(ReportCategory.AI, sampleInsight(), List.of()),
                new CategoryTrendSection(ReportCategory.DEVELOPMENT, sampleInsight(), List.of()),
                new CategoryTrendSection(ReportCategory.KOREA_REAL_ESTATE, sampleInsight(), List.of()),
                new CategoryTrendSection(ReportCategory.KOREA_ECONOMY, sampleInsight(), List.of()),
                new CategoryTrendSection(ReportCategory.GLOBAL_ECONOMY, sampleInsight(), List.of())
        );

        when(rssCollector.collectAll()).thenReturn(collected);
        when(deduplicationService.deduplicate(collected)).thenReturn(collected);
        when(importanceRanker.rank(collected, collected)).thenReturn(ranked);
        when(trendAnalyzer.analyze(anyMap())).thenReturn(sections);

        TrendReport report = new TrendReport(
                LocalDate.of(2026, 4, 7),
                Instant.parse("2026-04-07T01:00:00Z"),
                "markdown",
                sections,
                NotificationChannel.EMAIL,
                true
        );
        when(formatter.format(anyList())).thenReturn(report);

        Optional<TrendReport> result = service.generate();

        assertThat(result).contains(report);
        verify(trendAnalyzer).analyze(argThat(this::containsTopThreePerCategory));
        verify(formatter).format(sections);
    }

    @Test
    void limitsArticlesPerSourceWithinCategory() {
        RssCollector rssCollector = mock(RssCollector.class);
        DeduplicationService deduplicationService = mock(DeduplicationService.class);
        ImportanceRanker importanceRanker = mock(ImportanceRanker.class);
        TrendAnalyzer trendAnalyzer = mock(TrendAnalyzer.class);
        MarkdownReportFormatter formatter = mock(MarkdownReportFormatter.class);
        ReportProperties reportProperties = new ReportProperties();
        reportProperties.setTopN(3);
        reportProperties.setMaxArticlesPerSource(1);
        TrendReportGenerationService service = new TrendReportGenerationService(
                rssCollector, deduplicationService, importanceRanker, trendAnalyzer, formatter, reportProperties
        );

        List<CollectedArticle> collected = List.of(sampleArticle(ReportCategory.AI, "AI-1"));
        List<RankedArticle> ranked = List.of(
                sampleRankedArticle(ReportCategory.AI, "AI-1", 98, "same-source"),
                sampleRankedArticle(ReportCategory.AI, "AI-2", 97, "same-source"),
                sampleRankedArticle(ReportCategory.AI, "AI-3", 96, "other-source"),
                sampleRankedArticle(ReportCategory.AI, "AI-4", 95, "third-source")
        );
        List<CategoryTrendSection> sections = List.of(
                new CategoryTrendSection(ReportCategory.AI, sampleInsight(), List.of())
        );

        when(rssCollector.collectAll()).thenReturn(collected);
        when(deduplicationService.deduplicate(collected)).thenReturn(collected);
        when(importanceRanker.rank(collected, collected)).thenReturn(ranked);
        when(trendAnalyzer.analyze(anyMap())).thenReturn(sections);
        when(formatter.format(anyList())).thenReturn(new TrendReport(
                LocalDate.of(2026, 4, 7),
                Instant.parse("2026-04-07T01:00:00Z"),
                "markdown",
                sections,
                NotificationChannel.EMAIL,
                true
        ));

        service.generate();

        verify(trendAnalyzer).analyze(argThat(selected ->
                titles(selected.get(ReportCategory.AI)).equals(List.of("AI-1", "AI-3", "AI-4"))
        ));
    }

    private boolean containsTopThreePerCategory(Map<ReportCategory, List<RankedArticle>> selected) {
        return titles(selected.get(ReportCategory.AI)).equals(List.of("AI-1", "AI-2", "AI-3"))
                && titles(selected.get(ReportCategory.DEVELOPMENT)).equals(List.of("DEV-1", "DEV-2", "DEV-3"))
                && titles(selected.get(ReportCategory.KOREA_REAL_ESTATE)).equals(List.of("KR-RE-1", "KR-RE-2", "KR-RE-3"))
                && titles(selected.get(ReportCategory.KOREA_ECONOMY)).equals(List.of("KR-EC-1", "KR-EC-2", "KR-EC-3"))
                && titles(selected.get(ReportCategory.GLOBAL_ECONOMY)).equals(List.of("GL-EC-1", "GL-EC-2", "GL-EC-3"));
    }

    private List<String> titles(List<RankedArticle> rankedArticles) {
        return rankedArticles.stream()
                .map(rankedArticle -> rankedArticle.article().title())
                .toList();
    }

    private CollectedArticle sampleArticle(ReportCategory category, String title) {
        return new CollectedArticle(
                category,
                "source-" + title,
                "https://source.example/" + title + "/rss.xml",
                title,
                "https://source.example/" + title,
                Instant.parse("2026-04-07T00:00:00Z"),
                title + " summary",
                0.95,
                List.of("keyword")
        );
    }

    private RankedArticle sampleRankedArticle(ReportCategory category, String title, double totalScore) {
        return sampleRankedArticle(category, title, totalScore, "source-" + title);
    }

    private RankedArticle sampleRankedArticle(ReportCategory category, String title, double totalScore, String sourceName) {
        return new RankedArticle(
                new CollectedArticle(
                        category,
                        sourceName,
                        "https://source.example/" + sourceName + "/rss.xml",
                        title,
                        "https://source.example/" + title,
                        Instant.parse("2026-04-07T00:00:00Z"),
                        title + " summary",
                        0.95,
                        List.of("keyword")
                ),
                new ScoreBreakdown(totalScore, 0.9, 0.8, 0.95, 0.2, 0.35, 0.30, 0.20, 0.15, 1)
        );
    }

    private CategoryInsight sampleInsight() {
        return new CategoryInsight(
                "summary",
                "detail",
                "impact",
                "practical",
                List.of("a", "b", "c"),
                true
        );
    }
}
