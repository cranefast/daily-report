package com.report.dailyreport.ranker;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.util.TextNormalizationUtils;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportanceRanker {

    private static final double CLUSTER_TITLE_SIMILARITY_THRESHOLD = 0.72;
    private static final double CLUSTER_TEXT_SIMILARITY_THRESHOLD = 0.48;
    private static final double CLUSTER_SUPPORTING_TEXT_THRESHOLD = 0.30;

    private final ReportProperties reportProperties;
    private final Clock clock;

    public List<RankedArticle> rank(List<CollectedArticle> deduplicatedArticles, List<CollectedArticle> rawArticles) {
        Map<CollectedArticle, TopicClusterStats> clusterStatsByArticle = buildTopicClusterStats(rawArticles);
        List<RankedArticle> rankedArticles = new ArrayList<>();
        for (CollectedArticle article : deduplicatedArticles) {
            TopicClusterStats clusterStats = clusterStatsByArticle.getOrDefault(article, TopicClusterStats.single(article));
            rankedArticles.add(new RankedArticle(article, calculateScore(article, clusterStats)));
        }

        rankedArticles.sort(Comparator.comparing(
                rankedArticle -> rankedArticle.scoreBreakdown().totalScore(),
                Comparator.reverseOrder()
        ));
        return rankedArticles;
    }

    private ScoreBreakdown calculateScore(CollectedArticle article, TopicClusterStats clusterStats) {
        ReportProperties.Scoring scoring = reportProperties.getScoring();
        double recencyScore = calculateRecencyScore(article.publishedAt());
        double keywordScore = calculateKeywordScore(article, clusterStats);
        double reliabilityScore = clamp(article.sourceReliability());
        double repetitionScore = calculateRepetitionScore(clusterStats);

        double total = 100 * (
                (recencyScore * scoring.getRecencyWeight())
                        + (keywordScore * scoring.getKeywordWeight())
                        + (reliabilityScore * scoring.getSourceReliabilityWeight())
                        + (repetitionScore * scoring.getRepetitionWeight())
        );

        return new ScoreBreakdown(
                total,
                recencyScore,
                keywordScore,
                reliabilityScore,
                repetitionScore,
                scoring.getRecencyWeight(),
                scoring.getKeywordWeight(),
                scoring.getSourceReliabilityWeight(),
                scoring.getRepetitionWeight(),
                clusterStats.articleCount()
        );
    }

    private double calculateRecencyScore(Instant publishedAt) {
        if (publishedAt == null) {
            return 0.35;
        }
        long ageHours = Math.max(0, Duration.between(publishedAt, Instant.now(clock)).toHours());
        double lookbackHours = Math.max(1, reportProperties.getLookbackHours());
        return clamp(Math.exp(-ageHours / lookbackHours));
    }

    private double calculateKeywordScore(CollectedArticle article, TopicClusterStats clusterStats) {
        Set<String> articleTokens = clusterStats.tokensByArticle().getOrDefault(
                article,
                TextNormalizationUtils.tokenizeAll(article.title(), article.summary())
        );
        if (articleTokens.isEmpty()) {
            return 0.0;
        }

        String text = (article.title() + " " + article.summary()).toLowerCase(Locale.ROOT);
        long sourceMatches = article.sourceKeywords().stream()
                .filter(keyword -> text.contains(keyword.toLowerCase(Locale.ROOT)))
                .count();
        long repeatedTermMatches = clusterStats.repeatedTokens().stream()
                .filter(articleTokens::contains)
                .count();

        double sourceKeywordScore = clamp(sourceMatches / 4.0);
        if (clusterStats.articleCount() <= 1) {
            return sourceKeywordScore;
        }

        double repeatedKeywordScore = clamp(repeatedTermMatches / 3.0);
        return clamp((repeatedKeywordScore * 0.75) + (sourceKeywordScore * 0.25));
    }

    private double calculateRepetitionScore(TopicClusterStats clusterStats) {
        if (clusterStats.articleCount() <= 1) {
            return 0.15;
        }

        double articleBoost = (clusterStats.articleCount() - 1) * 0.25;
        double sourceBoost = (clusterStats.uniqueSourceCount() - 1) * 0.20;
        return clamp(0.15 + articleBoost + sourceBoost);
    }

    private Map<CollectedArticle, TopicClusterStats> buildTopicClusterStats(List<CollectedArticle> rawArticles) {
        List<CollectedArticle> sortedArticles = rawArticles.stream()
                .sorted(Comparator
                        .comparing(CollectedArticle::publishedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(CollectedArticle::sourceReliability, Comparator.reverseOrder()))
                .toList();

        List<TopicCluster> clusters = new ArrayList<>();
        for (CollectedArticle article : sortedArticles) {
            TopicCluster targetCluster = null;
            double bestScore = 0.0;

            for (TopicCluster cluster : clusters) {
                if (cluster.category() != article.category()) {
                    continue;
                }
                for (CollectedArticle existing : cluster.articles()) {
                    TopicSimilarity similarity = topicSimilarity(article, existing);
                    if (similarity.matches() && similarity.score() > bestScore) {
                        bestScore = similarity.score();
                        targetCluster = cluster;
                    }
                }
            }

            if (targetCluster == null) {
                List<CollectedArticle> articles = new ArrayList<>();
                articles.add(article);
                clusters.add(new TopicCluster(article.category(), articles));
                continue;
            }

            targetCluster.articles().add(article);
        }

        Map<CollectedArticle, TopicClusterStats> clusterStatsByArticle = new HashMap<>();
        for (TopicCluster cluster : clusters) {
            TopicClusterStats stats = toClusterStats(cluster);
            for (CollectedArticle article : cluster.articles()) {
                clusterStatsByArticle.put(article, stats);
            }
        }
        return clusterStatsByArticle;
    }

    private TopicClusterStats toClusterStats(TopicCluster cluster) {
        Map<CollectedArticle, Set<String>> tokensByArticle = new HashMap<>();
        Map<String, Integer> tokenFrequencies = new HashMap<>();
        Set<String> uniqueSources = new HashSet<>();

        for (CollectedArticle article : cluster.articles()) {
            Set<String> tokens = TextNormalizationUtils.tokenizeAll(article.title(), article.summary());
            tokensByArticle.put(article, tokens);
            uniqueSources.add(article.sourceName());
            for (String token : tokens) {
                tokenFrequencies.merge(token, 1, Integer::sum);
            }
        }

        Set<String> repeatedTokens = tokenFrequencies.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        return new TopicClusterStats(cluster.articles().size(), uniqueSources.size(), repeatedTokens, tokensByArticle);
    }

    private TopicSimilarity topicSimilarity(CollectedArticle left, CollectedArticle right) {
        double titleSimilarity = TextNormalizationUtils.jaccardSimilarity(left.title(), right.title());
        double textSimilarity = TextNormalizationUtils.jaccardSimilarity(
                left.title() + " " + left.summary(),
                right.title() + " " + right.summary()
        );

        boolean matches = (titleSimilarity >= CLUSTER_TITLE_SIMILARITY_THRESHOLD
                && textSimilarity >= CLUSTER_SUPPORTING_TEXT_THRESHOLD)
                || textSimilarity >= CLUSTER_TEXT_SIMILARITY_THRESHOLD;
        return new TopicSimilarity(titleSimilarity, textSimilarity, Math.max(titleSimilarity, textSimilarity), matches);
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private record TopicCluster(ReportCategory category, List<CollectedArticle> articles) {
    }

    private record TopicClusterStats(
            int articleCount,
            int uniqueSourceCount,
            Set<String> repeatedTokens,
            Map<CollectedArticle, Set<String>> tokensByArticle
    ) {

        private static TopicClusterStats single(CollectedArticle article) {
            return new TopicClusterStats(
                    1,
                    1,
                    Set.of(),
                    Map.of(article, TextNormalizationUtils.tokenizeAll(article.title(), article.summary()))
            );
        }
    }

    private record TopicSimilarity(double titleSimilarity, double textSimilarity, double score, boolean matches) {
    }
}
