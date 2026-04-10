package com.report.dailyreport.analyzer;

import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FallbackTrendAnalyzerTest {

    private final FallbackTrendAnalyzer analyzer = new FallbackTrendAnalyzer();

    @Test
    void keepsFullTextWithoutEllipsisInFallbackOutput() {
        Map<ReportCategory, List<RankedArticle>> input = new EnumMap<>(ReportCategory.class);
        input.put(ReportCategory.KOREA_REAL_ESTATE, List.of(
                rankedArticle(
                        "서울 아파트 거래량 반등과 전세가 회복 조짐이 동시에 나타나고 있습니다",
                        "서울 아파트 거래량 반등과 전세가 회복 조짐이 동시에 나타나고 있습니다. 대출 규제 완화 기대와 공급 지연이 함께 언급됩니다."
                ),
                rankedArticle(
                        "수도권 분양 일정 조정이 청약 대기 수요에 미치는 영향",
                        "수도권 분양 일정 조정이 청약 대기 수요에 영향을 주고 있습니다."
                ),
                rankedArticle(
                        "전세 시장 안정 여부를 가를 신규 입주 물량 체크",
                        "전세 시장 안정 여부를 가를 신규 입주 물량 체크가 필요합니다."
                )
        ));

        List<CategoryTrendSection> result = analyzer.analyze(input);

        assertThat(result).hasSize(1);
        CategoryTrendSection section = result.getFirst();
        assertThat(section.insight().summary()).doesNotContain("…");
        assertThat(section.insight().detailedReport()).doesNotContain("…");
        assertThat(section.articles().getFirst().highlight()).doesNotContain("…");
        assertThat(section.insight().detailedReport()).contains("서울 아파트 거래량 반등과 전세가 회복 조짐이 동시에 나타나고 있습니다");
    }

    private RankedArticle rankedArticle(String title, String summary) {
        return new RankedArticle(
                new CollectedArticle(
                        ReportCategory.KOREA_REAL_ESTATE,
                        "매일경제 부동산",
                        "https://www.mk.co.kr/rss/50300009/",
                        title,
                        "https://example.com/" + title.hashCode(),
                        Instant.parse("2026-04-10T00:00:00Z"),
                        summary,
                        0.88,
                        List.of("부동산", "아파트", "전세")
                ),
                new ScoreBreakdown(90.0, 0.9, 0.8, 0.88, 0.2, 0.35, 0.30, 0.20, 0.15, 1)
        );
    }
}
