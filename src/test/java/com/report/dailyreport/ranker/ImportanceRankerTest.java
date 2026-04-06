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
                ReportCategory.ECONOMY,
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
}
