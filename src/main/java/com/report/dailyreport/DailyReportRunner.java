package com.report.dailyreport;

import com.report.dailyreport.application.TrendReportGenerationService;
import com.report.dailyreport.notifier.NotificationDispatcher;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "report.runner", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DailyReportRunner implements ApplicationRunner {

    private final TrendReportGenerationService trendReportGenerationService;
    private final NotificationDispatcher notificationDispatcher;

    @Override
    public void run(ApplicationArguments args) {
        trendReportGenerationService.generate()
                .ifPresent(notificationDispatcher::dispatch);
    }
}
