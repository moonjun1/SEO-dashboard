# SEO Dashboard 테스트 전략

## 테스트 현황 요약

| 모듈 | 테스트 파일 | 테스트 수 | 대상 영역 |
|------|------------|----------|----------|
| seo-common | 2 | 40 | URL 검증, URL 유틸리티 |
| seo-crawler | 6 | 55 | SEO 분석기 (메타태그, 헤딩, 이미지, 링크, 성능) |
| seo-api | 4 | 48 | 인증(JWT, AuthService), 레이트리미터, 공개 SEO |
| **합계** | **12** | **143** | |

## 테스트 구조

```
seo-common/src/test/java/com/seodashboard/common/util/
  UrlValidatorTest.java          -- 25개 테스트
  UrlUtilsTest.java              -- 15개 테스트

seo-crawler/src/test/java/com/seodashboard/crawler/analyzer/
  MetaTagAnalyzerTest.java       -- SEO 메타태그 분석
  HeadingAnalyzerTest.java       -- 헤딩 구조 분석
  ImageAnalyzerTest.java         -- 이미지 alt 텍스트 등
  LinkAnalyzerTest.java          -- 내부/외부 링크 분석
  PerformanceAnalyzerTest.java   -- 성능 지표 분석
  SeoAnalyzerTest.java           -- 통합 SEO 분석
                                    (6개 파일 합계 55개 테스트)

seo-api/src/test/java/com/seodashboard/api/
  auth/jwt/JwtTokenProviderTest.java       -- 16개 테스트
  auth/service/AuthServiceTest.java        -- 23개 테스트 (회원가입, 로그인, 토큰 갱신 등)
  config/RateLimitInterceptorTest.java     -- 13개 테스트 (IP 기반, 유저 기반 제한)
  publicseo/service/PublicSeoServiceTest.java -- 12개 테스트 (비인증 SEO 분석)
```

## 테스트하는 것과 그 이유

### 보안 로직 (77개 테스트)

**원칙: 보안 로직은 반드시 테스트한다.**

| 대상 | 테스트 수 | 이유 |
|------|----------|------|
| UrlValidator | 25 | 악의적 URL 차단은 SSRF 방어의 핵심. 프라이빗 IP, 잘못된 스킴, 허용 범위 초과 등 경계 조건을 빠짐없이 검증 |
| RateLimitInterceptor | 13 | API 남용 방지. IP 기반/유저 기반 제한, 한도 초과 시 동작, 리셋 타이밍 검증 |
| JwtTokenProvider | 16 | 토큰 생성, 검증, 만료, 변조 감지. 인증 체계의 정확성이 전체 시스템 보안을 좌우 |
| AuthService | 23 | 회원가입 중복 검사, 로그인 자격 증명, 토큰 갱신 플로우. 인증 비즈니스 로직의 정합성 보장 |

### 핵심 비즈니스 로직 (55개 테스트)

**원칙: 핵심 가치 = SEO 점수 정확성.**

| 대상 | 이유 |
|------|------|
| MetaTagAnalyzer | title, description, OG 태그 분석은 SEO 점수의 기본. 누락/과다 감지 정확도가 제품 신뢰도를 결정 |
| HeadingAnalyzer | h1~h6 구조 분석. 다중 h1, 순서 건너뜀 등 구조적 문제를 정확히 감지해야 함 |
| ImageAnalyzer | alt 텍스트 누락, 이미지 크기 최적화 등. 접근성과 SEO 점수 모두에 영향 |
| LinkAnalyzer | 내부/외부 링크 비율, 깨진 링크 감지. 사이트 구조 분석의 핵심 |
| PerformanceAnalyzer | 페이지 크기, 리소스 수 등 성능 지표. 정량적 분석이므로 계산 정확도 필수 |
| SeoAnalyzer | 개별 분석기 통합. 최종 점수 계산과 종합 평가의 정확성 검증 |

### 공유 유틸리티 (27개 테스트)

**원칙: 공유 유틸은 regression 방지가 필수.**

| 대상 | 테스트 수 | 이유 |
|------|----------|------|
| UrlUtils | 15 | 여러 모듈에서 사용하는 URL 정규화, 도메인 추출 등. 변경 시 영향 범위가 넓어 회귀 테스트 필수 |
| PublicSeoService | 12 | 비인증 사용자 대상 공개 SEO 분석. 입력 검증부터 결과 반환까지 전체 플로우 검증 |

## 테스트하지 않는 것과 그 이유

### SiteService, ReportService

CRUD 위주의 서비스 레이어입니다. Repository를 mock하고 `save()` 호출을 verify하는 테스트는 구현을 그대로 복사하는 것에 불과합니다. mock-verify 안티패턴에 해당하며, 리팩토링 시 테스트가 깨지면서 실제 버그는 잡지 못합니다. 통합 테스트로 검증하는 것이 더 효과적입니다.

### CrawlBatchPersister

EntityManager와 배치 쿼리를 mock하는 것은 실제 데이터베이스 동작을 전혀 반영하지 못합니다. 플러시 타이밍, 배치 크기, 트랜잭션 경계 등은 실제 DB와의 통합 테스트에서만 의미 있게 검증됩니다.

### CrawlEngine 동시성 로직

동시성 테스트는 본질적으로 비결정적(non-deterministic)입니다. Thread.sleep 기반 테스트는 flaky하고, CI 환경의 CPU 코어 수와 스케줄링에 따라 결과가 달라집니다. `max-concurrent-requests` 같은 설정은 부하 테스트로 검증하는 것이 적합합니다.

### 프론트엔드

현재 MVP 단계이며, UI 변경이 빈번합니다. 컴포넌트 단위 테스트보다는 안정화 이후 E2E 테스트를 도입하는 것이 비용 대비 효과가 높습니다. 프론트엔드 기술 스택(React 19, Vite 8)이 아직 초기이므로 테스트 인프라 투자 시점을 늦추는 전략입니다.

## 테스트 실행 방법

### 전체 테스트

```bash
./gradlew test
```

### 모듈별 테스트

```bash
./gradlew :seo-common:test
./gradlew :seo-crawler:test
./gradlew :seo-api:test
```

### 특정 테스트 클래스 실행

```bash
./gradlew :seo-api:test --tests "com.seodashboard.api.auth.jwt.JwtTokenProviderTest"
```

모든 테스트는 외부 의존성(DB, Redis) 없이 실행됩니다. Docker 컨테이너를 띄우지 않아도 테스트를 실행할 수 있습니다.

## 테스트 작성 규칙

### 네이밍 컨벤션

한글 `@DisplayName`으로 테스트 의도를 명확히 표현합니다.

```java
@Nested
@DisplayName("URL 유효성 검증")
class ValidateUrl {

    @Test
    @DisplayName("프라이빗 IP 주소는 거부한다")
    void rejectsPrivateIpAddress() {
        // given, when, then
    }
}
```

### 구조 원칙

- **@Nested**: 관련 테스트를 논리적으로 그룹화
- **@DisplayName**: 한글로 테스트 의도 서술. 메서드명 대신 DisplayName을 읽었을 때 무엇을 검증하는지 이해할 수 있어야 함
- **Given-When-Then**: 준비-실행-검증 구조를 일관되게 적용
- **단일 검증**: 한 테스트에서 하나의 동작만 검증. 여러 단언(assertion)이 필요하면 `assertAll` 사용

### 새 테스트 추가 기준

아래 조건에 해당하면 테스트를 작성합니다:

1. **보안 관련 로직**: 인증, 인가, 입력 검증, 외부 입력 처리
2. **점수 계산 로직**: SEO 분석 점수, 가중치, 등급 산정
3. **공유 유틸리티**: 2개 이상 모듈에서 사용하는 코드
4. **버그 수정**: 재발 방지를 위한 회귀 테스트

아래 조건에서는 테스트를 작성하지 않습니다:

1. 단순 CRUD (Repository mock-verify)
2. 프레임워크 기능 재검증 (Spring Security 필터 체인 등)
3. 비결정적 동시성 테스트
4. 빈번히 변경되는 UI 컴포넌트
