package com.report.dailyreport.notifier;

import com.report.dailyreport.config.NotificationProperties;
import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.TrendReport;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
@Slf4j
public class SlackNotifier implements Notifier {

    private final WebClient.Builder webClientBuilder;
    private final NotificationProperties notificationProperties;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.SLACK;
    }

    @Override
    public void send(TrendReport report) {
        if (!StringUtils.hasText(notificationProperties.getSlack().getWebhookUrl())) {
            throw new IllegalStateException("notification.slack.webhook-url is not configured");
        }

        webClientBuilder.build()
                .post()
                .uri(notificationProperties.getSlack().getWebhookUrl())
                .bodyValue(Map.of("text", report.markdown()))
                .retrieve()
                .bodyToMono(String.class)
                .block();
        log.info("Daily report posted to Slack webhook");
    }
}
