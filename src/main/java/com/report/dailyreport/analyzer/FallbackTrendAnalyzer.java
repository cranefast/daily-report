package com.report.dailyreport.analyzer;

import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
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

        String leadSummary = rankedArticles.getFirst().article().title();
        String topTitles = rankedArticles.stream()
                .limit(3)
                .map(article -> article.article().title())
                .collect(Collectors.joining(", "));

        String summary = "%s 분야 상위 기사들은 '%s' 이슈를 중심으로 움직이고 있습니다."
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
        return "%s 총점 %.1f점으로 선정됐고, 핵심 포인트는 %s"
                .formatted(
                        article.sourceName(),
                        rankedArticle.scoreBreakdown().totalScore(),
                        article.title()
                );
    }

    private String buildCategoryDetailedAngle(ReportCategory category) {
        return switch (category) {
            case AI -> "신규 모델·에이전트 경쟁과 반도체·클라우드 투자 파급을 함께 봐야 합니다.";
            case DEVELOPMENT -> "개발 생산성, 도구 호환성, 팀 적용 난이도를 같이 판단해야 합니다.";
            case KOREA_REAL_ESTATE -> "한국 주택시장 기준으로 정책, 대출 규제, 공급 일정 변화가 실수요와 가격에 어떻게 이어지는지 비교가 필요합니다.";
            case KOREA_ECONOMY -> "한국 기준으로 기준금리, 환율, 수출, 물가 변화가 내수와 기업 실적에 어떻게 이어지는지 봐야 합니다.";
            case GLOBAL_ECONOMY -> "미국과 글로벌 기준으로 연준 정책, 물가, 성장률, 원자재 변화가 시장 기대와 업종별 체감 경기로 이어지는지 봐야 합니다.";
        };
    }

    private String buildIndustryImpact(ReportCategory category) {
        return switch (category) {
            case AI, DEVELOPMENT -> "IT 산업에서는 플랫폼 경쟁, 개발 생산성, 인프라 투자 우선순위 조정으로 이어질 가능성이 큽니다. 관련 팀은 API·플랫폼 변경과 비용 구조 변화를 함께 봐야 합니다.";
            case KOREA_REAL_ESTATE -> "한국 부동산 시장에서는 대출 규제, 공급 계획, 거래 회복 여부에 따라 건설, 금융, 내수 업종까지 연쇄 영향이 퍼질 수 있습니다. 수도권과 지방의 온도차도 함께 봐야 합니다.";
            case KOREA_ECONOMY -> "한국 거시경제 변화는 수출 제조업, 내수 소비, 금융 업종 전반에 파급됩니다. 특히 환율과 수출 흐름이 기업 실적 전망을 얼마나 바꾸는지 확인이 필요합니다.";
            case GLOBAL_ECONOMY -> "미국과 글로벌 거시경제 변화는 금리 민감 업종, 원자재, 수출주, 달러 자산 전반에 파급됩니다. 연준 메시지와 국제 분쟁 변수가 시장 기대를 바꾸는지 함께 봐야 합니다.";
        };
    }

    private String buildPracticalImplications(ReportCategory category) {
        return switch (category) {
            case AI -> "AI 기능을 다루는 조직이라면 모델 비용, 성능, 벤더 종속성 관점에서 PoC 범위를 다시 정리하는 것이 좋습니다. 후속 발표가 이어지는지 관찰 우선순위를 높이세요.";
            case DEVELOPMENT -> "개발 조직은 도구 업그레이드 범위, 호환성 영향, 팀 생산성 개선 포인트를 바로 점검하는 것이 좋습니다. 릴리스 노트와 마이그레이션 가이드를 함께 확인하세요.";
            case KOREA_REAL_ESTATE -> "한국 부동산 관련 의사결정에서는 거래량, 전세가, 신규 공급, 대출 규제를 같이 확인해야 합니다. 정책 발표가 있으면 지역별 체감 차이까지 함께 보세요.";
            case KOREA_ECONOMY -> "한국 경제 지표는 기준금리 결정, 수출 실적, 물가 발표를 묶어서 보는 것이 안전합니다. 산업별로 환율 민감도와 내수 회복 속도를 함께 점검하세요.";
            case GLOBAL_ECONOMY -> "글로벌 경제 지표는 연준 발언, 미국 고용·물가, 원자재 가격을 연속 추세로 보는 것이 안전합니다. 달러 강세와 지정학 변수까지 함께 점검하세요.";
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
            case KOREA_REAL_ESTATE -> List.of(
                    "거래량과 전세가 동행 여부 확인",
                    "대출·세제 정책의 실제 수요 반영 점검",
                    "수도권과 지방 시장 온도차 추적"
            );
            case KOREA_ECONOMY -> List.of(
                    "한은·물가·수출 일정 확인",
                    "환율과 수출주 실적 연결 점검",
                    "정책 메시지와 내수 체감 괴리 확인"
            );
            case GLOBAL_ECONOMY -> List.of(
                    "다음 미국 CPI·고용·FOMC 일정 확인",
                    "유가·달러와 업종 실적 연결 점검",
                    "연준 메시지와 시장 기대 괴리 확인"
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
