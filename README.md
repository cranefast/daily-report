# Daily Trend Report Generator

Spring Boot 기반 개인용 최신 트렌드 리포트 자동 생성기입니다. RSS로 AI/개발/부동산/경제 뉴스를 수집하고, 중복 제거와 중요도 점수 계산을 거쳐 상위 이슈를 선별한 뒤 OpenAI `Responses API`로 상세 분석 리포트를 생성합니다.

## 구성

```text
DailyReportRunner
  -> RssCollector
  -> DeduplicationService
  -> ImportanceRanker
  -> OpenAiTrendAnalyzer (실패 시 FallbackTrendAnalyzer)
  -> MarkdownReportFormatter
  -> NotificationDispatcher (Email / Slack)
```

## 실행 방법

1. JDK 21이 설치된 상태에서 루트에서 `./gradlew test`로 빌드를 확인합니다.
2. 기본 dry-run 실행은 `./gradlew bootRun` 입니다.
3. 실제 발송까지 하려면 `REPORT_DRY_RUN=false`와 채널 설정을 함께 넘깁니다.

예시:

```bash
export OPENAI_API_KEY=sk-...
export REPORT_DRY_RUN=true
./gradlew bootRun
```

실제 이메일 발송 예시:

```bash
export OPENAI_API_KEY=sk-...
export REPORT_DRY_RUN=false
export REPORT_CHANNEL=EMAIL
export EMAIL_HOST=smtp.gmail.com
export EMAIL_PORT=587
export EMAIL_USERNAME=your-account@gmail.com
export EMAIL_PASSWORD=app-password
export EMAIL_TO=receiver@example.com
./gradlew bootRun
```

## 환경설정

주요 설정은 `src/main/resources/application.yml` 에 있고, 운영에서는 환경변수로 덮어쓰는 방식을 권장합니다.

- `OPENAI_API_KEY`: OpenAI API 키
- `OPENAI_MODEL`: 기본값 `gpt-5.4`
- `REPORT_TOP_N`: 상위 기사 수, 내부적으로 3~5 범위로 보정
- `REPORT_DRY_RUN`: `true`면 콘솔 출력만 수행
- `REPORT_CHANNEL`: `EMAIL` 또는 `SLACK`
- `EMAIL_*`: SMTP 및 수신자 설정
- `SLACK_WEBHOOK_URL`: Slack Incoming Webhook URL

## GitHub Actions 설정

워크플로 파일은 `.github/workflows/daily-report.yml` 입니다.

- `workflow_dispatch`: 수동 실행, `dry_run` 입력 가능
- `schedule`: `0 23 * * *` (UTC), 한국 시간 오전 8시
- 실행 순서: checkout -> JDK 21 -> `./gradlew test` -> `./gradlew bootRun`

필수 Secrets:

- `OPENAI_API_KEY`
- `EMAIL_HOST`
- `EMAIL_PORT`
- `EMAIL_USERNAME`
- `EMAIL_PASSWORD`
- `EMAIL_TO`
- `EMAIL_FROM` (선택)
- `SLACK_WEBHOOK_URL` (Slack 사용 시)

선택 Variables:

- `OPENAI_MODEL`
- `REPORT_CHANNEL`

## Email 설정

기본 전송 채널은 Email입니다. 본 프로젝트는 `spring-boot-starter-mail`을 사용하므로 SMTP 계정을 준비해야 합니다.

- Gmail 예시는 `EMAIL_HOST=smtp.gmail.com`, `EMAIL_PORT=587`
- Gmail을 쓸 경우 일반 비밀번호 대신 앱 비밀번호를 권장합니다.
- dry-run 모드에서는 메일 전송 없이 콘솔에 Markdown 리포트만 출력합니다.

## Slack 확장 방법

이미 `SlackNotifier`가 포함돼 있으므로 아래만 설정하면 됩니다.

```bash
export REPORT_DRY_RUN=false
export REPORT_CHANNEL=SLACK
export SLACK_WEBHOOK_URL=https://hooks.slack.com/services/...
./gradlew bootRun
```

향후 Teams, Discord 같은 채널도 `Notifier` 인터페이스를 구현해 쉽게 추가할 수 있습니다.

## 테스트 범위

- `DeduplicationServiceTest`: URL/제목 유사도 기반 중복 제거
- `ImportanceRankerTest`: 최신성/키워드/반복 언급 기반 중요도 계산
- `MarkdownReportFormatterTest`: 리포트 포맷 섹션 구성
- `DailyReportApplicationTests`: 스프링 컨텍스트 로딩

## 참고

- OpenAI 분석은 공식 `Responses API` 호출 기반입니다.
- `OPENAI_API_KEY`가 없거나 API 호출이 실패하면 규칙 기반 fallback 분석으로 계속 진행합니다.
- RSS 일부 실패, OpenAI 실패, 전송 실패는 전체 프로세스를 중단하지 않고 로그로 남깁니다.
