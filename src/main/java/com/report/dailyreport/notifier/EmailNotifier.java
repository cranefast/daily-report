package com.report.dailyreport.notifier;

import com.report.dailyreport.config.NotificationProperties;
import com.report.dailyreport.formatter.HtmlEmailReportFormatter;
import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.TrendReport;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;
    private final NotificationProperties notificationProperties;
    private final HtmlEmailReportFormatter htmlEmailReportFormatter;

    @Override
    public NotificationChannel channel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(TrendReport report) throws Exception {
        if (!StringUtils.hasText(notificationProperties.getEmail().getTo())) {
            throw new IllegalStateException("notification.email.to is not configured");
        }

        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
        helper.setTo(notificationProperties.getEmail().getTo().split("\\s*,\\s*"));
        if (StringUtils.hasText(notificationProperties.getEmail().getFrom())) {
            helper.setFrom(notificationProperties.getEmail().getFrom());
        }
        helper.setSubject(notificationProperties.getEmail().getSubjectPrefix() + " " + report.reportDate());
        helper.setText(report.markdown(), htmlEmailReportFormatter.format(report));
        mailSender.send(mimeMessage);
        log.info("Daily report email sent to {}", notificationProperties.getEmail().getTo());
    }
}
