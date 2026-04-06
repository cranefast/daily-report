package com.report.dailyreport.notifier;

import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.model.TrendReport;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationDispatcher {

    private final List<Notifier> notifiers;
    private final ReportProperties reportProperties;

    public void dispatch(TrendReport report) {
        System.out.println(report.markdown());

        if (reportProperties.isDryRun()) {
            log.info("Dry-run mode is enabled. Notification dispatch was skipped.");
            return;
        }

        notifiers.stream()
                .filter(notifier -> notifier.supports(reportProperties.getChannel()))
                .findFirst()
                .ifPresentOrElse(
                        notifier -> sendSafely(notifier, report),
                        () -> log.warn("No notifier registered for channel={}", reportProperties.getChannel())
                );
    }

    private void sendSafely(Notifier notifier, TrendReport report) {
        try {
            notifier.send(report);
        } catch (Exception exception) {
            log.error("Notification delivery failed: channel={}, message={}",
                    reportProperties.getChannel(), exception.getMessage(), exception);
        }
    }
}
