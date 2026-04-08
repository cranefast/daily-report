package com.report.dailyreport.analyzer;

import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.util.TextNormalizationUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class FallbackTrendAnalyzer implements TrendAnalyzer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");

    @Override
    public List<CategoryTrendSection> analyze(Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        List<CategoryTrendSection> sections = new ArrayList<>();
        for (Map.Entry<ReportCategory, List<RankedArticle>> entry : articlesByCategory.entrySet()) {
            sections.add(new CategoryTrendSection(
                    entry.getKey(),
                    buildCategoryInsight(entry.getKey(), entry.getValue()),
                    buildArticleHighlights(entry.getValue())
            ));
        }
        return sections;
    }

    private CategoryInsight buildCategoryInsight(ReportCategory category, List<RankedArticle> rankedArticles) {
        if (rankedArticles.isEmpty()) {
            return new CategoryInsight(
                    "%s 분야는 오늘 수집된 상위 기사가 부족해 인사이트가 제한적입니다.".formatted(category.getDisplayName()),
                    "%s 관련 RSS 기사 수가 부족해 중요도 상위 3건 기준 비교 리포트를 충분히 만들지 못했습니다.".formatted(category.getDisplayName()),
                    buildIndustryImpact(category),
                    buildPracticalImplications(category),
                    buildFollowUpPoints(category),
                    false
            );
        }

        String leadSummary = compactArticleText(rankedArticles.getFirst().article());
        String topTitles = rankedArticles.stream()
                .limit(3)
                .map(article -> TextNormalizationUtils.shorten(article.article().title(), 40))
                .collect(Collectors.joining(", "));

        String summary = "%s 분야 상위 기사들은 %s 흐름을 중심으로 움직이고 있습니다."
                .formatted(category.getDisplayName(), leadSummary);
        String detailedReport = "중요도 상위 %d건은 %s 이슈에 집중돼 있습니다. 공통적으로 %s"
                .formatted(
                        rankedArticles.size(),
                        topTitles,
                        buildCategoryDetailedAngle(category)
                );

        return new CategoryInsight(
                summary,
                detailedReport,
                buildIndustryImpact(category),
                buildPracticalImplications(category),
                buildFollowUpPoints(category),
                false
        );
    }

    private List<ArticleHighlight> buildArticleHighlights(List<RankedArticle> rankedArticles) {
        return rankedArticles.stream()
                .map(rankedArticle -> new ArticleHighlight(
                        rankedArticle,
                        buildArticleHighlight(rankedArticle)
                ))
                .toList();
    }

    private String buildArticleHighlight(RankedArticle rankedArticle) {
        CollectedArticle article = rankedArticle.article();
        String summary = compactArticleText(article);
        return "%s 총점 %.1f점으로 선정됐고, 핵심 포인트는 %s"
                .formatted(
                        article.sourceName(),
                        rankedArticle.scoreBreakdown().totalScore(),
                        summary
                );
    }

    private String compactArticleText(CollectedArticle article) {
        String base = article.summary().isBlank() ? article.title() : article.summary();
        return TextNormalizationUtils.shorten(base, 70);
    }

    private String buildCategoryDetailedAngle(ReportCategory category) {
        return switch (category) {
            case AI -> "신규 모델·에이전트 경쟁과 반도체·클라우드 투자 파급을 함께 봐야 합니다.";
            case DEVELOPMENT -> "개발 생산성, 도구 호환성, 팀 적용 난이도를 같이 판단해야 합니다.";
            case REAL_ESTATE -> "금리·공급·수요 변화가 이전 추세와 얼마나 다른지 비교가 필요합니다.";
            case ECONOMY -> "거시지표 변화가 금리 기대와 업종별 체감 경기로 이어지는지 봐야 합니다.";
        };
    }

    private String buildIndustryImpact(ReportCategory category) {
        return switch (category) {
            case AI, DEVELOPMENT -> "IT 산업에서는 플랫폼 경쟁, 개발 생산성, 인프라 투자 우선순위 조정으로 이어질 가능성이 큽니다. 관련 팀은 API·플랫폼 변경과 비용 구조 변화를 함께 봐야 합니다.";
            case REAL_ESTATE -> "부동산 시장에서는 금리, 공급, 거래 회복 여부에 따라 건설, 금융, 소비재 업종까지 연쇄 영향이 퍼질 수 있습니다. 이전 추세와 달라지는 신호인지 함께 관찰해야 합니다.";
            case ECONOMY -> "거시경제 변화는 금리 민감 업종, 수출 제조업, 소비 업종 전반에 파급됩니다. 직전 흐름과 비교해 정책 방향과 시장 기대치가 바뀌는지 확인이 필요합니다.";
        };
    }

    private String buildPracticalImplications(ReportCategory category) {
        return switch (category) {
            case AI -> "AI 기능을 다루는 조직이라면 모델 비용, 성능, 벤더 종속성 관점에서 PoC 범위를 다시 정리하는 것이 좋습니다. 후속 발표가 이어지는지 관찰 우선순위를 높이세요.";
            case DEVELOPMENT -> "개발 조직은 도구 업그레이드 범위, 호환성 영향, 팀 생산성 개선 포인트를 바로 점검하는 것이 좋습니다. 릴리스 노트와 마이그레이션 가이드를 함께 확인하세요.";
            case REAL_ESTATE -> "부동산 관련 의사결정에서는 거래량, 금리, 신규 공급 데이터를 같이 확인해야 합니다. 정책 발표나 대출 규제 변화가 있으면 건설·금융 섹터 관찰 강도를 높이세요.";
            case ECONOMY -> "경제 지표는 단일 기사보다 연속 추세가 중요합니다. 다음 발표 일정과 중앙은행 발언, 업종별 실적 가이던스를 묶어서 보는 것이 안전합니다.";
        };
    }

    private List<String> buildFollowUpPoints(ReportCategory category) {
        return switch (category) {
            case AI -> List.of(
                    "후속 모델 발표 여부 확인",
                    "클라우드·GPU 비용 변화 점검",
                    "경쟁사 대응 전략 비교"
            );
            case DEVELOPMENT -> List.of(
                    "릴리스 노트와 브레이킹 체인지 확인",
                    "팀 적용 범위와 롤백 계획 정리",
                    "SDK·플러그인 호환성 확인"
            );
            case REAL_ESTATE -> List.of(
                    "거래량과 재고 지표 동행 여부 확인",
                    "정책·금리 변화의 실제 수요 반영 점검",
                    "건설·금융·소비 업종 영향 추적"
            );
            case ECONOMY -> List.of(
                    "다음 CPI·고용·금리 일정 확인",
                    "수혜·피해 업종 실적 연결 점검",
                    "정책 메시지와 시장 기대 괴리 확인"
            );
        };
    }

    @SuppressWarnings("unused")
    private String formatPublishedAt(CollectedArticle article) {
        if (article.publishedAt() == null) {
            return "발행 시각 미확인";
        }
        return DATE_TIME_FORMATTER.format(article.publishedAt().atZone(ASIA_SEOUL));
    }
}
