package com.report.dailyreport;

import com.report.dailyreport.analyzer.TrendAnalyzer;
import com.report.dailyreport.collector.RssCollector;
import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.filter.DeduplicationService;
import com.report.dailyreport.formatter.MarkdownReportFormatter;
import com.report.dailyreport.model.AnalyzedArticle;
import com.report.dailyreport.model.ArticleAnalysis;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.TrendReport;
import com.report.dailyreport.notifier.NotificationDispatcher;
import com.report.dailyreport.ranker.ImportanceRanker;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "report.runner", name = "enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DailyReportRunner implements ApplicationRunner {

    private final RssCollector rssCollector;
    private final DeduplicationService deduplicationService;
    private final ImportanceRanker importanceRanker;
    private final TrendAnalyzer trendAnalyzer;
    private final MarkdownReportFormatter markdownReportFormatter;
    private final NotificationDispatcher notificationDispatcher;
    private final ReportProperties reportProperties;

    @Override
    public void run(ApplicationArguments args) {
        List<com.report.dailyreport.model.CollectedArticle> collectedArticles = rssCollector.collectAll();
        if (collectedArticles.isEmpty()) {
            log.warn("No articles were collected. Report generation stopped.");
            return;
        }

        List<com.report.dailyreport.model.CollectedArticle> deduplicatedArticles =
                deduplicationService.deduplicate(collectedArticles);
        List<RankedArticle> rankedArticles = importanceRanker.rank(deduplicatedArticles, collectedArticles);
        List<RankedArticle> topArticles = rankedArticles.stream()
                .limit(reportProperties.effectiveTopN())
                .toList();

        if (topArticles.isEmpty()) {
            log.warn("No ranked articles available after filtering.");
            return;
        }
        if (topArticles.size() < 3) {
            log.warn("Only {} ranked articles available. Report will be generated with limited items.", topArticles.size());
        }

        List<AnalyzedArticle> analyzedArticles = topArticles.stream()
                .map(this::analyzeSafely)
                .toList();
        TrendReport trendReport = markdownReportFormatter.format(analyzedArticles);
        notificationDispatcher.dispatch(trendReport);
    }

    private AnalyzedArticle analyzeSafely(RankedArticle article) {
        try {
            ArticleAnalysis analysis = trendAnalyzer.analyze(article);
            return new AnalyzedArticle(article, analysis);
        } catch (Exception exception) {
            log.warn("Analysis failed for title='{}'. Using minimal fallback. message={}",
                    article.article().title(), exception.getMessage());
            ArticleAnalysis analysis = new ArticleAnalysis(
                    article.article().summary(),
                    "분석 중 오류가 발생해 최소 요약만 유지했습니다.",
                    "분석기 오류로 상세 reasoning은 생략됐습니다.",
                    "산업 영향은 원문과 점수 기준으로 별도 검토가 필요합니다.",
                    "원문 링크와 로그를 함께 확인하세요.",
                    List.of("오류 로그 확인", "원문 기사 재검토", "다음 실행에서 재시도"),
                    false
            );
            return new AnalyzedArticle(article, analysis);
        }
    }
}
