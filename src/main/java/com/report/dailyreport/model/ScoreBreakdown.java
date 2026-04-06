package com.report.dailyreport.model;

public record ScoreBreakdown(
        double totalScore,
        double recencyScore,
        double keywordScore,
        double sourceReliabilityScore,
        double repetitionScore,
        double recencyWeight,
        double keywordWeight,
        double sourceReliabilityWeight,
        double repetitionWeight,
        int repetitionCount
) {
}
