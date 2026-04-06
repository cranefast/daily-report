package com.report.dailyreport.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "notification")
public class NotificationProperties {

    private Email email = new Email();
    private Slack slack = new Slack();

    @Getter
    @Setter
    public static class Email {

        private String to;
        private String from;
        private String subjectPrefix = "[Daily Report]";
    }

    @Getter
    @Setter
    public static class Slack {

        private String webhookUrl;
    }
}
