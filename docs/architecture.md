# SEO 올인원 대시보드 - 시스템 아키텍처

## 1. 전체 시스템 아키텍처 다이어그램

```
                                    [Client]
                                       |
                                       | HTTPS
                                       v
                              +------------------+
                              |   Nginx Reverse  |
                              |      Proxy       |
                              +--------+---------+
                                       |
                          +------------+------------+
                          |                         |
                          v                         v
                  +-------+--------+      +---------+-------+
                  |   React SPA    |      |    seo-api      |
                  |  (별도 레포)    |      | (Spring Boot)   |
                  +----------------+      +---+----+----+---+
                                              |    |    |
                       +----------------------+    |    +------------------+
                       |                           |                      |
                       v                           v                      v
              +--------+--------+      +-----------+---------+   +--------+--------+
              |   seo-crawler   |      |    seo-scheduler    |   |     seo-ai      |
              | (크롤링 엔진)    |      | (Quartz + Batch)   |   | (AI 분석 엔진)   |
              +--------+--------+      +-----------+---------+   +--------+--------+
                       |                           |                      |
                       +----------+    +-----------+                      |
                                  |    |                                  |
                                  v    v                                  |
                        +---------+----+--------+                         |
                        |       Kafka           |<------------------------+
                        | (메시지 큐 / 이벤트)   |
                        +---------+-------------+
                                  |
            +----------+----------+----------+----------+
            |          |          |          |          |
            v          v          v          v          v
     +------+--+ +----+----+ +---+----+ +---+----+ +---+------+
     |PostgreSQL| |Timescale| | Redis  | | MinIO  | | OpenAI / |
     |  (메인)  | |   DB    | | (캐시) | |(파일)  | | Claude   |
     +---------+ +---------+ +--------+ +--------+ +----------+

     +----------------------------------------------------------+
     |              Prometheus + Grafana (모니터링)                |
     +----------------------------------------------------------+
```

## 2. 멀티모듈 Gradle 프로젝트 구성

### 2.1 모듈 개요

```
seo-dashboard/ (루트 프로젝트)
├── seo-common/          # 공통 도메인, DTO, 유틸리티, 예외 처리
├── seo-api/             # REST API, 인증/인가, WebSocket
├── seo-crawler/         # 크롤링 엔진, 페이지 분석
├── seo-scheduler/       # 스케줄링, 배치 작업 관리
├── seo-ai/              # AI 콘텐츠 분석, 메타 생성
├── docs/                # 설계 문서
├── docker/              # Docker 구성 파일
├── build.gradle.kts     # 루트 빌드 스크립트
└── settings.gradle.kts  # 멀티모듈 설정
```

### 2.2 모듈별 상세 구조

#### seo-common (공통 모듈)
```
seo-common/
├── src/main/java/com/seodashboard/common/
│   ├── domain/                # 공통 엔티티 (BaseEntity, Auditable)
│   │   ├── BaseEntity.java
│   │   ├── Site.java
│   │   ├── User.java
│   │   └── enums/             # Status, Priority 등 열거형
│   ├── dto/                   # 공통 DTO
│   │   ├── ApiResponse.java
│   │   ├── PageResponse.java
│   │   └── ErrorResponse.java
│   ├── exception/             # 글로벌 예외 정의
│   │   ├── BusinessException.java
│   │   ├── ErrorCode.java
│   │   └── GlobalExceptionHandler.java
│   ├── config/                # 공통 설정
│   │   ├── JpaAuditingConfig.java
│   │   └── JacksonConfig.java
│   └── util/                  # 유틸리티 클래스
│       ├── SeoScoreCalculator.java
│       └── UrlValidator.java
└── build.gradle.kts
```

#### seo-api (API 모듈)
```
seo-api/
├── src/main/java/com/seodashboard/api/
│   ├── SeoApiApplication.java       # 메인 애플리케이션 (단일 배포 시 진입점)
│   ├── config/
│   │   ├── SecurityConfig.java       # Spring Security + JWT
│   │   ├── WebConfig.java            # CORS, 인터셉터
│   │   ├── RedisConfig.java
│   │   ├── KafkaProducerConfig.java
│   │   ├── SwaggerConfig.java
│   │   └── WebSocketConfig.java
│   ├── auth/
│   │   ├── controller/AuthController.java
│   │   ├── service/AuthService.java
│   │   ├── jwt/JwtTokenProvider.java
│   │   └── dto/                      # LoginRequest, TokenResponse 등
│   ├── site/
│   │   ├── controller/SiteController.java
│   │   ├── service/SiteService.java
│   │   └── repository/SiteRepository.java
│   ├── crawl/
│   │   ├── controller/CrawlController.java
│   │   ├── service/CrawlService.java
│   │   └── repository/
│   ├── keyword/
│   │   ├── controller/KeywordController.java
│   │   ├── service/KeywordService.java
│   │   └── repository/
│   ├── analysis/
│   │   ├── controller/AnalysisController.java
│   │   └── service/AnalysisService.java
│   ├── report/
│   │   ├── controller/ReportController.java
│   │   ├── service/ReportService.java
│   │   └── generator/PdfReportGenerator.java
│   ├── dashboard/
│   │   ├── controller/DashboardController.java
│   │   └── service/DashboardService.java
│   └── notification/
│       ├── controller/NotificationController.java
│       ├── service/NotificationService.java
│       └── websocket/NotificationWebSocketHandler.java
└── build.gradle.kts
```

#### seo-crawler (크롤링 엔진 모듈)
```
seo-crawler/
├── src/main/java/com/seodashboard/crawler/
│   ├── config/
│   │   ├── WebClientConfig.java        # 비동기 HTTP 클라이언트 설정
│   │   ├── CrawlerProperties.java      # 크롤러 설정값 (동시 요청 수, 타임아웃 등)
│   │   └── KafkaConsumerConfig.java
│   ├── engine/
│   │   ├── CrawlEngine.java            # 크롤링 오케스트레이터
│   │   ├── PageFetcher.java            # 비동기 페이지 다운로드 (WebClient)
│   │   ├── HtmlParser.java             # Jsoup 기반 HTML 파싱
│   │   ├── LinkExtractor.java          # 내부/외부 링크 추출
│   │   └── RobotsTxtParser.java        # robots.txt 파싱 및 준수
│   ├── analyzer/
│   │   ├── MetaTagAnalyzer.java        # title, description, OG 태그 분석
│   │   ├── ImageAnalyzer.java          # img alt 속성, 이미지 최적화 체크
│   │   ├── LinkHealthChecker.java      # 깨진 링크 검출
│   │   ├── PerformanceAnalyzer.java    # 페이지 로딩 속도 측정
│   │   ├── StructureAnalyzer.java      # heading 구조, 시맨틱 HTML 체크
│   │   └── SitemapAnalyzer.java        # sitemap.xml 유효성 검증
│   ├── event/
│   │   ├── CrawlEventPublisher.java    # Kafka 이벤트 발행
│   │   ├── CrawlEventConsumer.java     # Kafka 이벤트 소비
│   │   └── CrawlEvent.java            # 이벤트 DTO
│   ├── service/
│   │   └── CrawlExecutionService.java  # 크롤링 실행 및 결과 저장
│   └── domain/
│       ├── CrawlJob.java
│       ├── CrawlResult.java
│       └── PageAnalysis.java
└── build.gradle.kts
```

#### seo-scheduler (스케줄링 모듈)
```
seo-scheduler/
├── src/main/java/com/seodashboard/scheduler/
│   ├── config/
│   │   ├── QuartzConfig.java           # Quartz 스케줄러 설정
│   │   └── KafkaConfig.java
│   ├── job/
│   │   ├── KeywordRankingJob.java      # 매일 키워드 순위 수집
│   │   ├── SiteCrawlJob.java           # 주기적 사이트 재크롤링
│   │   ├── ReportGenerationJob.java    # 주간/월간 리포트 자동 생성
│   │   └── AlertCheckJob.java          # 순위 변동 알림 체크
│   ├── service/
│   │   ├── ScheduleManagementService.java  # 스케줄 CRUD
│   │   ├── KeywordRankingService.java      # 구글 검색 순위 수집 로직
│   │   └── RankingDataCollector.java       # 순위 데이터 수집기
│   └── domain/
│       ├── ScheduleConfig.java
│       └── JobExecutionLog.java
└── build.gradle.kts
```

#### seo-ai (AI 분석 모듈)
```
seo-ai/
├── src/main/java/com/seodashboard/ai/
│   ├── config/
│   │   ├── AiClientConfig.java         # OpenAI/Claude API 클라이언트 설정
│   │   ├── AiProperties.java           # API 키, 모델 설정
│   │   └── KafkaConsumerConfig.java
│   ├── service/
│   │   ├── ContentAnalysisService.java  # SEO 콘텐츠 분석
│   │   ├── MetaGenerationService.java   # 메타 타이틀/디스크립션 생성
│   │   ├── KeywordDensityService.java   # 키워드 밀도 분석
│   │   ├── ReadabilityService.java      # 가독성 점수 산출
│   │   └── SeoScoringService.java       # 종합 SEO 점수 계산
│   ├── client/
│   │   ├── AiClient.java               # AI API 추상화 인터페이스
│   │   ├── OpenAiClient.java           # OpenAI 구현체
│   │   └── ClaudeClient.java           # Claude 구현체
│   ├── prompt/
│   │   ├── PromptTemplateManager.java   # 프롬프트 템플릿 관리
│   │   └── templates/                   # 프롬프트 템플릿 파일
│   ├── event/
│   │   └── AiAnalysisEventConsumer.java # Kafka 이벤트 소비
│   └── domain/
│       └── ContentAnalysis.java
└── build.gradle.kts
```

## 3. 모듈 간 의존 관계

### 3.1 의존성 다이어그램

```
                    seo-common
                   /    |    \     \
                  /     |     \     \
                 v      v      v     v
           seo-api  seo-crawler  seo-scheduler  seo-ai
              |         |             |
              |         +------+------+
              |                |
              +--------> Kafka (이벤트 기반 느슨한 결합)
```

### 3.2 의존성 매트릭스

| 모듈 | 의존 대상 | 결합 방식 |
|------|----------|-----------|
| `seo-common` | 없음 (독립) | - |
| `seo-api` | `seo-common` | 컴파일 의존 |
| `seo-crawler` | `seo-common` | 컴파일 의존 |
| `seo-scheduler` | `seo-common` | 컴파일 의존 |
| `seo-ai` | `seo-common` | 컴파일 의존 |
| `seo-api` -> `seo-crawler` | Kafka 이벤트 | 비동기 메시지 |
| `seo-api` -> `seo-ai` | Kafka 이벤트 | 비동기 메시지 |
| `seo-scheduler` -> `seo-crawler` | Kafka 이벤트 | 비동기 메시지 |
| `seo-scheduler` -> `seo-ai` | Kafka 이벤트 | 비동기 메시지 |

### 3.3 Gradle 의존성 설정

```kotlin
// settings.gradle.kts
rootProject.name = "seo-dashboard"
include("seo-common", "seo-api", "seo-crawler", "seo-scheduler", "seo-ai")

// seo-common/build.gradle.kts
// 외부 의존성만. 다른 모듈에 대한 의존 없음.

// seo-api/build.gradle.kts
dependencies {
    implementation(project(":seo-common"))
    // 직접 의존하지 않음: seo-crawler, seo-scheduler, seo-ai
    // Kafka를 통한 느슨한 결합
}

// seo-crawler/build.gradle.kts
dependencies {
    implementation(project(":seo-common"))
}

// seo-scheduler/build.gradle.kts
dependencies {
    implementation(project(":seo-common"))
}

// seo-ai/build.gradle.kts
dependencies {
    implementation(project(":seo-common"))
}
```

### 3.4 설계 원칙

- **모듈 간 직접 의존 금지**: `seo-api`가 `seo-crawler`를 직접 호출하지 않음
- **이벤트 기반 통신**: 모듈 간 통신은 Kafka 이벤트를 통해 수행
- **공통 모듈 최소화**: `seo-common`은 순수 도메인/DTO만 포함, 비즈니스 로직 없음
- **단일 배포 가능**: 포트폴리오 특성상 하나의 Spring Boot 앱으로 통합 배포 가능 (모듈은 논리적 분리)

## 4. 데이터 흐름도

### 4.1 사이트 크롤링 흐름

```
[사용자] --POST /api/crawl/start--> [seo-api]
                                       |
                                  CrawlRequest 생성
                                  crawl_jobs 저장 (status=PENDING)
                                       |
                               Kafka: crawl.request 발행
                                       |
                                       v
                                 [seo-crawler]
                                       |
                          1. robots.txt 확인
                          2. 시작 URL 페치 (WebClient 비동기)
                          3. HTML 파싱 (Jsoup)
                          4. 내부 링크 추출 → BFS 크롤링
                          5. 페이지별 분석 수행:
                             - MetaTagAnalyzer
                             - ImageAnalyzer
                             - LinkHealthChecker
                             - PerformanceAnalyzer
                             - StructureAnalyzer
                          6. 결과 저장 (crawl_results, page_analyses)
                          7. crawl_jobs 상태 업데이트 (COMPLETED)
                                       |
                               Kafka: crawl.completed 발행
                                       |
                           +-----------+-----------+
                           |                       |
                           v                       v
                      [seo-api]               [seo-ai]
                   WebSocket 알림        SEO 점수 종합 산출
                   사용자에게 완료 통보    page_analyses 업데이트
```

### 4.2 키워드 순위 트래킹 흐름

```
[seo-scheduler] --Quartz Cron (매일 06:00)-->
       |
  등록된 키워드 조회
       |
  Kafka: keyword.ranking.collect 발행 (키워드별 메시지)
       |
       v
[seo-scheduler: RankingDataCollector]
       |
  1. 구글 검색 API 호출 (또는 SERP 스크래핑)
  2. 대상 사이트 순위 파싱
  3. keyword_rankings 저장 (TimescaleDB hypertable)
  4. 이전 순위 대비 변동 계산
       |
  순위 급변동 감지?
  ├── YES → Kafka: notification.alert 발행
  │           → [seo-api] → notifications 저장 + WebSocket 푸시
  └── NO  → 완료
```

### 4.3 AI 콘텐츠 분석 흐름

```
[사용자] --POST /api/analysis/content--> [seo-api]
                                            |
                                       요청 유효성 검증
                                       content_analyses 저장 (status=PENDING)
                                            |
                                    Kafka: ai.analysis.request 발행
                                            |
                                            v
                                       [seo-ai]
                                            |
                          1. 텍스트 전처리
                          2. 키워드 밀도 분석 (KeywordDensityService)
                          3. 가독성 점수 산출 (ReadabilityService)
                          4. AI API 호출 (ContentAnalysisService)
                             - SEO 점수 산출
                             - 개선 제안 생성
                          5. 메타 타이틀/디스크립션 생성 (MetaGenerationService)
                          6. content_analyses 업데이트
                                            |
                                    Kafka: ai.analysis.completed 발행
                                            |
                                            v
                                       [seo-api]
                                    WebSocket 알림
                                    캐시 갱신 (Redis)
```

### 4.4 리포트 생성 흐름

```
[seo-scheduler] --Quartz Cron (매주 월요일 09:00)-->
       |                                                [사용자] --POST /api/reports/generate-->
       |                                                            |
       +------------------------------------------------------------+
                                    |
                            Kafka: report.generate 발행
                                    |
                                    v
                              [seo-api: ReportService]
                                    |
                   1. 기간별 데이터 수집
                      - crawl_results 집계
                      - keyword_rankings 추이
                      - content_analyses 요약
                   2. SEO 점수 종합 계산 (SeoScoreCalculator)
                   3. 개선 우선순위 산출
                   4. PDF 생성 (PdfReportGenerator)
                   5. MinIO 업로드
                   6. reports 테이블 저장
                                    |
                            Kafka: report.completed 발행
                                    |
                                    v
                              notifications 저장
                              WebSocket 푸시
```

## 5. 외부 서비스 연동 구조

### 5.1 서비스 연동 맵

```
+-------------------+     +-------------------+     +-------------------+
|   OpenAI API      |     |   Claude API      |     |   Google SERP     |
|   (GPT-4)         |     |   (Claude 3.5)    |     |   (순위 수집)      |
+--------+----------+     +--------+----------+     +--------+----------+
         |                         |                         |
         +------------+------------+                         |
                      |                                      |
              +-------+-------+                    +---------+---------+
              |    seo-ai     |                    |   seo-scheduler   |
              | (Strategy     |                    | (RankingData      |
              |  Pattern으로   |                    |  Collector)       |
              |  교체 가능)    |                    +-------------------+
              +---------------+
```

### 5.2 외부 서비스 상세

| 서비스 | 용도 | 연동 방식 | Fallback 전략 |
|--------|------|-----------|--------------|
| **OpenAI API** | 콘텐츠 분석, 메타 생성 | REST (WebClient) | Claude API로 전환 |
| **Claude API** | 대체 AI 엔진 | REST (WebClient) | OpenAI로 전환 |
| **Google SERP** | 키워드 순위 수집 | HTTP 스크래핑 / API | 캐시된 마지막 순위 반환 |
| **MinIO** | PDF 리포트 저장 | S3 호환 SDK | 로컬 파일시스템 |
| **Redis** | 캐싱, 세션, Rate Limiting | Spring Data Redis | 인메모리 캐시 (Caffeine) |
| **Kafka** | 모듈 간 비동기 통신 | Spring Kafka | 동기 호출 Fallback |
| **PostgreSQL** | 메인 데이터 저장소 | Spring Data JPA | - |
| **TimescaleDB** | 시계열 데이터 (순위 이력) | JPA + Native Query | 일반 PG 테이블 |

### 5.3 AI 클라이언트 추상화 (Strategy Pattern)

```java
// AI 제공자 교체가 투명하게 이루어지도록 Strategy Pattern 적용
public interface AiClient {
    ContentAnalysisResult analyzeContent(String content, AnalysisOptions options);
    MetaTagSuggestion generateMeta(String content, MetaOptions options);
}

// application.yml로 활성 클라이언트 전환
// ai.provider: openai | claude
```

### 5.4 외부 서비스 장애 대응

```
외부 서비스 호출
       |
  Circuit Breaker (Resilience4j)
       |
  ├── 정상 → 응답 반환
  ├── 타임아웃 → Retry (최대 3회, exponential backoff)
  ├── 연속 실패 → Circuit Open
  │     └── Fallback 전략 실행
  └── Half-Open → 테스트 요청 → 복구 확인
```

## 6. 배포 아키텍처 (Docker Compose)

```
docker-compose.yml
├── seo-app          # Spring Boot 통합 앱 (단일 컨테이너)
├── postgres         # PostgreSQL 15 + TimescaleDB 확장
├── redis            # Redis 7
├── kafka            # Apache Kafka (KRaft 모드, ZooKeeper 불필요)
├── minio            # MinIO 오브젝트 스토리지
├── prometheus       # 메트릭 수집
└── grafana          # 대시보드 시각화
```

### 배포 전략 참고

포트폴리오 프로젝트 특성상 **단일 Spring Boot 애플리케이션으로 통합 배포**를 기본으로 한다.
모듈은 논리적 경계를 유지하되, 하나의 JAR로 패키징한다.
향후 트래픽 증가 시 모듈별 독립 서비스로 분리할 수 있는 구조를 유지한다.

```kotlin
// seo-api가 메인 애플리케이션으로서 다른 모듈을 포함
// seo-api/build.gradle.kts
dependencies {
    implementation(project(":seo-common"))
    implementation(project(":seo-crawler"))
    implementation(project(":seo-scheduler"))
    implementation(project(":seo-ai"))
}
```

## 7. 보안 계층

### 7.1 SSRF 방어 (UrlValidator)

공개 SEO 분석 엔드포인트(`/public/analyze`)에서 사용자가 입력한 URL을 서버 측에서 fetch하기 때문에 SSRF(Server-Side Request Forgery) 공격에 노출될 수 있다. 이를 방어하기 위해 `UrlValidator.validateForFetch()` 메서드가 다음 항목을 차단한다:

- **localhost / 0.0.0.0 / .local** 호스트 차단
- **사설 IP 대역** 차단 (10.x, 172.16~31.x, 192.168.x)
- **루프백 주소** (127.0.0.1) 차단
- **링크-로컬 주소** (169.254.x.x) 차단
- **클라우드 메타데이터 엔드포인트** (169.254.169.254) 차단 (AWS/GCP/Azure)
- HTTP/HTTPS 스킴만 허용 (file://, ftp:// 등 차단)

```
[사용자 URL 입력] → UrlValidator.validateForFetch()
    ├── 스킴 검증 (HTTP/HTTPS만)
    ├── 호스트 검증 (localhost, .local 차단)
    ├── DNS 조회 후 IP 검증
    │   ├── isLoopbackAddress() → 차단
    │   ├── isSiteLocalAddress() → 차단 (사설 IP)
    │   ├── isLinkLocalAddress() → 차단
    │   └── isCloudMetadataRange() → 차단 (169.254.169.254)
    └── 통과 시 → PageFetcher.fetch() 실행
```

### 7.2 Rate Limiting (RateLimitInterceptor)

IP 기반 슬라이딩 윈도우 카운터 방식의 Rate Limiting을 `RateLimitInterceptor`로 구현한다. `WebConfig`에서 엔드포인트별로 인스턴스를 등록하며, 클라이언트 IP는 `X-Forwarded-For` → `X-Real-IP` → `RemoteAddr` 순서로 추출한다.

| 엔드포인트 | 제한 | 윈도우 |
|-----------|------|--------|
| `/public/analyze` | 5회 | 1분 |
| `/auth/login` | 10회 | 1분 |
| `/auth/signup` | 3회 | 1분 |

제한 초과 시 HTTP 429 응답과 함께 `Retry-After` 헤더를 반환한다.

### 7.3 JWT 시크릿 검증

`JwtTokenProvider` 생성 시 다음 보안 검증을 수행한다:

- **비-로컬 프로파일**: `JWT_SECRET` 환경변수 설정 필수 (기본값 사용 차단)
- **키 길이 검증**: Base64 디코딩 후 최소 256비트(32바이트) 이상 필수
- **Refresh Token Rotation**: 토큰 갱신 시 이전 토큰 즉시 무효화, 재사용 시도 감지(replay detection) 시 해당 사용자의 모든 세션 무효화

## 8. 캐싱 계층

Redis를 사용한 다단계 캐싱으로 반복 조회 성능을 최적화한다. `RedisConfig`에서 캐시별 TTL을 설정한다.

### 8.1 캐시 구성

| 캐시 이름 | TTL | 용도 | 비고 |
|-----------|-----|------|------|
| `dashboard` | 30초 | 종합 대시보드 조회 | 짧은 TTL로 실시간성 유지 |
| `publicAnalysis` | 5분 | 공개 SEO 분석 결과 | COMPLETED 상태만 캐시 |
| `publicRanking` | 2분 | 공개 SEO 점수 랭킹 | 새 분석 완료 시 evict |
| 기본 | 1분 | 그 외 캐시 항목 | 기본 TTL |

### 8.2 캐시 무효화 전략

```
[공개 분석 완료]
    ├── @CacheEvict("publicRanking", allEntries=true)  ← 랭킹 목록 갱신
    └── @Cacheable("publicAnalysis", key="#id")         ← 개별 결과 캐시

[대시보드 조회]
    └── @Cacheable("dashboard")                         ← 30초 TTL 자동 만료
```

- `publicRanking`: 새로운 공개 분석이 완료될 때마다 전체 evict (순위 변동 반영)
- `publicAnalysis`: 분석 ID 기준 개별 캐시, COMPLETED 상태가 아니면 캐시하지 않음
- Fallback: Redis 장애 시 Caffeine 인메모리 캐시로 전환 가능

## 9. 병렬 크롤링

### 9.1 Virtual Threads + Semaphore 기반 BFS 크롤링

`CrawlEngine`은 Java Virtual Threads와 Semaphore를 조합하여 BFS(너비 우선 탐색) 레벨별 병렬 페이지 fetch를 수행한다.

```
[BFS 레벨별 병렬 크롤링]

레벨 0: [시작 URL]
         │ fetch (1개)
         v
레벨 1: [링크A] [링크B] [링크C] [링크D] [링크E]
         │ Virtual Threads + Semaphore(동시 N개)
         │ CompletableFuture 병렬 fetch
         v
레벨 2: [링크F] [링크G] ... [링크N]
         │ 동일 방식 병렬 fetch
         v
    (maxDepth 또는 maxPages 도달 시 종료)
```

핵심 설계:
- **`Executors.newVirtualThreadPerTaskExecutor()`**: URL별 Virtual Thread 할당, OS 스레드 고갈 없이 대량 동시 요청 가능
- **`Semaphore(concurrency)`**: 대상 서버 부하 제어를 위한 동시 요청 수 제한 (`CrawlerProperties.maxConcurrentRequests`)
- **BFS 레벨 단위 실행**: 같은 depth의 URL들을 한꺼번에 병렬 fetch한 후, 발견된 링크를 다음 레벨로 전달
- **취소 지원**: `AtomicBoolean cancelFlag`로 레벨 간, 태스크 간 취소 확인
- **Polite Crawling**: Semaphore 해제 전 `delayBetweenRequestsMs`만큼 대기하여 대상 서버에 예의 유지

## 10. 트랜잭션 관리

### 10.1 CrawlBatchPersister - 배치 트랜잭션 분리

크롤링은 수분~수십 분 소요될 수 있으므로, 전체 크롤링을 하나의 트랜잭션으로 감싸면 DB 커넥션 고갈과 장시간 락 문제가 발생한다. 이를 방지하기 위해 `CrawlBatchPersister`가 독립 트랜잭션(`Propagation.REQUIRES_NEW`)으로 10건 단위 배치 저장을 수행한다.

```
[CrawlExecutionService.executeCrawl()] ← 트랜잭션 없음 (비트랜잭셔널)
    │
    ├── batchPersister.markRunning()         ← TX #1 (REQUIRES_NEW)
    │
    ├── for (10건 단위 배치):
    │     └── batchPersister.saveBatch()     ← TX #2, #3, #4, ... (각각 REQUIRES_NEW)
    │           ├── CrawlResult persist
    │           ├── PageAnalysis persist
    │           ├── flush()
    │           └── clear()                  ← 영속성 컨텍스트 초기화 (메모리 관리)
    │
    ├── batchPersister.markCompleted()       ← TX #N (REQUIRES_NEW)
    │
    └── batchPersister.updateSiteScore()     ← TX #N+1 (REQUIRES_NEW)
```

이점:
- **DB 커넥션 효율**: 각 배치 트랜잭션이 빠르게 커밋/반환되어 커넥션 풀을 점유하지 않음
- **메모리 관리**: `flush()` + `clear()`로 영속성 컨텍스트를 배치마다 초기화
- **부분 실패 복구**: 크롤링 중 오류 발생 시 이미 저장된 배치는 보존됨
- **상태 추적**: `markRunning()`, `markCompleted()`, `markFailed()` 각각 독립 트랜잭션

## 11. 이벤트 기반 처리

### 11.1 @TransactionalEventListener(AFTER_COMMIT) + @Async 패턴

크롤링 시작과 같은 비동기 작업은 이벤트 기반으로 트리거된다. `@TransactionalEventListener(phase = AFTER_COMMIT)`와 `@Async`를 조합하여, CrawlJob 생성 트랜잭션이 커밋된 후에만 실제 크롤링이 시작되도록 보장한다.

```
[CrawlService.startCrawl()]
    │
    ├── CrawlJob 생성 + 저장 (INSERT)
    ├── ApplicationEventPublisher.publishEvent(CrawlStartedEvent)
    ├── 트랜잭션 커밋                    ← DB에 CrawlJob이 확실히 존재
    │
    └── @TransactionalEventListener(AFTER_COMMIT)
        └── @Async("crawlExecutor")
            └── CrawlEventListener.handleCrawlStarted()
                └── CrawlExecutionService.executeCrawl()
```

이 패턴이 필요한 이유:
- **데이터 가시성**: `@Async`만 사용하면 비동기 스레드가 아직 커밋되지 않은 CrawlJob을 조회하여 실패할 수 있음
- **AFTER_COMMIT 보장**: 이벤트 리스너가 트랜잭션 커밋 완료 후에만 호출되므로, 크롤링 시작 시 CrawlJob 데이터가 DB에 확실히 존재
- **스레드 분리**: `@Async("crawlExecutor")`로 크롤링이 별도 스레드 풀에서 실행되어 HTTP 요청 스레드를 차단하지 않음
- **안전 마진**: 500ms 지연을 추가하여 트랜잭션 전파 완료를 보장

## 12. 확장성 고려사항

### 12.1 수평 확장 포인트

| 컴포넌트 | 현재 | 10x 확장 시 |
|----------|------|-------------|
| API 서버 | 단일 인스턴스 | 로드밸런서 + 다중 인스턴스 |
| 크롤러 | 앱 내 실행 | Kafka Consumer Group 기반 분산 |
| 스케줄러 | Quartz 단일 | Quartz 클러스터링 (DB 기반) |
| AI 분석 | 동기 처리 | Kafka Consumer 병렬 처리 |
| DB | 단일 PG | Read Replica + Connection Pool |
| 캐시 | 단일 Redis | Redis Cluster |

### 12.2 성능 병목 예측 및 대응

| 병목 지점 | 증상 | 대응 |
|-----------|------|------|
| 크롤링 대기열 적체 | 크롤링 요청 처리 지연 | Kafka 파티션 증설 + Consumer 수 증가 |
| DB 쓰기 부하 | keyword_rankings 삽입 지연 | TimescaleDB 청크 최적화 + 배치 INSERT |
| AI API Rate Limit | 콘텐츠 분석 대기열 증가 | 요청 큐잉 + Rate Limiter + 멀티 프로바이더 |
| Redis 메모리 | 캐시 eviction 빈번 | TTL 최적화 + 메모리 증설 |

### 12.3 마이크로서비스 전환 경로

현재 모놀리식 멀티모듈 구조에서, 필요 시 다음 순서로 분리:

1. **seo-crawler 분리** (가장 리소스 집약적, 독립 스케일링 이점 큼)
2. **seo-ai 분리** (외부 API 의존, 독립 Rate Limiting 필요)
3. **seo-scheduler 분리** (Quartz 클러스터링 독립 운영)
4. **seo-api 유지** (Gateway 역할로 전환)

Kafka 기반 이벤트 통신이 이미 구현되어 있으므로, 분리 시 코드 변경 최소화.
