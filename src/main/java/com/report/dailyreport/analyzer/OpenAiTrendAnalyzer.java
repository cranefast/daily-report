package com.report.dailyreport.analyzer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.report.dailyreport.config.OpenAiProperties;
import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.util.PromptTemplateService;
import com.report.dailyreport.util.TextNormalizationUtils;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class OpenAiTrendAnalyzer implements TrendAnalyzer {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId ASIA_SEOUL = ZoneId.of("Asia/Seoul");

    private final WebClient.Builder webClientBuilder;
    private final OpenAiProperties openAiProperties;
    private final PromptTemplateService promptTemplateService;
    private final ObjectMapper objectMapper;

    @Override
    public List<CategoryTrendSection> analyze(Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        JsonNode requestBody = buildRequestBody(articlesByCategory);
        JsonNode response = webClientBuilder.build()
                .post()
                .uri(openAiProperties.getBaseUrl() + "/responses")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + openAiProperties.getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block(openAiProperties.getTimeout());

        logUsage(response, articlesByCategory);
        String outputText = extractOutputText(response);
        return parseSections(outputText, articlesByCategory);
    }

    private JsonNode buildRequestBody(Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        String developerPrompt = promptTemplateService.load("prompts/article-analysis-system.txt");
        String userPrompt = promptTemplateService.render(
                "prompts/article-analysis-user.txt",
                buildPromptVariables(articlesByCategory)
        );
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
        request.put("max_output_tokens", openAiProperties.getMaxOutputTokens());
        request.set("text", objectMapper.createObjectNode()
                .put("verbosity", openAiProperties.getVerbosity())
                .set("format", buildResponseFormat()));
        return request;
    }

    private JsonNode buildResponseFormat() {
        var articleHighlightProperties = objectMapper.createObjectNode();
        articleHighlightProperties.set("id", objectMapper.createObjectNode().put("type", "string"));
        articleHighlightProperties.set("highlight", objectMapper.createObjectNode().put("type", "string"));

        var articleHighlightSchema = objectMapper.createObjectNode();
        articleHighlightSchema.put("type", "object");
        articleHighlightSchema.set("properties", articleHighlightProperties);
        articleHighlightSchema.putArray("required").add("id").add("highlight");
        articleHighlightSchema.put("additionalProperties", false);

        var followUpPointsSchema = objectMapper.createObjectNode();
        followUpPointsSchema.put("type", "array");
        followUpPointsSchema.set("items", objectMapper.createObjectNode().put("type", "string"));

        var articleHighlightsSchema = objectMapper.createObjectNode();
        articleHighlightsSchema.put("type", "array");
        articleHighlightsSchema.set("items", articleHighlightSchema);

        var categoryProperties = objectMapper.createObjectNode();
        categoryProperties.set("category", objectMapper.createObjectNode().put("type", "string"));
        categoryProperties.set("summary", objectMapper.createObjectNode().put("type", "string"));
        categoryProperties.set("detailed_report", objectMapper.createObjectNode().put("type", "string"));
        categoryProperties.set("industry_impact", objectMapper.createObjectNode().put("type", "string"));
        categoryProperties.set("practical_implications", objectMapper.createObjectNode().put("type", "string"));
        categoryProperties.set("follow_up_points", followUpPointsSchema);
        categoryProperties.set("article_highlights", articleHighlightsSchema);

        var categorySchema = objectMapper.createObjectNode();
        categorySchema.put("type", "object");
        categorySchema.set("properties", categoryProperties);
        categorySchema.putArray("required")
                .add("category")
                .add("summary")
                .add("detailed_report")
                .add("industry_impact")
                .add("practical_implications")
                .add("follow_up_points")
                .add("article_highlights");
        categorySchema.put("additionalProperties", false);

        var categoriesSchema = objectMapper.createObjectNode();
        categoriesSchema.put("type", "array");
        categoriesSchema.set("items", categorySchema);

        var schemaProperties = objectMapper.createObjectNode();
        schemaProperties.set("categories", categoriesSchema);

        var schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        schema.set("properties", schemaProperties);
        schema.putArray("required").add("categories");
        schema.put("additionalProperties", false);

        return objectMapper.createObjectNode()
                .put("type", "json_schema")
                .put("name", "category_trend_report")
                .put("strict", true)
                .set("schema", schema);
    }

    private Map<String, String> buildPromptVariables(Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("category_count", Integer.toString(articlesByCategory.size()));
        values.put("article_count", Integer.toString(articlesByCategory.values().stream().mapToInt(List::size).sum()));
        values.put("categories_payload", buildCategoriesPayload(articlesByCategory));
        return values;
    }

    private String buildCategoriesPayload(Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        List<Map<String, Object>> payload = new ArrayList<>();
        for (Map.Entry<ReportCategory, List<RankedArticle>> entry : articlesByCategory.entrySet()) {
            List<Map<String, Object>> articles = new ArrayList<>();
            for (int index = 0; index < entry.getValue().size(); index++) {
                RankedArticle rankedArticle = entry.getValue().get(index);
                CollectedArticle article = rankedArticle.article();
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", articleId(entry.getKey(), index));
                item.put("rank", index + 1);
                item.put("title", article.title());
                item.put("publishedAt", article.publishedAt() == null
                        ? "unknown"
                        : DATE_TIME_FORMATTER.format(article.publishedAt().atZone(ASIA_SEOUL)));
                item.put("score", "%.1f".formatted(rankedArticle.scoreBreakdown().totalScore()));
                item.put("summary", compactSummary(article.summary(), article.title()));
                articles.add(item);
            }

            Map<String, Object> categoryPayload = new LinkedHashMap<>();
            categoryPayload.put("category", entry.getKey().name());
            categoryPayload.put("articles", articles);
            payload.add(categoryPayload);
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to serialize category prompt payload", exception);
        }
    }

    private String compactSummary(String summary, String fallbackTitle) {
        String base = summary == null || summary.isBlank() ? fallbackTitle : summary;
        return TextNormalizationUtils.shorten(base, 180);
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

    private List<CategoryTrendSection> parseSections(
            String rawText,
            Map<ReportCategory, List<RankedArticle>> articlesByCategory
    ) {
        String sanitized = rawText.trim()
                .replace("```json", "")
                .replace("```", "")
                .trim();
        JsonNode root;
        try {
            root = objectMapper.readTree(sanitized);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to parse OpenAI analysis response", exception);
        }

        JsonNode categoriesNode = root.path("categories");
        if (!categoriesNode.isArray()) {
            throw new IllegalStateException("OpenAI response did not contain a categories array");
        }
        if (categoriesNode.size() != articlesByCategory.size()) {
            throw new IllegalStateException("OpenAI returned unexpected category count");
        }

        Map<ReportCategory, CategoryTrendSection> sectionsByCategory = new EnumMap<>(ReportCategory.class);
        for (JsonNode categoryNode : categoriesNode) {
            ReportCategory category = parseCategory(categoryNode.path("category").asText());
            if (sectionsByCategory.containsKey(category)) {
                throw new IllegalStateException("OpenAI returned a duplicated category: " + category);
            }

            List<RankedArticle> expectedArticles = articlesByCategory.get(category);
            if (expectedArticles == null) {
                throw new IllegalStateException("OpenAI returned an unexpected category: " + category);
            }

            CategoryInsight insight = new CategoryInsight(
                    categoryNode.path("summary").asText("요약을 생성하지 못했습니다."),
                    categoryNode.path("detailed_report").asText("상세 리포트를 생성하지 못했습니다."),
                    categoryNode.path("industry_impact").asText("산업 영향을 생성하지 못했습니다."),
                    categoryNode.path("practical_implications").asText("실무 시사점을 생성하지 못했습니다."),
                    readFollowUpPoints(categoryNode.path("follow_up_points")),
                    true
            );
            List<ArticleHighlight> articleHighlights = readArticleHighlights(
                    categoryNode.path("article_highlights"),
                    category,
                    expectedArticles
            );
            sectionsByCategory.put(category, new CategoryTrendSection(category, insight, articleHighlights));
        }

        List<CategoryTrendSection> sections = new ArrayList<>();
        for (ReportCategory category : articlesByCategory.keySet()) {
            CategoryTrendSection section = sectionsByCategory.get(category);
            if (section == null) {
                throw new IllegalStateException("OpenAI response missed category: " + category);
            }
            sections.add(section);
        }
        return sections;
    }

    private ReportCategory parseCategory(String rawValue) {
        try {
            return ReportCategory.valueOf(rawValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException("OpenAI returned unknown category: " + rawValue, exception);
        }
    }

    private List<ArticleHighlight> readArticleHighlights(
            JsonNode node,
            ReportCategory category,
            List<RankedArticle> expectedArticles
    ) {
        if (!node.isArray() || node.size() != expectedArticles.size()) {
            throw new IllegalStateException("OpenAI returned an unexpected article highlight count for " + category);
        }

        Map<String, String> highlightById = new LinkedHashMap<>();
        for (JsonNode articleNode : node) {
            String articleId = articleNode.path("id").asText();
            if (articleId.isBlank() || highlightById.put(articleId, articleNode.path("highlight").asText()) != null) {
                throw new IllegalStateException("OpenAI returned duplicated article highlight ids for " + category);
            }
        }

        List<ArticleHighlight> highlights = new ArrayList<>();
        for (int index = 0; index < expectedArticles.size(); index++) {
            String expectedId = articleId(category, index);
            String highlight = highlightById.get(expectedId);
            if (highlight == null || highlight.isBlank()) {
                throw new IllegalStateException("OpenAI omitted highlight for article id=" + expectedId);
            }
            highlights.add(new ArticleHighlight(expectedArticles.get(index), highlight));
        }
        return highlights;
    }

    private List<String> readFollowUpPoints(JsonNode node) {
        if (!node.isArray()) {
            return List.of("후속 체크 포인트를 생성하지 못했습니다.");
        }
        return objectMapper.convertValue(node, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
    }

    private String articleId(ReportCategory category, int index) {
        return category.name() + "-" + (index + 1);
    }

    private void logUsage(JsonNode response, Map<ReportCategory, List<RankedArticle>> articlesByCategory) {
        JsonNode usage = response.path("usage");
        if (usage.isMissingNode()) {
            return;
        }

        int inputTokens = usage.path("input_tokens").asInt(0);
        int outputTokens = usage.path("output_tokens").asInt(0);
        int cachedTokens = usage.path("input_tokens_details").path("cached_tokens").asInt(0);
        int reasoningTokens = usage.path("output_tokens_details").path("reasoning_tokens").asInt(0);
        int articleCount = articlesByCategory.values().stream().mapToInt(List::size).sum();
        log.info(
                "OpenAI usage: model={}, categories={}, articles={}, input_tokens={}, cached_tokens={}, output_tokens={}, reasoning_tokens={}",
                openAiProperties.getModel(),
                articlesByCategory.size(),
                articleCount,
                inputTokens,
                cachedTokens,
                outputTokens,
                reasoningTokens
        );
    }
}
