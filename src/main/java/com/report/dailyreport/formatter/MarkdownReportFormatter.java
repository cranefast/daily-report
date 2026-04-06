package com.report.dailyreport.formatter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.AnalyzedArticle;
import com.report.dailyreport.model.ArticleAnalysis;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkdownReportFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final ReportProperties reportProperties;
    private final Clock clock;

    public TrendReport format(List<AnalyzedArticle> analyzedArticles) {
        ZoneId zoneId = ZoneId.of(reportProperties.getZoneId());
        LocalDate reportDate = LocalDate.now(zoneId);
        Instant generatedAt = Instant.now(clock);

        StringBuilder markdown = new StringBuilder();
        markdown.append("[").append(reportDate).append(" 최신 트렌드 리포트]\n\n");
        markdown.append("- 생성 시각: ")
                .append(DATE_TIME_FORMATTER.format(generatedAt.atZone(zoneId)))
                .append('\n');
        markdown.append("- 채널: ").append(reportProperties.getChannel()).append('\n');
        markdown.append("- 실행 모드: ").append(reportProperties.isDryRun() ? "dry-run" : "live").append("\n\n");

        markdown.append("## 1. 오늘의 핵심 요약\n\n");
        for (AnalyzedArticle article : analyzedArticles.stream().limit(3).toList()) {
            markdown.append("- [")
                    .append(article.rankedArticle().article().category().getDisplayName())
                    .append("] ")
                    .append(article.rankedArticle().article().title())
                    .append(": ")
                    .append(article.analysis().summary())
                    .append('\n');
        }

        markdown.append("\n## 2. 주요 이슈\n\n");
        for (int index = 0; index < analyzedArticles.size(); index++) {
            AnalyzedArticle analyzedArticle = analyzedArticles.get(index);
            markdown.append("### ").append(index + 1).append(". ")
                    .append(analyzedArticle.rankedArticle().article().title())
                    .append('\n');
            markdown.append("- 카테고리: ").append(analyzedArticle.rankedArticle().article().category().getDisplayName()).append('\n');
            markdown.append("- 출처: [").append(analyzedArticle.rankedArticle().article().sourceName()).append("](")
                    .append(analyzedArticle.rankedArticle().article().link()).append(")\n");
            markdown.append("- 발행 시각: ").append(formatPublishedAt(analyzedArticle, zoneId)).append('\n');
            markdown.append("- 생성 방식: ").append(analyzedArticle.analysis().generatedByAi() ? "OpenAI" : "규칙 기반 fallback").append('\n');
            markdown.append("- 점수 세부: ").append(formatScoreBreakdown(analyzedArticle.rankedArticle().scoreBreakdown())).append('\n');
            markdown.append("- 핵심 요약: ").append(analyzedArticle.analysis().summary()).append('\n');
            markdown.append("- 상세 설명: ").append(analyzedArticle.analysis().detailedExplanation()).append('\n');
            markdown.append("- 중요 이유: ").append(analyzedArticle.analysis().importanceReason()).append('\n');
            markdown.append("- IT/산업 영향: ").append(analyzedArticle.analysis().industryImpact()).append('\n');
            markdown.append("- 실무 시사점: ").append(analyzedArticle.analysis().practicalImplications()).append('\n');
            markdown.append("- 후속 체크 포인트:\n");
            for (String followUpPoint : analyzedArticle.analysis().followUpPoints()) {
                markdown.append("  - ").append(followUpPoint).append('\n');
            }
            markdown.append('\n');
        }

        markdown.append("## 3. IT 종합 인사이트\n\n");
        markdown.append(buildIntegratedInsight(analyzedArticles)).append("\n\n");

        markdown.append("## 4. 관찰 포인트\n\n");
        buildObservationPoints(analyzedArticles).forEach(point -> markdown.append("- ").append(point).append('\n'));

        return new TrendReport(
                reportDate,
                generatedAt,
                markdown.toString(),
                analyzedArticles,
                reportProperties.getChannel(),
                reportProperties.isDryRun()
        );
    }

    private String formatPublishedAt(AnalyzedArticle analyzedArticle, ZoneId zoneId) {
        if (analyzedArticle.rankedArticle().article().publishedAt() == null) {
            return "발행 시각 미확인";
        }
        return DATE_TIME_FORMATTER.format(analyzedArticle.rankedArticle().article().publishedAt().atZone(zoneId));
    }

    private String formatScoreBreakdown(ScoreBreakdown score) {
        return String.format(
                Locale.ROOT,
                "총점 %.2f점 | 최신성 %.2f x %.2f | 키워드 %.2f x %.2f | 출처신뢰도 %.2f x %.2f | 반복언급 %.2f x %.2f (반복 %d건)",
                score.totalScore(),
                score.recencyScore(), score.recencyWeight(),
                score.keywordScore(), score.keywordWeight(),
                score.sourceReliabilityScore(), score.sourceReliabilityWeight(),
                score.repetitionScore(), score.repetitionWeight(),
                score.repetitionCount()
        );
    }

    private String buildIntegratedInsight(List<AnalyzedArticle> analyzedArticles) {
        List<ArticleAnalysis> itAnalyses = analyzedArticles.stream()
                .filter(article -> article.rankedArticle().article().category() == ReportCategory.AI
                        || article.rankedArticle().article().category() == ReportCategory.DEVELOPMENT)
                .map(AnalyzedArticle::analysis)
                .toList();
        if (itAnalyses.isEmpty()) {
            return "오늘 선정된 상위 이슈 중 AI/개발 카테고리가 부족해 별도 IT 종합 인사이트는 제한적으로 제공됩니다.";
        }

        String impactSummary = itAnalyses.stream()
                .map(ArticleAnalysis::industryImpact)
                .filter(Objects::nonNull)
                .distinct()
                .limit(2)
                .collect(Collectors.joining(" "));
        String practicalSummary = itAnalyses.stream()
                .map(ArticleAnalysis::practicalImplications)
                .filter(Objects::nonNull)
                .distinct()
                .limit(2)
                .collect(Collectors.joining(" "));

        return impactSummary + " " + practicalSummary;
    }

    private List<String> buildObservationPoints(List<AnalyzedArticle> analyzedArticles) {
        return analyzedArticles.stream()
                .flatMap(article -> article.analysis().followUpPoints().stream())
                .distinct()
                .limit(6)
                .toList();
    }
}
