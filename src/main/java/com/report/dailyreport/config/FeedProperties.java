package com.report.dailyreport.config;

import com.report.dailyreport.model.ReportCategory;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "feeds")
public class FeedProperties {

    private Map<ReportCategory, List<FeedSourceProperties>> categories = new EnumMap<>(ReportCategory.class);

    @Getter
    @Setter
    public static class FeedSourceProperties {

        private String name;
        private String url;
        private double reliability = 0.7;
        private List<String> keywords = new ArrayList<>();
    }
}
