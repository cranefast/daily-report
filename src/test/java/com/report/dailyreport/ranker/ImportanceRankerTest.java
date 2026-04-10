package com.report.dailyreport.ranker;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ImportanceRankerTest {

    @Test
    void ranksRecentKeywordRichArticlesHigher() {
        ReportProperties properties = new ReportProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC);
        ImportanceRanker ranker = new ImportanceRanker(properties, clock);

        CollectedArticle highSignal = new CollectedArticle(
                ReportCategory.AI,
                "OpenAI News",
                "https://openai.com/news/rss.xml",
                "OpenAI releases new GPT model and API for agent workflows",
                "https://example.com/high-signal",
                Instant.parse("2026-04-06T10:00:00Z"),
                "The release expands agent support for enterprise API users.",
                0.98,
                List.of("gpt", "api", "agent")
        );
        CollectedArticle staleArticle = new CollectedArticle(
                ReportCategory.GLOBAL_ECONOMY,
                "Example Economy",
                "https://example.com/rss.xml",
                "Market update",
                "https://example.com/stale",
                Instant.parse("2026-03-30T10:00:00Z"),
                "General market update without signal words.",
                0.60,
                List.of("inflation")
        );

        List<RankedArticle> ranked = ranker.rank(List.of(highSignal, staleArticle), List.of(highSignal, staleArticle, highSignal));

        assertThat(ranked).hasSize(2);
        assertThat(ranked.getFirst().article().title()).isEqualTo(highSignal.title());
        assertThat(ranked.getFirst().scoreBreakdown().totalScore()).isGreaterThan(ranked.get(1).scoreBreakdown().totalScore());
        assertThat(ranked.getFirst().scoreBreakdown().repetitionCount()).isEqualTo(2);
    }

    @Test
    void scoresKoreanEconomyKeywords() {
        ReportProperties properties = new ReportProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-04-10T00:00:00Z"), ZoneOffset.UTC);
        ImportanceRanker ranker = new ImportanceRanker(properties, clock);

        CollectedArticle koreaEconomy = new CollectedArticle(
                ReportCategory.KOREA_ECONOMY,
                "한국은행 보도자료 통계",
                "https://www.bok.or.kr/portal/bbs/B0000501/news.rss?",
                "한국은행, 기준금리 동결 속 물가와 성장률 전망 상향",
                "https://example.com/korea-economy",
                Instant.parse("2026-04-09T23:00:00Z"),
                "기준금리, 물가, 성장률, 환율 변화가 동시에 주목받고 있다.",
                0.97,
                List.of("기준금리", "물가", "성장률")
        );

        List<RankedArticle> ranked = ranker.rank(List.of(koreaEconomy), List.of(koreaEconomy));

        assertThat(ranked).hasSize(1);
        assertThat(ranked.getFirst().scoreBreakdown().keywordScore()).isGreaterThan(0.5);
    }
}
