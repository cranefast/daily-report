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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ImportanceRanker {

    private static final Map<ReportCategory, Set<String>> CATEGORY_KEYWORDS = buildCategoryKeywords();

    private final ReportProperties reportProperties;
    private final Clock clock;

    public List<RankedArticle> rank(List<CollectedArticle> deduplicatedArticles, List<CollectedArticle> rawArticles) {
        Map<String, Integer> repetitionCounts = buildRepetitionCounts(rawArticles);
        List<RankedArticle> rankedArticles = new ArrayList<>();
        for (CollectedArticle article : deduplicatedArticles) {
            int repetitionCount = repetitionCounts.getOrDefault(TextNormalizationUtils.themeKey(article.title()), 1);
            rankedArticles.add(new RankedArticle(article, calculateScore(article, repetitionCount)));
        }

        rankedArticles.sort(Comparator.comparing(
                rankedArticle -> rankedArticle.scoreBreakdown().totalScore(),
                Comparator.reverseOrder()
        ));
        return rankedArticles;
    }

    private ScoreBreakdown calculateScore(CollectedArticle article, int repetitionCount) {
        ReportProperties.Scoring scoring = reportProperties.getScoring();
        double recencyScore = calculateRecencyScore(article.publishedAt());
        double keywordScore = calculateKeywordScore(article);
        double reliabilityScore = clamp(article.sourceReliability());
        double repetitionScore = calculateRepetitionScore(repetitionCount);

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
                repetitionCount
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

    private double calculateKeywordScore(CollectedArticle article) {
        String text = (article.title() + " " + article.summary()).toLowerCase(Locale.ROOT);
        Set<String> categoryKeywords = CATEGORY_KEYWORDS.getOrDefault(article.category(), Set.of());
        long sourceMatches = article.sourceKeywords().stream()
                .filter(keyword -> text.contains(keyword.toLowerCase(Locale.ROOT)))
                .count();
        long categoryMatches = categoryKeywords.stream()
                .filter(text::contains)
                .count();
        return clamp((sourceMatches + categoryMatches) / 4.0);
    }

    private double calculateRepetitionScore(int repetitionCount) {
        if (repetitionCount <= 1) {
            return 0.2;
        }
        return clamp(0.2 + ((repetitionCount - 1) * 0.4));
    }

    private Map<String, Integer> buildRepetitionCounts(List<CollectedArticle> rawArticles) {
        Map<String, Integer> counts = new HashMap<>();
        for (CollectedArticle article : rawArticles) {
            String key = TextNormalizationUtils.themeKey(article.title());
            if (key.isBlank()) {
                continue;
            }
            counts.merge(key, 1, Integer::sum);
        }
        return counts;
    }

    private static Map<ReportCategory, Set<String>> buildCategoryKeywords() {
        Map<ReportCategory, Set<String>> keywords = new EnumMap<>(ReportCategory.class);
        keywords.put(ReportCategory.AI, Set.of("ai", "llm", "model", "agent", "inference", "gpu", "semiconductor"));
        keywords.put(ReportCategory.DEVELOPMENT, Set.of("release", "framework", "sdk", "developer", "api", "platform"));
        keywords.put(ReportCategory.KOREA_REAL_ESTATE, Set.of(
                "부동산", "주택", "아파트", "전세", "매매", "청약", "분양", "재건축", "재개발", "공급", "housing", "apartment"
        ));
        keywords.put(ReportCategory.KOREA_ECONOMY, Set.of(
                "금리", "기준금리", "물가", "환율", "수출", "성장률", "고용", "소비", "inflation", "exports", "growth"
        ));
        keywords.put(ReportCategory.GLOBAL_ECONOMY, Set.of(
                "inflation", "gdp", "rates", "employment", "consumer", "exports", "fed", "tariff", "oil", "global economy", "us economy"
        ));
        return keywords;
    }

    private double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}
