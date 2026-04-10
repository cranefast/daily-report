package com.report.dailyreport.notifier;

import com.report.dailyreport.config.NotificationProperties;
import com.report.dailyreport.config.ReportProperties;
import com.report.dailyreport.formatter.HtmlEmailReportFormatter;
import com.report.dailyreport.model.ArticleHighlight;
import com.report.dailyreport.model.CategoryInsight;
import com.report.dailyreport.model.CategoryTrendSection;
import com.report.dailyreport.model.CollectedArticle;
import com.report.dailyreport.model.NotificationChannel;
import com.report.dailyreport.model.RankedArticle;
import com.report.dailyreport.model.ReportCategory;
import com.report.dailyreport.model.ScoreBreakdown;
import com.report.dailyreport.model.TrendReport;
import jakarta.mail.Multipart;
import jakarta.mail.Part;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.javamail.JavaMailSender;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailNotifierTest {

    @Test
    void sendsHtmlEmailWithPlainTextFallback() throws Exception {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        NotificationProperties notificationProperties = new NotificationProperties();
        notificationProperties.getEmail().setTo("receiver@example.com; second@example.com,\nthird@example.com");
        notificationProperties.getEmail().setFrom("sender@example.com");

        ReportProperties reportProperties = new ReportProperties();
        reportProperties.setTopN(3);
        reportProperties.setZoneId("Asia/Seoul");
        HtmlEmailReportFormatter formatter = new HtmlEmailReportFormatter(reportProperties);
        EmailNotifier notifier = new EmailNotifier(mailSender, notificationProperties, formatter);

        notifier.send(sampleReport());

        ArgumentCaptor<MimeMessage> messageCaptor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(messageCaptor.capture());

        MimeMessage capturedMessage = messageCaptor.getValue();
        capturedMessage.saveChanges();
        List<String> contentTypes = new ArrayList<>();
        List<String> bodies = new ArrayList<>();
        collectParts(capturedMessage, contentTypes, bodies);

        InternetAddress[] recipients = (InternetAddress[]) capturedMessage.getRecipients(MimeMessage.RecipientType.TO);
        assertThat(recipients).extracting(InternetAddress::getAddress)
                .containsExactly("receiver@example.com", "second@example.com", "third@example.com");
        assertThat(contentTypes).anyMatch(type -> type.contains("text/plain"));
        assertThat(contentTypes).anyMatch(type -> type.contains("text/html"));
        assertThat(String.join("\n", bodies)).contains("plain text report");
        assertThat(String.join("\n", bodies)).contains("GitHub ships new Copilot workflow tooling");
    }

    private TrendReport sampleReport() {
        return new TrendReport(
                LocalDate.of(2026, 4, 10),
                Instant.parse("2026-04-10T13:00:00Z"),
                "plain text report",
                List.of(new CategoryTrendSection(
                        ReportCategory.DEVELOPMENT,
                        new CategoryInsight(
                                "개발 분야는 팀 생산성과 플랫폼 전환 이슈가 함께 커지고 있습니다.",
                                "상위 기사는 개발 도구 업그레이드와 워크플로 표준화 흐름을 보여 줍니다.",
                                "개발 조직은 도구 선택뿐 아니라 플랫폼 종속성과 교육 비용도 함께 봐야 합니다.",
                                "도입 전 권한 체계와 릴리스 노트를 먼저 검토하는 것이 좋습니다.",
                                List.of("릴리스 노트 확인", "권한 범위 검토", "CI 영향 점검"),
                                true
                        ),
                        List.of(new ArticleHighlight(
                                new RankedArticle(
                                        new CollectedArticle(
                                                ReportCategory.DEVELOPMENT,
                                                "GitHub Blog",
                                                "https://github.blog/feed/",
                                                "GitHub ships new Copilot workflow tooling",
                                                "https://github.blog/example",
                                                Instant.parse("2026-04-10T08:00:00Z"),
                                                "GitHub announced workflow improvements for development teams.",
                                                0.94,
                                                List.of("developer", "workflow")
                                        ),
                                        new ScoreBreakdown(88.4, 0.92, 0.75, 0.94, 0.60, 0.35, 0.30, 0.20, 0.15, 2)
                                ),
                                "워크플로 자동화가 팀 생산성과 협업 방식에 직접 영향을 줍니다."
                        ))
                )),
                NotificationChannel.EMAIL,
                false
        );
    }

    private void collectParts(Part part, List<String> contentTypes, List<String> bodies) throws Exception {
        contentTypes.add(part.getContentType());
        Object content = part.getContent();
        if (content instanceof String text) {
            bodies.add(text);
            return;
        }
        if (content instanceof Multipart multipart) {
            for (int index = 0; index < multipart.getCount(); index++) {
                collectParts((Part) multipart.getBodyPart(index), contentTypes, bodies);
            }
        }
    }
}
