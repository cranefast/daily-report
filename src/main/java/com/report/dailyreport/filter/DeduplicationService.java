package com.report.dailyreport.filter;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.util.TextNormalizationUtils;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeduplicationService {

    private final ReportProperties reportProperties;

    public List<CollectedArticle> deduplicate(List<CollectedArticle> articles) {
        List<CollectedArticle> sorted = articles.stream()
                .sorted(Comparator
                        .comparing(CollectedArticle::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CollectedArticle::sourceReliability, Comparator.reverseOrder()))
                .toList();

        Set<String> seenUrls = new HashSet<>();
        List<CollectedArticle> unique = new ArrayList<>();
        for (CollectedArticle article : sorted) {
            String normalizedUrl = normalizeUrl(article.link());
            if (!normalizedUrl.isBlank() && seenUrls.contains(normalizedUrl)) {
                continue;
            }

            boolean similarTitleExists = unique.stream()
                    .anyMatch(existing -> titleSimilarity(article.title(), existing.title())
                            >= reportProperties.getDuplicateTitleSimilarityThreshold());
            if (similarTitleExists) {
                continue;
            }

            if (!normalizedUrl.isBlank()) {
                seenUrls.add(normalizedUrl);
            }
            unique.add(article);
        }
        return unique;
    }

    public double titleSimilarity(String left, String right) {
        return TextNormalizationUtils.jaccardSimilarity(left, right);
    }

    private String normalizeUrl(String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        try {
            URI uri = new URI(url);
            String query = uri.getQuery();
            String sanitizedQuery = null;
            if (query != null && !query.isBlank()) {
                sanitizedQuery = List.of(query.split("&")).stream()
                        .filter(parameter -> !parameter.startsWith("utm_"))
                        .filter(parameter -> !parameter.startsWith("fbclid="))
                        .reduce((left, right) -> left + "&" + right)
                        .orElse(null);
            }
            return new URI(
                    uri.getScheme(),
                    uri.getAuthority(),
                    uri.getPath(),
                    sanitizedQuery,
                    null
            ).toString();
        } catch (URISyntaxException ignored) {
            return url.trim();
        }
    }
}
