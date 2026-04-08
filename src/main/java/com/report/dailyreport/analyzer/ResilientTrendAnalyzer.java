package com.report.dailyreport.analyzer;

import com.report.dailyreport.config.OpenAiProperties;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class ResilientTrendAnalyzer implements TrendAnalyzer {

    private final OpenAiTrendAnalyzer openAiTrendAnalyzer;
    private final FallbackTrendAnalyzer fallbackTrendAnalyzer;
    private final OpenAiProperties openAiProperties;
    private final AtomicBoolean missingApiKeyLogged = new AtomicBoolean(false);

    @Override
    public List<CategoryTrendSection> analyze(Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        EnumMap<ReportCategory, List<RankedArticle>> requested = new EnumMap<>(ReportCategory.class);
        requested.putAll(articlesByCategory);

        if (!StringUtils.hasText(openAiProperties.getApiKey())) {
            logMissingKeyOnce();
            return fallbackTrendAnalyzer.analyze(requested);
        }

        EnumMap<ReportCategory, List<RankedArticle>> nonEmptyCategories = new EnumMap<>(ReportCategory.class);
        requested.forEach((category, articles) -> {
            if (articles != null && !articles.isEmpty()) {
                nonEmptyCategories.put(category, articles);
            }
        });

        if (nonEmptyCategories.isEmpty()) {
            return fallbackTrendAnalyzer.analyze(requested);
        }

        try {
            List<CategoryTrendSection> aiSections = openAiTrendAnalyzer.analyze(nonEmptyCategories);
            return mergeWithFallbackSections(requested, aiSections);
        } catch (Exception exception) {
            log.warn("OpenAI category analysis failed. Falling back for {} categories. message={}",
                    requested.size(), exception.getMessage());
            return fallbackTrendAnalyzer.analyze(requested);
        }
    }

    private List<CategoryTrendSection> mergeWithFallbackSections(
            Map<ReportCategory, List<RankedArticle>> requested,
            List<CategoryTrendSection> aiSections
    ) {
        Map<ReportCategory, CategoryTrendSection> aiByCategory = new EnumMap<>(ReportCategory.class);
        for (CategoryTrendSection aiSection : aiSections) {
            aiByCategory.put(aiSection.category(), aiSection);
        }

        List<CategoryTrendSection> merged = new ArrayList<>();
        for (Map.Entry<ReportCategory, List<RankedArticle>> entry : requested.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                CategoryTrendSection aiSection = aiByCategory.get(entry.getKey());
                if (aiSection != null) {
                    merged.add(aiSection);
                    continue;
                }
            }
            merged.add(singleFallback(entry.getKey(), entry.getValue()));
        }
        return merged;
    }

    private CategoryTrendSection singleFallback(ReportCategory category, List<RankedArticle> articles) {
        EnumMap<ReportCategory, List<RankedArticle>> fallbackInput = new EnumMap<>(ReportCategory.class);
        fallbackInput.put(category, articles == null ? List.of() : articles);
        return fallbackTrendAnalyzer.analyze(fallbackInput).getFirst();
    }

    private void logMissingKeyOnce() {
        if (missingApiKeyLogged.compareAndSet(false, true)) {
            log.warn("OPENAI_API_KEY is not configured. Falling back to rule-based analysis.");
        }
    }
}
