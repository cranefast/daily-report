package com.report.dailyreport.notifier;

import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.TrendReport;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationDispatcherTest {

    @Test
    void skipsNotifierInDryRunMode() throws Exception {
        Notifier emailNotifier = mock(Notifier.class);
        when(emailNotifier.supports(NotificationChannel.EMAIL)).thenReturn(true);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(emailNotifier));

        dispatcher.dispatch(report(NotificationChannel.EMAIL, true));

        verify(emailNotifier, never()).send(any());
    }

    @Test
    void dispatchesUsingReportChannel() throws Exception {
        Notifier emailNotifier = mock(Notifier.class);
        Notifier slackNotifier = mock(Notifier.class);
        when(emailNotifier.supports(NotificationChannel.EMAIL)).thenReturn(true);
        when(slackNotifier.supports(NotificationChannel.SLACK)).thenReturn(true);
        NotificationDispatcher dispatcher = new NotificationDispatcher(List.of(emailNotifier, slackNotifier));

        TrendReport report = report(NotificationChannel.SLACK, false);
        dispatcher.dispatch(report);

        verify(slackNotifier).send(report);
        verify(emailNotifier, never()).send(any());
    }

    private TrendReport report(NotificationChannel channel, boolean dryRun) {
        return new TrendReport(
                LocalDate.of(2026, 4, 7),
                Instant.parse("2026-04-07T01:00:00Z"),
                "markdown",
                List.of(),
                channel,
                dryRun
        );
    }
}
