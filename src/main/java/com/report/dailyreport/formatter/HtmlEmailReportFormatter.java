package com.report.dailyreport.formatter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component
@RequiredArgsConstructor
public class HtmlEmailReportFormatter {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final ReportProperties reportProperties;

    public String format(TrendReport report) {
        ZoneId zoneId = ZoneId.of(reportProperties.getZoneId());
        String generatedAt = DATE_TIME_FORMATTER.format(report.generatedAt().atZone(zoneId));
        String preheader = buildPreheader(report.sections());
        StringBuilder html = new StringBuilder();

        html.append("<!DOCTYPE html>");
        html.append("<html lang=\"ko\">");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<title>").append(escape(report.reportDate() + " 최신 트렌드 리포트")).append("</title>");
        html.append("<style>");
        html.append("body{margin:0;padding:0;background:#f4efe6;color:#1f2937;font-family:'Apple SD Gothic Neo','Noto Sans KR','Segoe UI',sans-serif;}");
        html.append("table{border-collapse:collapse;border-spacing:0;}a{color:#0f4c81;text-decoration:none;}");
        html.append(".shell{width:100%;background:#f4efe6;padding:24px 12px;}");
        html.append(".container{width:100%;max-width:680px;background:#fffdf8;border-radius:24px;overflow:hidden;}");
        html.append(".pad-lg{padding:32px 36px;}.pad-md{padding:24px 28px;}.stack-gap{padding-top:20px;}");
        html.append(".hero{background:#183153;color:#f8fafc;}.eyebrow{font-size:12px;letter-spacing:0.08em;text-transform:uppercase;color:#b7c7de;}");
        html.append(".headline{font-size:28px;line-height:1.3;font-weight:700;color:#ffffff;padding-top:12px;}");
        html.append(".subcopy{font-size:14px;line-height:1.7;color:#dbe7f6;padding-top:14px;}");
        html.append(".chip{display:inline-block;margin:6px 8px 0 0;padding:8px 12px;border-radius:999px;background:#f0f6ff;color:#17324d;font-size:12px;font-weight:700;}");
        html.append(".section-title{font-size:22px;line-height:1.4;font-weight:700;color:#16324f;padding-bottom:12px;}");
        html.append(".summary-card,.detail-card,.article-card{border:1px solid #e5ddd0;border-radius:18px;background:#fffaf2;}");
        html.append(".summary-card td{padding:18px 20px;}.detail-card td{padding:24px 24px 10px 24px;}.article-card td{padding:18px 20px;}");
        html.append(".category-name{font-size:18px;line-height:1.4;font-weight:700;color:#17324d;}");
        html.append(".muted{font-size:13px;line-height:1.6;color:#6b7280;}.body-copy{font-size:15px;line-height:1.75;color:#253040;}");
        html.append(".label{font-size:12px;font-weight:700;letter-spacing:0.04em;text-transform:uppercase;color:#8b5e3c;padding-bottom:8px;}");
        html.append(".divider{border-top:1px solid #ece3d5;}.bullet{font-size:14px;line-height:1.7;color:#374151;padding-top:8px;}");
        html.append(".article-title{font-size:17px;line-height:1.5;font-weight:700;color:#0f4c81;}.quote{font-size:14px;line-height:1.7;color:#374151;background:#ffffff;border-left:4px solid #d8b07a;border-radius:12px;padding:14px 16px;margin-top:12px;}");
        html.append(".meta{font-size:12px;line-height:1.7;color:#6b7280;padding-top:10px;}.footer{font-size:12px;line-height:1.7;color:#7c8696;}");
        html.append("@media screen and (max-width: 620px){.shell{padding:12px 0 !important;}.container{border-radius:0 !important;}.pad-lg{padding:24px 20px !important;}.pad-md{padding:20px 16px !important;}.headline{font-size:24px !important;}.section-title{font-size:20px !important;}.summary-card td,.detail-card td,.article-card td{padding:18px 16px !important;}}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div style=\"display:none;max-height:0;overflow:hidden;opacity:0;color:transparent;\">")
                .append(escape(preheader))
                .append("</div>");
        html.append("<table role=\"presentation\" class=\"shell\" width=\"100%\"><tr><td align=\"center\">");
        html.append("<table role=\"presentation\" class=\"container\" width=\"100%\">");
        appendHero(html, report, generatedAt);
        appendSummary(html, report.sections());
        appendDetailSections(html, report.sections(), zoneId);
        appendFooter(html);
        html.append("</table>");
        html.append("</td></tr></table>");
        html.append("</body></html>");
        return html.toString();
    }

    private void appendHero(StringBuilder html, TrendReport report, String generatedAt) {
        html.append("<tr><td class=\"hero pad-lg\">");
        html.append("<div class=\"eyebrow\">Daily Trend Brief</div>");
        html.append("<div class=\"headline\">")
                .append(escape(report.reportDate() + " 최신 트렌드 리포트"))
                .append("</div>");
        html.append("<div class=\"subcopy\">")
                .append("핵심 이슈를 바로 훑고, 카테고리별 상세 포인트와 기사 링크까지 한 번에 확인할 수 있도록 정리했습니다.")
                .append("</div>");
        html.append("<div style=\"padding-top:16px;\">");
        appendChip(html, "생성 시각 " + generatedAt);
        appendChip(html, "채널 " + report.channel());
        appendChip(html, "실행 모드 " + (report.dryRun() ? "dry-run" : "live"));
        appendChip(html, "카테고리 " + report.sections().size() + "개");
        html.append("</div>");
        html.append("</td></tr>");
    }

    private void appendSummary(StringBuilder html, List<CategoryTrendSection> sections) {
        html.append("<tr><td class=\"pad-md\">");
        html.append("<div class=\"section-title\">한눈에 보는 핵심 요약</div>");
        for (CategoryTrendSection section : sections) {
            html.append("<table role=\"presentation\" width=\"100%\" class=\"summary-card\" style=\"margin-top:14px;\">");
            html.append("<tr><td>");
            html.append("<div class=\"category-name\">").append(escape(section.category().getDisplayName())).append("</div>");
            html.append("<div class=\"body-copy\" style=\"padding-top:10px;\">")
                    .append(escape(section.insight().summary()))
                    .append("</div>");
            html.append("</td></tr></table>");
        }
        html.append("</td></tr>");
    }

    private void appendDetailSections(StringBuilder html, List<CategoryTrendSection> sections, ZoneId zoneId) {
        html.append("<tr><td class=\"pad-md\" style=\"padding-top:0;\">");
        html.append("<div class=\"section-title\">분야별 상세 리포트</div>");
        for (CategoryTrendSection section : sections) {
            html.append("<table role=\"presentation\" width=\"100%\" class=\"detail-card\" style=\"margin-top:18px;\">");
            html.append("<tr><td>");
            html.append("<div style=\"display:flex;\">");
            html.append("<div class=\"category-name\">").append(escape(section.category().getDisplayName())).append("</div>");
            html.append("</div>");
            html.append("<div class=\"muted\" style=\"padding-top:10px;\">");
            html.append("선정 기사 ").append(section.articles().size()).append("건 / 목표 ").append(reportProperties.effectiveTopN()).append("건");
            html.append(" &nbsp;|&nbsp; 생성 방식 ").append(escape(section.insight().generatedByAi() ? "OpenAI" : "규칙 기반 fallback"));
            html.append("</div>");
            appendParagraph(html, "핵심 인사이트", section.insight().summary());
            appendParagraph(html, "상세 리포트", section.insight().detailedReport());
            appendParagraph(html, "산업 영향", section.insight().industryImpact());
            appendParagraph(html, "실무 시사점", section.insight().practicalImplications());
            appendFollowUpPoints(html, section.insight().followUpPoints());
            appendArticles(html, section.articles(), zoneId);
            html.append("</td></tr></table>");
        }
        html.append("</td></tr>");
    }

    private void appendParagraph(StringBuilder html, String label, String body) {
        html.append("<div style=\"padding-top:18px;\">");
        html.append("<div class=\"label\">").append(escape(label)).append("</div>");
        html.append("<div class=\"body-copy\">").append(escape(body)).append("</div>");
        html.append("</div>");
    }

    private void appendFollowUpPoints(StringBuilder html, List<String> points) {
        html.append("<div style=\"padding-top:18px;\">");
        html.append("<div class=\"label\">후속 체크 포인트</div>");
        if (points == null || points.isEmpty()) {
            html.append("<div class=\"body-copy\">후속 체크 포인트를 생성하지 못했습니다.</div>");
            html.append("</div>");
            return;
        }
        for (String point : points) {
            html.append("<div class=\"bullet\">&#8226; ").append(escape(point)).append("</div>");
        }
        html.append("</div>");
    }

    private void appendArticles(StringBuilder html, List<ArticleHighlight> articles, ZoneId zoneId) {
        html.append("<div style=\"padding-top:22px;\">");
        html.append("<div class=\"label\">중요 뉴스 링크</div>");
        if (articles == null || articles.isEmpty()) {
            html.append("<div class=\"body-copy\">오늘은 상위 기사 확보가 부족했습니다.</div>");
            html.append("</div>");
            return;
        }

        for (int index = 0; index < articles.size(); index++) {
            ArticleHighlight article = articles.get(index);
            html.append("<table role=\"presentation\" width=\"100%\" class=\"article-card\" style=\"margin-top:12px;background:#ffffff;\">");
            html.append("<tr><td>");
            html.append("<div class=\"muted\">TOP ").append(index + 1).append("</div>");
            html.append("<div style=\"padding-top:6px;\"><a class=\"article-title\" href=\"")
                    .append(escapeAttribute(article.rankedArticle().article().link()))
                    .append("\">")
                    .append(escape(article.rankedArticle().article().title()))
                    .append("</a></div>");
            html.append("<div class=\"meta\">")
                    .append("출처 ").append(escape(article.rankedArticle().article().sourceName()))
                    .append(" &nbsp;|&nbsp; 발행 ").append(escape(formatPublishedAt(article, zoneId)))
                    .append(" &nbsp;|&nbsp; ").append(escape(formatScore(article.rankedArticle().scoreBreakdown())))
                    .append("</div>");
            html.append("<div class=\"quote\">").append(escape(article.highlight())).append("</div>");
            html.append("</td></tr></table>");
        }
        html.append("</div>");
    }

    private void appendFooter(StringBuilder html) {
        html.append("<tr><td class=\"pad-md\" style=\"padding-top:8px;\">");
        html.append("<div class=\"divider\"></div>");
        html.append("<div class=\"footer\" style=\"padding-top:16px;\">");
        html.append("이 메일은 Daily Report 자동 리포트가 발송했습니다. 모바일에서는 카드가 한 줄로 자연스럽게 쌓이도록 구성했습니다.");
        html.append("</div>");
        html.append("</td></tr>");
    }

    private void appendChip(StringBuilder html, String text) {
        html.append("<span class=\"chip\">").append(escape(text)).append("</span>");
    }

    private String buildPreheader(List<CategoryTrendSection> sections) {
        if (sections == null || sections.isEmpty()) {
            return "오늘의 핵심 트렌드 리포트가 도착했습니다.";
        }
        return sections.stream()
                .limit(2)
                .map(section -> section.category().getDisplayName() + " " + section.insight().summary())
                .collect(Collectors.joining(" | "));
    }

    private String formatPublishedAt(ArticleHighlight articleHighlight, ZoneId zoneId) {
        if (articleHighlight.rankedArticle().article().publishedAt() == null) {
            return "발행 시각 미확인";
        }
        return DATE_TIME_FORMATTER.format(articleHighlight.rankedArticle().article().publishedAt().atZone(zoneId));
    }

    private String formatScore(ScoreBreakdown score) {
        return String.format(
                Locale.ROOT,
                "총점 %.2f점 · 최신성 %.2f · 키워드 %.2f · 출처 %.2f · 반복 %d건",
                score.totalScore(),
                score.recencyScore(),
                score.keywordScore(),
                score.sourceReliabilityScore(),
                score.repetitionCount()
        );
    }

    private String escape(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }

    private String escapeAttribute(String value) {
        return HtmlUtils.htmlEscape(value == null ? "" : value);
    }
}
