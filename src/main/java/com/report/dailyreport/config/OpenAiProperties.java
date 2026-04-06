package com.report.dailyreport.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "openai")
public class OpenAiProperties {

    private String apiKey;
    private String baseUrl = "https://api.openai.com/v1";
    private String model = "gpt-5.4";
    private String reasoningEffort = "low";
    private String verbosity = "medium";
    private Duration timeout = Duration.ofSeconds(45);
}
