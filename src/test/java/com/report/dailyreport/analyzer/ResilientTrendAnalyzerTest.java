package com.report.dailyreport.analyzer;

import com.report.dailyreport.config.OpenAiProperties;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResilientTrendAnalyzerTest {

    @Test
    void usesFallbackWhenApiKeyIsMissing() {
        OpenAiTrendAnalyzer openAiTrendAnalyzer = mock(OpenAiTrendAnalyzer.class);
        FallbackTrendAnalyzer fallbackTrendAnalyzer = mock(FallbackTrendAnalyzer.class);
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("");
        ResilientTrendAnalyzer analyzer = new ResilientTrendAnalyzer(openAiTrendAnalyzer, fallbackTrendAnalyzer, openAiProperties);

        Map<ReportCategory, List<RankedArticle>> input = inputMap();
        List<CategoryTrendSection> fallback = List.of(sampleSection(false));
        when(fallbackTrendAnalyzer.analyze(input)).thenReturn(fallback);

        List<CategoryTrendSection> result = analyzer.analyze(input);

        assertThat(result).isEqualTo(fallback);
        verify(fallbackTrendAnalyzer).analyze(input);
    }

    @Test
    void usesFallbackWhenOpenAiCallFails() {
        OpenAiTrendAnalyzer openAiTrendAnalyzer = mock(OpenAiTrendAnalyzer.class);
        FallbackTrendAnalyzer fallbackTrendAnalyzer = mock(FallbackTrendAnalyzer.class);
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("test-key");
        ResilientTrendAnalyzer analyzer = new ResilientTrendAnalyzer(openAiTrendAnalyzer, fallbackTrendAnalyzer, openAiProperties);

        Map<ReportCategory, List<RankedArticle>> input = inputMap();
        List<CategoryTrendSection> fallback = List.of(sampleSection(false));
        doThrow(new IllegalStateException("boom")).when(openAiTrendAnalyzer).analyze(input);
        when(fallbackTrendAnalyzer.analyze(input)).thenReturn(fallback);

        List<CategoryTrendSection> result = analyzer.analyze(input);

        assertThat(result).isEqualTo(fallback);
        verify(openAiTrendAnalyzer).analyze(input);
        verify(fallbackTrendAnalyzer).analyze(input);
    }

    @Test
    void usesOpenAiAnalysisWhenApiKeyExists() {
        OpenAiTrendAnalyzer openAiTrendAnalyzer = mock(OpenAiTrendAnalyzer.class);
        FallbackTrendAnalyzer fallbackTrendAnalyzer = mock(FallbackTrendAnalyzer.class);
        OpenAiProperties openAiProperties = new OpenAiProperties();
        openAiProperties.setApiKey("test-key");
        ResilientTrendAnalyzer analyzer = new ResilientTrendAnalyzer(openAiTrendAnalyzer, fallbackTrendAnalyzer, openAiProperties);

        Map<ReportCategory, List<RankedArticle>> input = inputMap();
        List<CategoryTrendSection> openAiSections = List.of(sampleSection(true));
        when(openAiTrendAnalyzer.analyze(input)).thenReturn(openAiSections);

        List<CategoryTrendSection> result = analyzer.analyze(input);

        assertThat(result).isEqualTo(openAiSections);
        verify(openAiTrendAnalyzer).analyze(input);
    }

    private Map<ReportCategory, List<RankedArticle>> inputMap() {
        EnumMap<ReportCategory, List<RankedArticle>> input = new EnumMap<>(ReportCategory.class);
        input.put(ReportCategory.AI, List.of(sampleArticle()));
        return input;
    }

    private RankedArticle sampleArticle() {
        return new RankedArticle(
                new CollectedArticle(
                        ReportCategory.AI,
                        "OpenAI News",
                        "https://openai.com/news/rss.xml",
                        "OpenAI ships a new model",
                        "https://openai.com/news/model",
                        Instant.parse("2026-04-07T00:00:00Z"),
                        "summary",
                        0.98,
                        List.of("model")
                ),
                new ScoreBreakdown(90.0, 0.9, 0.8, 0.98, 0.2, 0.35, 0.30, 0.20, 0.15, 1)
        );
    }

    private CategoryTrendSection sampleSection(boolean generatedByAi) {
        return new CategoryTrendSection(
                ReportCategory.AI,
                new CategoryInsight(
                        "summary",
                        "detail",
                        "impact",
                        "practical",
                        List.of("a", "b", "c"),
                        generatedByAi
                ),
                List.of()
        );
    }
}
