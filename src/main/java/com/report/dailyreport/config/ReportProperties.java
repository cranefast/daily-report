package com.report.dailyreport.config;

import com.report.dailyreport.model.NotificationChannel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "report")
public class ReportProperties {

    private int topN = 3;
    private boolean dryRun = true;
    private NotificationChannel channel = NotificationChannel.EMAIL;
    private String zoneId = "Asia/Seoul";
    private double duplicateTitleSimilarityThreshold = 0.82;
    private long lookbackHours = 120;
    private int maxEntriesPerSource = 20;
    private int maxArticlesPerSource = 2;
    private Scoring scoring = new Scoring();
    private Runner runner = new Runner();

    public int effectiveTopN() {
        return Math.max(3, Math.min(5, topN));
    }

    public int effectiveMaxArticlesPerSource() {
        return Math.max(1, maxArticlesPerSource);
    }

    @Getter
    @Setter
    public static class Scoring {

        private double recencyWeight = 0.30;
        private double keywordWeight = 0.15;
        private double sourceReliabilityWeight = 0.20;
        private double repetitionWeight = 0.35;
    }

    @Getter
    @Setter
    public static class Runner {

        private boolean enabled = true;
    }
}
