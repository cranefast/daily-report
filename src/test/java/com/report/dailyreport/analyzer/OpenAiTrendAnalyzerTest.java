package com.report.dailyreport.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.dailyreport.config.OpenAiProperties;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.util.PromptTemplateService;
import java.time.Instant;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

@ExtendWith(OutputCaptureExtension.class)
class OpenAiTrendAnalyzerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final OpenAiTrendAnalyzer analyzer = new OpenAiTrendAnalyzer(
            mock(WebClient.Builder.class),
            new OpenAiProperties(),
            mock(PromptTemplateService.class),
            objectMapper
    );

    @Test
    void rejectsIncompleteResponsesWithMaxOutputTokenHint() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "status": "incomplete",
                  "incomplete_details": {
                    "reason": "max_output_tokens"
                  }
                }
                """);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(analyzer, "validateStructuredResponse", response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reason=max_output_tokens")
                .hasMessageContaining("Increase openai.max-output-tokens or reduce report.top-n.");
    }

    @Test
    void rejectsRefusalResponsesBeforeParsing() throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "status": "completed",
                  "output": [
                    {
                      "content": [
                        {
                          "type": "refusal",
                          "refusal": "safety policy refusal"
                        }
                      ]
                    }
                  ]
                }
                """);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(analyzer, "validateStructuredResponse", response))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("OpenAI response was refused")
                .hasMessageContaining("safety policy refusal");
    }

    @Test
    void logsOutputPreviewWhenStructuredJsonParsingFails(CapturedOutput output) throws Exception {
        JsonNode response = objectMapper.readTree("""
                {
                  "status": "completed"
                }
                """);

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(
                analyzer,
                "parseSections",
                response,
                "{\"categories\":[",
                inputMap()
        )).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to parse OpenAI analysis response");

        assertThat(output.getOut()).contains("Failed to parse OpenAI analysis response. status=completed");
        assertThat(output.getOut()).contains("output_preview={\"categories\":[");
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
}
