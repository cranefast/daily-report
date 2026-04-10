package com.report.dailyreport.formatter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import com.report.dailyreport.model.CollectedArticle;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlEmailReportFormatterTest {

    @Test
    void formatsResponsiveHtmlForEmail() {
        ReportProperties properties = new ReportProperties();
        properties.setTopN(3);
        properties.setChannel(NotificationChannel.EMAIL);
        properties.setZoneId("Asia/Seoul");
        HtmlEmailReportFormatter formatter = new HtmlEmailReportFormatter(properties);

        TrendReport report = new TrendReport(
                LocalDate.of(2026, 4, 10),
                Instant.parse("2026-04-10T13:00:00Z"),
                "markdown",
                List.of(sampleSection()),
                NotificationChannel.EMAIL,
                false
        );

        String html = formatter.format(report);

        assertThat(html).contains("<!DOCTYPE html>");
        assertThat(html).contains("@media screen and (max-width: 620px)");
        assertThat(html).contains("2026-04-10 최신 트렌드 리포트");
        assertThat(html).contains("한눈에 보는 핵심 요약");
        assertThat(html).contains("GitHub ships new Copilot workflow tooling");
        assertThat(html).contains("href=\"https://github.blog/example\"");
        assertThat(html).contains("OpenAI").doesNotContain("undefined");
    }

    private CategoryTrendSection sampleSection() {
        return new CategoryTrendSection(
                ReportCategory.DEVELOPMENT,
                new CategoryInsight(
                        "개발 분야는 팀 생산성과 플랫폼 전환 이슈가 함께 커지고 있습니다.",
                        "상위 기사는 개발 도구 업그레이드와 워크플로 표준화 흐름을 보여 줍니다.",
                        "개발 조직은 도구 선택뿐 아니라 플랫폼 종속성과 교육 비용도 함께 봐야 합니다.",
                        "도입 전 권한 체계와 릴리스 노트를 먼저 검토하는 것이 좋습니다.",
                        List.of("릴리스 노트 확인", "권한 범위 검토", "CI 영향 점검"),
                        true
                ),
                List.of(new ArticleHighlight(
                        new RankedArticle(
                                new CollectedArticle(
                                        ReportCategory.DEVELOPMENT,
                                        "GitHub Blog",
                                        "https://github.blog/feed/",
                                        "GitHub ships new Copilot workflow tooling",
                                        "https://github.blog/example",
                                        Instant.parse("2026-04-10T08:00:00Z"),
                                        "GitHub announced workflow improvements for development teams.",
                                        0.94,
                                        List.of("developer", "workflow")
                                ),
                                new ScoreBreakdown(88.4, 0.92, 0.75, 0.94, 0.60, 0.35, 0.30, 0.20, 0.15, 2)
                        ),
                        "워크플로 자동화가 팀 생산성과 협업 방식에 직접 영향을 줍니다."
                ))
        );
    }
}
