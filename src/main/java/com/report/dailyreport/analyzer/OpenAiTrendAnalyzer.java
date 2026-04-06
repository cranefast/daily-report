package com.report.dailyreport.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.dailyreport.config.OpenAiProperties;
import com.report.dailyreport.model.ArticleAnalysis;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.util.PromptTemplateService;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiTrendAnalyzer implements TrendAnalyzer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm z");

    private final WebClient.Builder webClientBuilder;
    private final OpenAiProperties openAiProperties;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;
    private final FallbackTrendAnalyzer fallbackTrendAnalyzer;

    @Override
    public ArticleAnalysis analyze(RankedArticle article) {
        if (!StringUtils.hasText(openAiProperties.getApiKey())) {
            log.warn("OPENAI_API_KEY is not configured. Falling back to rule-based analysis.");
            return fallbackTrendAnalyzer.analyze(article);
        }

        try {
            JsonNode requestBody = buildRequestBody(article);
            JsonNode response = webClientBuilder.build()
                    .post()
                    .uri(openAiProperties.getBaseUrl() + "/responses")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block(openAiProperties.getTimeout());

            String outputText = extractOutputText(response);
            return parseAnalysis(outputText);
        } catch (Exception exception) {
            log.warn("OpenAI analysis failed for title='{}'. Falling back. message={}",
                    article.article().title(), exception.getMessage());
            return fallbackTrendAnalyzer.analyze(article);
        }
    }

    private JsonNode buildRequestBody(RankedArticle article) {
        String developerPrompt = promptTemplateService.load("prompts/article-analysis-system.txt");
        String userPrompt = promptTemplateService.render("prompts/article-analysis-user.txt", buildPromptVariables(article));
        JsonNode input = objectMapper.createArrayNode()
                .add(objectMapper.createObjectNode()
                        .put("role", "developer")
                        .set("content", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .put("type", "input_text")
                                        .put("text", developerPrompt))))
                .add(objectMapper.createObjectNode()
                        .put("role", "user")
                        .set("content", objectMapper.createArrayNode()
                                .add(objectMapper.createObjectNode()
                                        .put("type", "input_text")
                                        .put("text", userPrompt))));

        var request = objectMapper.createObjectNode();
        request.put("model", openAiProperties.getModel());
        request.set("input", input);
        request.set("reasoning", objectMapper.createObjectNode().put("effort", openAiProperties.getReasoningEffort()));
        request.set("text", objectMapper.createObjectNode().put("verbosity", openAiProperties.getVerbosity()));
        return request;
    }

    private Map<String, String> buildPromptVariables(RankedArticle rankedArticle) {
        CollectedArticle article = rankedArticle.article();
        ScoreBreakdown score = rankedArticle.scoreBreakdown();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("category", article.category().getDisplayName());
        values.put("category_guidance", article.category().getGuidance());
        values.put("title", article.title());
        values.put("source_name", article.sourceName());
        values.put("source_url", article.sourceUrl());
        values.put("article_url", article.link());
        values.put("published_at", article.publishedAt() == null
                ? "발행 시각 미확인"
                : DATE_TIME_FORMATTER.format(article.publishedAt().atZone(ZoneId.of("Asia/Seoul"))));
        values.put("summary", article.summary());
        values.put("score_total", "%.2f".formatted(score.totalScore()));
        values.put("score_breakdown", """
                - recency: %.2f x %.2f
                - keyword: %.2f x %.2f
                - sourceReliability: %.2f x %.2f
                - repetition: %.2f x %.2f (mentions=%d)
                """.formatted(
                score.recencyScore(), score.recencyWeight(),
                score.keywordScore(), score.keywordWeight(),
                score.sourceReliabilityScore(), score.sourceReliabilityWeight(),
                score.repetitionScore(), score.repetitionWeight(),
                score.repetitionCount()
        ));
        return values;
    }

    private String extractOutputText(JsonNode response) {
        if (response == null || response.isMissingNode()) {
            throw new IllegalStateException("OpenAI response is empty");
        }

        JsonNode outputText = response.get("output_text");
        if (outputText != null && outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        StringBuilder builder = new StringBuilder();
        JsonNode outputs = response.path("output");
        if (outputs.isArray()) {
            for (JsonNode output : outputs) {
                JsonNode contents = output.path("content");
                if (!contents.isArray()) {
                    continue;
                }
                for (JsonNode content : contents) {
                    JsonNode textNode = content.get("text");
                    if (textNode != null && textNode.isTextual()) {
                        if (!builder.isEmpty()) {
                            builder.append('\n');
                        }
                        builder.append(textNode.asText());
                    }
                }
            }
        }

        if (!builder.isEmpty()) {
            return builder.toString();
        }
        throw new IllegalStateException("OpenAI response did not contain text content");
    }

    private ArticleAnalysis parseAnalysis(String rawText) throws Exception {
        String sanitized = rawText.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();
        JsonNode root = objectMapper.readTree(sanitized);

        return new ArticleAnalysis(
                root.path("summary").asText("요약을 생성하지 못했습니다."),
                root.path("detailed_explanation").asText("상세 설명을 생성하지 못했습니다."),
                root.path("importance_reason").asText("중요 이유를 생성하지 못했습니다."),
                root.path("industry_impact").asText("산업 영향 설명을 생성하지 못했습니다."),
                root.path("practical_implications").asText("실무 시사점을 생성하지 못했습니다."),
                readFollowUpPoints(root.path("follow_up_points")),
                true
        );
    }

    private List<String> readFollowUpPoints(JsonNode node) {
        if (!node.isArray()) {
            return List.of("후속 체크 포인트를 생성하지 못했습니다.");
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }
}
