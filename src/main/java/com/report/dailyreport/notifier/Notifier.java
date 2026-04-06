package com.report.dailyreport.notifier;

import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.TrendReport;

public interface Notifier {

    NotificationChannel channel();

    void send(TrendReport report) throws Exception;

    default boolean supports(NotificationChannel channel) {
        return channel() == channel;
    }
}
