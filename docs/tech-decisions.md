# 기술 스택 및 설계 결정

본 문서는 SEO Dashboard 프로젝트의 주요 기술 선정 근거와 아키텍처 패턴을 정리합니다.

---

## 1. 백엔드 언어 및 프레임워크

### Java 21 + Spring Boot 3.3

**선정 사유**

| 항목 | 설명 |
|------|------|
| Virtual Threads | 크롤링 작업에서 I/O 대기 시간이 길어 경량 스레드가 필수. Virtual Threads를 활용하여 Semaphore 기반 병렬 크롤링을 구현하였고, 기존 순차 처리 대비 3~5배 성능 향상 달성 |
| Record 패턴 | DTO 클래스를 Java Record로 선언하여 보일러플레이트 코드 제거. 불변성 보장으로 스레드 안전성 확보 |
| LTS 지원 | Java 21은 2023년 9월 출시된 장기 지원(LTS) 버전으로, 프로덕션 안정성과 최신 기능을 동시에 확보 |
| Spring Boot 3.3 | Spring Framework 6 기반으로 Jakarta EE 전환 완료. GraalVM Native Image 지원, 향후 성능 최적화 가능 |

**고려했으나 선택하지 않은 대안**

- Kotlin: 팀 내 Java 숙련도가 높아 전환 비용 대비 이점 부족
- Node.js: CPU 집약적 SEO 분석 작업에 부적합, 타입 안전성 부족

---

## 2. 멀티모듈 Gradle 구조

### 모듈 구성

```
seo-dashboard/
├── seo-common/       # 공통 엔티티, DTO, 예외처리
├── seo-api/          # REST API, 인증, 컨트롤러
├── seo-crawler/      # 크롤링 엔진, SEO 분석기
├── seo-scheduler/    # 키워드 순위 수집
└── seo-ai/           # AI 콘텐츠 분석
```

**선정 사유**

| 항목 | 설명 |
|------|------|
| 관심사 분리 | 크롤링, AI 분석, 스케줄링은 각각 독립된 도메인. 모듈 경계를 명확히 하여 코드 응집도 향상 |
| 독립 테스트 | 각 모듈을 독립적으로 테스트 가능. 크롤러 모듈 테스트 시 API 모듈 의존 불필요 |
| 점진적 분리 | 현재는 단일 JAR로 통합 배포하되, 트래픽 증가 시 모듈 단위로 마이크로서비스 전환 가능 |
| 빌드 최적화 | 변경된 모듈만 재빌드하여 개발 주기 단축 |

**모듈 간 의존 관계 원칙**

- `seo-common`은 다른 모듈에 의존하지 않음 (순수 도메인/DTO)
- 나머지 모듈은 `seo-common`만 의존
- 모듈 간 통신은 Spring Event + `@Async`로 비동기 처리 (향후 Kafka 전환 대비)

---

## 3. 프론트엔드

### React + Vite

**선정 사유**

| 항목 | 설명 |
|------|------|
| SPA 적합성 | 대시보드 특성상 SSR(서버 사이드 렌더링)이 불필요. SEO가 필요한 공개 페이지도 API 기반 데이터 렌더링으로 충분 |
| Vite HMR | 개발 중 파일 저장 시 즉시 반영되는 Hot Module Replacement로 프론트엔드 개발 생산성 향상 |
| 빌드 속도 | Vite의 ESBuild 기반 빌드가 Webpack 대비 10~100배 빠름 |
| 경량 설정 | Next.js 대비 설정이 단순하고, 불필요한 SSR/ISR 오버헤드 없음 |

**Next.js를 선택하지 않은 이유**

이 프로젝트는 인증된 사용자가 사용하는 관리 대시보드가 주 기능입니다. 공개 SEO 진단 페이지는 API 응답을 동적으로 렌더링하므로 SSR의 SEO 이점이 크지 않습니다. Next.js의 App Router, Server Components 등 추가 복잡성 대비 이점이 부족하다고 판단하였습니다.

### TypeScript 미도입

**결정 사유**

| 항목 | 설명 |
|------|------|
| MVP 속도 | 프로토타입 단계에서 타입 정의 오버헤드 없이 빠른 기능 구현 우선 |
| 팀 숙련도 | JavaScript에 익숙한 상태에서 TypeScript 학습 비용 발생 |
| 전환 가능 | Vite는 TypeScript를 기본 지원하여 향후 점진적 마이그레이션 가능 |

향후 프로젝트 규모가 커지면 TypeScript 도입을 권장합니다. 특히 API 응답 타입 정의와 컴포넌트 Props 검증에서 이점이 큽니다.

---

## 4. 데이터베이스

### PostgreSQL 15

**선정 사유**

| 항목 | 설명 |
|------|------|
| JSONB 지원 | SEO 분석 결과(이슈 목록, 헤딩 구조, 키워드 밀도)는 구조가 유동적. JSONB 컬럼으로 스키마 변경 없이 유연하게 저장 |
| Full-text Search | 향후 크롤링 결과 내 텍스트 검색 시 별도 Elasticsearch 없이 PostgreSQL GIN 인덱스 활용 가능 |
| 안정성 | ACID 트랜잭션, MVCC 기반 동시성 제어로 크롤링 결과의 데이터 정합성 보장 |
| 생태계 | Spring Data JPA와의 통합이 성숙하고, TimescaleDB 확장으로 시계열 데이터 최적화 가능 |

**JSONB 활용 사례**

```
page_analyses.issues           → SEO 이슈 목록 (배열)
page_analyses.heading_structure → 헤딩 구조 (중첩 객체)
content_analyses.keyword_density → 키워드 밀도 분석 결과
content_analyses.suggestions    → AI 개선 제안 목록
reports.summary                → 리포트 요약 데이터
```

### Redis 7

**선정 사유**

| 항목 | 설명 |
|------|------|
| Refresh Token 저장 | 세션리스 JWT 아키텍처에서 Refresh Token을 Redis에 저장하여 Token Rotation 및 재사용 공격 탐지 구현 |
| 캐시 계층 | 대시보드(30초), 공개 분석(5분), 순위(2분) 등 엔드포인트별 TTL을 적용한 다계층 캐시 |
| Rate Limiting | IP 기반 요청 제한 카운터를 Redis Atomic 연산으로 구현 |
| 인메모리 속도 | 마이크로초 단위 응답으로 대시보드 조회 성능 향상 |

---

## 5. ORM 및 마이그레이션

### JPA (Spring Data JPA)

**선정 사유**

- 타입 안전한 쿼리: 컴파일 타임에 쿼리 오류 감지
- Repository 패턴: 데이터 접근 로직을 도메인 계층에서 분리
- 엔티티 매핑: 10개 테이블의 관계(1:N, 1:1)를 객체 모델로 자연스럽게 표현
- 배치 최적화: `spring.jpa.properties.hibernate.default_batch_fetch_size=100`으로 N+1 문제 해결

**`open-in-view: false` 설정**

기본값인 `true`를 명시적으로 `false`로 변경하였습니다. 이는 Controller 계층에서 Lazy Loading이 발생하여 예측 불가능한 쿼리가 실행되는 문제를 방지합니다. 모든 데이터 조회는 Service 계층에서 완료하도록 강제합니다.

### Flyway (마이그레이션)

**선정 사유**

- 버전 관리: 스키마 변경을 SQL 파일로 추적하여 환경 간 일관성 보장
- 롤백 가능: 문제 발생 시 이전 버전으로 복원
- 팀 협업: 스키마 변경 이력이 Git에 기록되어 코드 리뷰 가능

---

## 6. 아키텍처 패턴

### 이벤트 기반 비동기 처리

```
[API 요청] → 202 Accepted 즉시 반환
     |
     v
[@TransactionalEventListener(AFTER_COMMIT)]
     |
     v
[@Async] 백그라운드 실행 → 결과 저장
     |
     v
[클라이언트 폴링으로 결과 확인]
```

**적용 대상**: 크롤링, AI 콘텐츠 분석

크롤링과 AI 분석은 수초에서 수분까지 소요될 수 있습니다. 사용자 요청을 즉시 수락(202)하고 백그라운드에서 처리한 후, 클라이언트가 상태를 폴링하는 방식을 채택하였습니다.

`@TransactionalEventListener(AFTER_COMMIT)`을 사용하여 트랜잭션이 성공적으로 커밋된 후에만 비동기 작업이 시작되도록 보장합니다. 이는 트랜잭션 롤백 시 불필요한 크롤링이 실행되는 문제를 방지합니다.

### Repository 패턴

모든 데이터 접근은 Spring Data JPA Repository를 통해 이루어집니다. Service 계층은 Repository 인터페이스에만 의존하므로, 데이터 소스 변경 시 Service 코드 수정이 불필요합니다.

### DTO 분리 패턴

```
Controller ←→ Request/Response DTO ←→ Service ←→ Entity ←→ Repository
```

엔티티를 API 응답에 직접 노출하지 않습니다. Request DTO와 Response DTO를 분리하여 다음을 달성합니다.

- API 스펙과 데이터베이스 스키마의 독립적 변경
- 민감 정보(비밀번호 해시 등) 노출 방지
- 입력 검증(`@Valid`, `@Size`, `@URL`)을 DTO 레벨에서 수행

### Builder 패턴

복잡한 객체 생성(SEO 분석 결과, 크롤링 결과 등)에 Builder 패턴을 적용하여 가독성을 높이고, 파라미터 순서 오류를 방지합니다. `PublicAnalysis.markCompleted()` 메서드의 28개 파라미터를 Result 객체로 통합한 것이 대표적인 사례입니다.

### Strategy 패턴 (AI 클라이언트)

```java
public interface AiClient {
    AiAnalysisResult analyzeContent(String content, List<String> targetKeywords);
    List<MetaSuggestion> generateMeta(String content, List<String> targetKeywords, int count);
}
```

AI 제공자(Mock, OpenAI, Claude)를 `application.yml`의 `ai.provider` 설정 하나로 교체할 수 있습니다. 현재는 API 키 없이 동작하는 `MockAiClient`가 기본 구현체이며, 규칙 기반으로 SEO 분석 및 메타 태그 생성을 수행합니다.

---

## 7. 선정 기준 요약

본 프로젝트의 기술 선정은 다음 원칙을 따랐습니다.

1. **실용성 우선**: MVP를 빠르게 완성할 수 있는 검증된 기술 선택
2. **확장 가능성**: 현재는 단순하되, 향후 규모 확장 시 전환 비용이 낮은 구조
3. **보안 기본 탑재**: 인증, 입력 검증, SSRF 방어 등을 기능 구현과 동시에 적용
4. **성능 측정 기반**: 추측이 아닌 실제 쿼리 로그와 측정값 기반으로 최적화 수행
