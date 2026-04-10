package com.report.dailyreport.model;

import lombok.Getter;

@Getter
public enum ReportCategory {
    AI("AI", "AI, LLM, 모델 출시, 에이전트, 반도체/클라우드 파급효과를 구체적으로 설명하세요."),
    DEVELOPMENT("개발", "개발자 도구, 프레임워크, 플랫폼 변경, 팀 생산성 영향과 실무 적용 포인트를 설명하세요."),
    KOREA_REAL_ESTATE("부동산(한국)", "한국 부동산 시장을 기준으로 정책, 금리, 공급, 전세/매매 흐름과 영향을 받는 산업군을 설명하세요."),
    KOREA_ECONOMY("경제(한국)", "한국 기준 거시경제 지표, 기준금리, 물가, 수출, 성장률 변화와 영향을 받는 산업군을 설명하세요."),
    GLOBAL_ECONOMY("경제(글로벌)", "미국과 글로벌 기준 거시경제 지표, 금리, 물가, 소비/투자 변화와 영향을 받는 산업군을 설명하세요.");

    private final String displayName;
    private final String guidance;

    ReportCategory(String displayName, String guidance) {
        this.displayName = displayName;
        this.guidance = guidance;
    }

}
