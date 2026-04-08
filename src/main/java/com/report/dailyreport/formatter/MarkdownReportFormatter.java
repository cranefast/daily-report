package com.report.dailyreport.formatter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MarkdownReportFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final ReportProperties reportProperties;
    private final Clock clock;

    public TrendReport format(List<CategoryTrendSection> categorySections) {
        ZoneId zoneId = ZoneId.of(reportProperties.getZoneId());
        Instant generatedAt = Instant.now(clock);
        LocalDate reportDate = generatedAt.atZone(zoneId).toLocalDate();

        StringBuilder markdown = new StringBuilder();
        markdown.append("[").append(reportDate).append(" 최신 트렌드 리포트]\n\n");
        markdown.append("- 생성 시각: ")
                .append(DATE_TIME_FORMATTER.format(generatedAt.atZone(zoneId)))
                .append('\n');
        markdown.append("- 채널: ").append(reportProperties.getChannel()).append('\n');
        markdown.append("- 실행 모드: ").append(reportProperties.isDryRun() ? "dry-run" : "live").append("\n\n");

        markdown.append("## 1. 분야별 핵심 요약\n\n");
        for (CategoryTrendSection section : categorySections) {
            markdown.append("- [")
                    .append(section.category().getDisplayName())
                    .append("] ")
                    .append(section.insight().summary())
                    .append('\n');
        }

        markdown.append("\n## 2. 분야별 상세 리포트 및 중요 뉴스\n\n");
        for (CategoryTrendSection section : categorySections) {
            markdown.append("### ").append(section.category().getDisplayName()).append('\n');
            markdown.append("- 선정 기사 수: ")
                    .append(section.articles().size())
                    .append("건 / 목표 ")
                    .append(reportProperties.effectiveTopN())
                    .append("건\n");
            markdown.append("- 생성 방식: ")
                    .append(section.insight().generatedByAi() ? "OpenAI" : "규칙 기반 fallback")
                    .append('\n');
            markdown.append("- 핵심 인사이트: ").append(section.insight().summary()).append('\n');
            markdown.append("- 상세 리포트: ").append(section.insight().detailedReport()).append('\n');
            markdown.append("- 산업 영향: ").append(section.insight().industryImpact()).append('\n');
            markdown.append("- 실무/시장 시사점: ").append(section.insight().practicalImplications()).append('\n');
            markdown.append("- 후속 체크 포인트:\n");
            for (String followUpPoint : section.insight().followUpPoints()) {
                markdown.append("  - ").append(followUpPoint).append('\n');
            }
            markdown.append("- 중요 뉴스 링크:\n");
            if (section.articles().isEmpty()) {
                markdown.append("  - 오늘은 상위 기사 확보가 부족했습니다.\n\n");
                continue;
            }
            for (int index = 0; index < section.articles().size(); index++) {
                ArticleHighlight articleHighlight = section.articles().get(index);
                markdown.append("  ")
                        .append(index + 1)
                        .append(". [")
                        .append(articleHighlight.rankedArticle().article().title())
                        .append("](")
                        .append(articleHighlight.rankedArticle().article().link())
                        .append(")\n");
                markdown.append("     - 출처: ").append(articleHighlight.rankedArticle().article().sourceName()).append('\n');
                markdown.append("     - 발행 시각: ").append(formatPublishedAt(articleHighlight, zoneId)).append('\n');
                markdown.append("     - 중요도: ").append(formatScoreBreakdown(articleHighlight.rankedArticle().scoreBreakdown())).append('\n');
                markdown.append("     - 한줄 포인트: ").append(articleHighlight.highlight()).append('\n');
            }
            markdown.append('\n');
        }

        return new TrendReport(
                reportDate,
                generatedAt,
                markdown.toString(),
                categorySections,
                reportProperties.getChannel(),
                reportProperties.isDryRun()
        );
    }

    private String formatPublishedAt(ArticleHighlight articleHighlight, ZoneId zoneId) {
        if (articleHighlight.rankedArticle().article().publishedAt() == null) {
            return "발행 시각 미확인";
        }
        return DATE_TIME_FORMATTER.format(articleHighlight.rankedArticle().article().publishedAt().atZone(zoneId));
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
}
