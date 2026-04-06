package com.report.dailyreport.notifier;

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

    public void dispatch(TrendReport report) {
        System.out.println(report.markdown());

        if (report.dryRun()) {
            log.info("Dry-run mode is enabled. Notification dispatch was skipped.");
            return;
        }

        notifiers.stream()
                .filter(notifier -> notifier.supports(report.channel()))
                .findFirst()
                .ifPresentOrElse(
                        notifier -> sendSafely(notifier, report),
                        () -> log.warn("No notifier registered for channel={}", report.channel())
                );
    }

    private void sendSafely(Notifier notifier, TrendReport report) {
        try {
            notifier.send(report);
        } catch (Exception exception) {
            log.error("Notification delivery failed: channel={}, message={}",
                    report.channel(), exception.getMessage(), exception);
        }
    }
}
