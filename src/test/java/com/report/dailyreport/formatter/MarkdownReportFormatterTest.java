package com.report.dailyreport.formatter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.AnalyzedArticle;
import com.report.dailyreport.model.ArticleAnalysis;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownReportFormatterTest {

    @Test
    void formatsExpectedSections() {
        ReportProperties properties = new ReportProperties();
        properties.setChannel(NotificationChannel.EMAIL);
        Clock clock = Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC);
        MarkdownReportFormatter formatter = new MarkdownReportFormatter(properties, clock);

        AnalyzedArticle analyzedArticle = new AnalyzedArticle(
                new RankedArticle(
                        new CollectedArticle(
                                ReportCategory.DEVELOPMENT,
                                "GitHub Blog",
                                "https://github.blog/feed/",
                                "GitHub ships new Copilot workflow tooling",
                                "https://github.blog/example",
                                Instant.parse("2026-04-06T08:00:00Z"),
                                "GitHub announced workflow improvements for development teams.",
                                0.94,
                                List.of("developer", "workflow")
                        ),
                        new ScoreBreakdown(88.4, 0.92, 0.75, 0.94, 0.60, 0.35, 0.30, 0.20, 0.15, 2)
                ),
                new ArticleAnalysis(
                        "개발팀 생산성을 높일 수 있는 워크플로 개선입니다.",
                        "릴리스는 협업과 자동화 흐름을 강화하는 방향입니다.",
                        "도구 변화가 팀 생산성과 플랫폼 종속성에 직접 연결됩니다.",
                        "IT 조직은 개발 파이프라인과 플랫폼 전략을 같이 점검해야 합니다.",
                        "도입 전 권한 체계와 릴리스 노트를 검토하는 것이 좋습니다.",
                        List.of("릴리스 노트 확인", "권한 범위 검토", "CI 파이프라인 영향 점검"),
                        true
                )
        );

        TrendReport report = formatter.format(List.of(analyzedArticle));

        assertThat(report.markdown()).contains("[2026-04-06 최신 트렌드 리포트]");
        assertThat(report.markdown()).contains("## 1. 오늘의 핵심 요약");
        assertThat(report.markdown()).contains("## 2. 주요 이슈");
        assertThat(report.markdown()).contains("## 3. IT 종합 인사이트");
        assertThat(report.markdown()).contains("## 4. 관찰 포인트");
    }
}
