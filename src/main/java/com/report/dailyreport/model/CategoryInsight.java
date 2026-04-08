package com.report.dailyreport.model;

import java.util.List;

public record CategoryInsight(
        String summary,
        String detailedReport,
        String industryImpact,
        String practicalImplications,
        List<String> followUpPoints,
        boolean generatedByAi
) {
    public CategoryInsight {
        followUpPoints = followUpPoints == null ? List.of() : List.copyOf(followUpPoints);
    }
}
