package com.report.dailyreport.config;

import com.report.dailyreport.model.NotificationChannel;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "report")
public class ReportProperties {

    private int topN = 4;
    private boolean dryRun = true;
    private NotificationChannel channel = NotificationChannel.EMAIL;
    private String zoneId = "Asia/Seoul";
    private double duplicateTitleSimilarityThreshold = 0.82;
    private long lookbackHours = 72;
    private int maxEntriesPerSource = 10;
    private Scoring scoring = new Scoring();
    private Runner runner = new Runner();

    public int effectiveTopN() {
        return Math.max(3, Math.min(5, topN));
    }

    @Getter
    @Setter
    public static class Scoring {

        private double recencyWeight = 0.35;
        private double keywordWeight = 0.30;
        private double sourceReliabilityWeight = 0.20;
        private double repetitionWeight = 0.15;
    }

    @Getter
    @Setter
    public static class Runner {

        private boolean enabled = true;
    }
}
