package com.report.dailyreport.analyzer;

import com.report.dailyreport.model.ArticleAnalysis;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.util.TextNormalizationUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class FallbackTrendAnalyzer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public ArticleAnalysis analyze(RankedArticle rankedArticle) {
        CollectedArticle article = rankedArticle.article();
        ScoreBreakdown scoreBreakdown = rankedArticle.scoreBreakdown();
        String publishedAt = article.publishedAt() == null
                ? "발행 시각 미확인"
                : DATE_TIME_FORMATTER.format(article.publishedAt().atZone(ZoneId.of("Asia/Seoul")));

        String summary = TextNormalizationUtils.shorten(
                !article.summary().isBlank() ? article.summary() : article.title(),
                180
        );

        String detailedExplanation = "%s 소스에서 %s에 발행된 기사로, \"%s\" 이슈를 다룹니다. 원문 요약을 바탕으로 핵심 맥락을 정리하면 %s"
                .formatted(article.sourceName(), publishedAt, article.title(), summary);

        String importanceReason = "점수 %.2f점입니다. 최신성 %.2f, 키워드 적합도 %.2f, 출처 신뢰도 %.2f, 반복 언급 %.2f가 반영됐습니다."
                .formatted(
                        scoreBreakdown.totalScore(),
                        scoreBreakdown.recencyScore(),
                        scoreBreakdown.keywordScore(),
                        scoreBreakdown.sourceReliabilityScore(),
                        scoreBreakdown.repetitionScore()
                );

        String industryImpact = buildIndustryImpact(article);
        String practicalImplications = buildPracticalImplications(article, scoreBreakdown);
        List<String> followUpPoints = buildFollowUpPoints(article);

        return new ArticleAnalysis(
                summary,
                detailedExplanation,
                importanceReason,
                industryImpact,
                practicalImplications,
                followUpPoints,
                false
        );
    }

    private String buildIndustryImpact(CollectedArticle article) {
        return switch (article.category()) {
            case AI, DEVELOPMENT -> "IT 산업에서는 플랫폼 경쟁, 개발 생산성, 인프라 투자 우선순위 조정으로 이어질 가능성이 큽니다. 관련 팀은 API/플랫폼 변경과 비용 구조 변화를 같이 봐야 합니다.";
            case REAL_ESTATE -> "부동산 시장에서는 금리, 공급, 거래 회복 여부에 따라 건설, 금융, 소비재 업종까지 연쇄 영향이 퍼질 수 있습니다. 이전 추세와 달라지는 신호인지 함께 관찰해야 합니다.";
            case ECONOMY -> "거시경제 변화는 금리 민감 업종, 수출 제조업, 소비 업종 전반에 파급됩니다. 직전 흐름과 비교해 정책 방향과 시장 기대치가 바뀌는지 확인이 필요합니다.";
        };
    }

    private String buildPracticalImplications(CollectedArticle article, ScoreBreakdown scoreBreakdown) {
        return switch (article.category()) {
            case AI -> "AI 기능을 다루는 조직이라면 모델 비용, 성능, 벤더 종속성 관점에서 PoC 범위를 다시 정리하는 것이 좋습니다. 반복 언급 수(%d건)가 높다면 우선 관찰 대상에 올릴 만합니다."
                    .formatted(scoreBreakdown.repetitionCount());
            case DEVELOPMENT -> "개발 조직은 도구 업그레이드 범위, 호환성 영향, 팀 생산성 개선 포인트를 바로 점검하는 것이 좋습니다. 릴리스 노트와 마이그레이션 가이드를 함께 확인하세요.";
            case REAL_ESTATE -> "부동산 관련 의사결정에서는 거래량, 금리, 신규 공급 데이터를 같이 확인해야 합니다. 정책 발표나 대출 규제 변화가 있으면 건설/금융 섹터 관찰 강도를 높이세요.";
            case ECONOMY -> "경제 지표는 단일 기사보다 연속 추세가 중요합니다. 다음 발표 일정과 중앙은행 발언, 업종별 실적 가이던스를 묶어서 보시는 것이 안전합니다.";
        };
    }

    private List<String> buildFollowUpPoints(CollectedArticle article) {
        String baseTitle = TextNormalizationUtils.shorten(article.title(), 80);
        return switch (article.category()) {
            case AI -> List.of(
                    baseTitle + " 관련 후속 제품/모델 발표가 이어지는지 확인",
                    "GPU·클라우드 비용 혹은 공급망 변화가 동반되는지 체크",
                    "주요 경쟁사 대응 전략과 가격 정책 비교"
            );
            case DEVELOPMENT -> List.of(
                    "공식 릴리스 노트와 브레이킹 체인지 여부 확인",
                    "팀 적용 시 테스트 범위와 롤백 계획 정리",
                    "관련 SDK·플러그인 호환성 확인"
            );
            case REAL_ESTATE -> List.of(
                    "거래량과 재고 지표가 같은 방향으로 움직이는지 확인",
                    "정책·금리 변화가 실제 수요에 반영되는지 점검",
                    "건설·금융·소비 업종 영향 확산 여부 추적"
            );
            case ECONOMY -> List.of(
                    "다음 CPI·고용·금리 일정과 컨센서스 확인",
                    "수혜/피해 업종이 실제 실적으로 연결되는지 추적",
                    "시장 기대와 정책 메시지의 괴리 여부 점검"
            );
        };
    }
}
