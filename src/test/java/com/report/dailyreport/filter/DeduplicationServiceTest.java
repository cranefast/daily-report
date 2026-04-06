package com.report.dailyreport.filter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.ReportCategory;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DeduplicationServiceTest {

    @Test
    void removesDuplicateUrlsAndSimilarTitles() {
        ReportProperties properties = new ReportProperties();
        properties.setDuplicateTitleSimilarityThreshold(0.6);
        DeduplicationService service = new DeduplicationService(properties);

        CollectedArticle newest = new CollectedArticle(
                ReportCategory.AI,
                "OpenAI News",
                "https://openai.com/news/rss.xml",
                "OpenAI launches new reasoning model for developers",
                "https://example.com/article?utm_source=x",
                Instant.parse("2026-04-06T10:00:00Z"),
                "summary",
                0.95,
                List.of("model")
        );
        CollectedArticle duplicateUrl = new CollectedArticle(
                ReportCategory.AI,
                "Mirror",
                "https://mirror.example.com/rss.xml",
                "OpenAI launches new reasoning model for developers",
                "https://example.com/article?utm_source=y",
                Instant.parse("2026-04-06T09:00:00Z"),
                "summary",
                0.80,
                List.of("model")
        );
        CollectedArticle similarTitle = new CollectedArticle(
                ReportCategory.AI,
                "Another Source",
                "https://another.example.com/rss.xml",
                "OpenAI unveils reasoning model aimed at developers",
                "https://another.example.com/article",
                Instant.parse("2026-04-06T08:00:00Z"),
                "summary",
                0.70,
                List.of("model")
        );

        List<CollectedArticle> deduplicated = service.deduplicate(List.of(similarTitle, duplicateUrl, newest));

        assertThat(deduplicated).hasSize(1);
        assertThat(deduplicated.getFirst().title()).isEqualTo(newest.title());
    }
}
