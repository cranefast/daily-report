package com.report.dailyreport.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.jsoup.Jsoup;

public final class TextNormalizationUtils {

    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\s]");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Set<String> STOP_WORDS = Set.of(
            "the", "and", "for", "with", "from", "that", "this", "into", "amid", "after",
            "over", "under", "about", "today", "news", "says", "will", "than",
            "report", "reports", "update", "latest", "more", "your", "their", "what",
            "when", "where", "how", "why", "are", "was", "were", "have", "has", "had"
    );

    private TextNormalizationUtils() {
    }

    public static String stripHtml(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return Jsoup.parse(value).text();
    }

    public static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "";
        }
        String withoutSourceSuffix = title.replaceAll("\\s+-\\s+[^-]{1,40}$", "");
        String normalized = NON_ALPHANUMERIC.matcher(withoutSourceSuffix.toLowerCase(Locale.ROOT)).replaceAll(" ");
        return MULTI_SPACE.matcher(normalized).replaceAll(" ").trim();
    }

    public static Set<String> tokenize(String value) {
        String normalized = normalizeTitle(stripHtml(value));
        if (normalized.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(normalized.split(" "))
                .filter(token -> token.length() >= 3)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public static double jaccardSimilarity(String left, String right) {
        Set<String> leftTokens = tokenize(left);
        Set<String> rightTokens = tokenize(right);
        if (leftTokens.isEmpty() || rightTokens.isEmpty()) {
            return normalizeTitle(left).equals(normalizeTitle(right)) ? 1.0 : 0.0;
        }

        Set<String> intersection = new LinkedHashSet<>(leftTokens);
        intersection.retainAll(rightTokens);

        Set<String> union = new LinkedHashSet<>(leftTokens);
        union.addAll(rightTokens);

        if (union.isEmpty()) {
            return 0.0;
        }

        double jaccard = (double) intersection.size() / union.size();
        double containment = (double) intersection.size() / Math.min(leftTokens.size(), rightTokens.size());
        return Math.max(jaccard, containment);
    }

    public static String themeKey(String title) {
        return tokenize(title).stream().limit(6).collect(Collectors.joining(" "));
    }

    public static String shorten(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, Math.max(0, maxLength - 1)).trim() + "…";
    }
}
