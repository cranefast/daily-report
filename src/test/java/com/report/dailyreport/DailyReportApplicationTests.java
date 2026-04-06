package com.report.dailyreport;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "report.runner.enabled=false")
class DailyReportApplicationTests {

    @Test
    void contextLoads() {
    }

}
