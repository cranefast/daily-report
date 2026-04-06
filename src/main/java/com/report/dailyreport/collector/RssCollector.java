package com.report.dailyreport.collector;

import com.report.dailyreport.config.FeedProperties;
import com.report.dailyreport.config.FeedProperties.FeedSourceProperties;
import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.util.TextNormalizationUtils;
import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.ExchangeStrategies;

@Service
@RequiredArgsConstructor
@Slf4j
public class RssCollector {

    private final WebClient.Builder webClientBuilder;
    private final FeedProperties feedProperties;
    private final ReportProperties reportProperties;

    private WebClient feedClient() {
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                .build();

        return webClientBuilder.clone()
                .exchangeStrategies(strategies)
                .defaultHeader(HttpHeaders.USER_AGENT, "daily-report-bot/1.0")
                .defaultHeader(HttpHeaders.ACCEPT,
                        MediaType.APPLICATION_XML_VALUE + "," +
                                MediaType.TEXT_XML_VALUE + ",application/rss+xml,application/atom+xml")
                .build();
    }

    public List<CollectedArticle> collectAll() {
        List<CollectedArticle> articles = new ArrayList<>();
        feedProperties.getCategories().forEach((category, sources) -> {
            for (FeedSourceProperties source : sources) {
                articles.addAll(collectFromSource(category, source));
            }
        });

        articles.sort(Comparator.comparing(
                CollectedArticle::publishedAt,
                Comparator.nullsLast(Comparator.reverseOrder())
        ));
        log.info("Collected {} raw articles from {} categories", articles.size(), feedProperties.getCategories().size());
        return articles;
    }

    private List<CollectedArticle> collectFromSource(ReportCategory category, FeedSourceProperties source) {
        try {
            String payload = feedClient()
                    .get()
                    .uri(source.getUrl())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            if (payload == null || payload.isBlank()) {
                log.warn("RSS payload was empty: source={}, url={}", source.getName(), source.getUrl());
                return List.of();
            }

            byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
            try (ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
                 XmlReader reader = new XmlReader(inputStream)) {
                SyndFeed feed = new SyndFeedInput().build(reader);
                return feed.getEntries().stream()
                        .limit(reportProperties.getMaxEntriesPerSource())
                        .map(entry -> toArticle(category, source, entry))
                        .filter(article -> !article.title().isBlank() && !article.link().isBlank())
                        .toList();
            }
        } catch (Exception exception) {
            log.warn("RSS collection failed: source={}, url={}, message={}", source.getName(), source.getUrl(), exception.getMessage());
            return List.of();
        }
    }

    private CollectedArticle toArticle(ReportCategory category, FeedSourceProperties source, SyndEntry entry) {
        return new CollectedArticle(
                category,
                source.getName(),
                source.getUrl(),
                TextNormalizationUtils.stripHtml(entry.getTitle()),
                entry.getLink(),
                extractPublishedAt(entry),
                extractSummary(entry),
                source.getReliability(),
                source.getKeywords()
        );
    }

    private Instant extractPublishedAt(SyndEntry entry) {
        if (entry.getPublishedDate() != null) {
            return entry.getPublishedDate().toInstant();
        }
        if (entry.getUpdatedDate() != null) {
            return entry.getUpdatedDate().toInstant();
        }
        return null;
    }

    private String extractSummary(SyndEntry entry) {
        if (entry.getDescription() != null && entry.getDescription().getValue() != null) {
            return TextNormalizationUtils.stripHtml(entry.getDescription().getValue());
        }

        List<SyndContent> contents = entry.getContents();
        if (contents == null || contents.isEmpty()) {
            return "";
        }
        return contents.stream()
                .map(SyndContent::getValue)
                .filter(value -> value != null && !value.isBlank())
                .map(TextNormalizationUtils::stripHtml)
                .findFirst()
                .orElse("");
    }
}
