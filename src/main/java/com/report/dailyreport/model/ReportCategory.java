package com.report.dailyreport.model;

public enum ReportCategory {
    AI("AI", "AI, LLM, 모델 출시, 에이전트, 반도체/클라우드 파급효과를 구체적으로 설명하세요."),
    DEVELOPMENT("개발", "개발자 도구, 프레임워크, 플랫폼 변경, 팀 생산성 영향과 실무 적용 포인트를 설명하세요."),
    REAL_ESTATE("부동산", "이전 추세와 달라진 점, 금리/공급/수요 변화, 영향을 받는 산업군을 설명하세요."),
    ECONOMY("경제", "거시경제 지표, 금리, 물가, 소비/투자 변화와 영향을 받는 산업군을 설명하세요.");

    private final String displayName;
    private final String guidance;

    ReportCategory(String displayName, String guidance) {
        this.displayName = displayName;
        this.guidance = guidance;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getGuidance() {
        return guidance;
    }
}
