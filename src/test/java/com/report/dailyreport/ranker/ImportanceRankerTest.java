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
    void boostsRepeatedTopicsAcrossSources() {
        ReportProperties properties = new ReportProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-04-06T12:00:00Z"), ZoneOffset.UTC);
        ImportanceRanker ranker = new ImportanceRanker(properties, clock);

        CollectedArticle representative = new CollectedArticle(
                ReportCategory.AI,
                "OpenAI News",
                "https://openai.com/news/rss.xml",
                "OpenAI releases new GPT model for agent workflows",
                "https://example.com/openai-gpt",
                Instant.parse("2026-04-06T10:00:00Z"),
                "The update expands agent orchestration for enterprise developers.",
                0.98,
                List.of("gpt", "api", "agent")
        );
        CollectedArticle supportingCoverageA = new CollectedArticle(
                ReportCategory.AI,
                "Google DeepMind Blog",
                "https://deepmind.google/blog/rss.xml",
                "Developers get a new OpenAI GPT model for agent systems",
                "https://example.com/deepmind-gpt",
                Instant.parse("2026-04-06T09:00:00Z"),
                "The new GPT release focuses on reliable agent execution for developer tools.",
                0.96,
                List.of("gpt", "agent", "developer")
        );
        CollectedArticle supportingCoverageB = new CollectedArticle(
                ReportCategory.AI,
                "Simon Willison",
                "https://simonwillison.net/atom/entries/",
                "OpenAI GPT update targets agent tooling for developers",
                "https://example.com/simon-gpt",
                Instant.parse("2026-04-06T08:00:00Z"),
                "Developers are evaluating the GPT update for agent workflows and tools.",
                0.86,
                List.of("openai", "gpt", "agent")
        );
        CollectedArticle singleMention = new CollectedArticle(
                ReportCategory.AI,
                "Hugging Face Blog",
                "https://huggingface.co/blog/feed.xml",
                "Open-source speech model improves transcription accuracy",
                "https://example.com/speech-model",
                Instant.parse("2026-04-06T11:00:00Z"),
                "The release improves multilingual speech recognition accuracy.",
                0.90,
                List.of("model", "open source")
        );

        List<RankedArticle> ranked = ranker.rank(
                List.of(representative, singleMention),
                List.of(representative, supportingCoverageA, supportingCoverageB, singleMention)
        );

        assertThat(ranked).hasSize(2);
        assertThat(ranked.getFirst().article().title()).isEqualTo(representative.title());
        assertThat(ranked.getFirst().scoreBreakdown().totalScore()).isGreaterThan(ranked.get(1).scoreBreakdown().totalScore());
        assertThat(ranked.getFirst().scoreBreakdown().repetitionCount()).isEqualTo(3);
        assertThat(ranked.getFirst().scoreBreakdown().repetitionScore()).isGreaterThan(ranked.get(1).scoreBreakdown().repetitionScore());
    }

    @Test
    void scoresSourceKeywordsWhenCoverageIsSingle() {
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
        assertThat(ranked.getFirst().scoreBreakdown().repetitionCount()).isEqualTo(1);
    }
}
