package com.report.dailyreport.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RuntimeConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemUTC();
    }
}
