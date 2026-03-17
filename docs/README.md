# SEO Dashboard 프로젝트 보고서

## 1. 프로젝트 개요

SEO Dashboard는 웹사이트의 검색엔진 최적화(SEO) 상태를 종합적으로 분석하고 관리하는 올인원 플랫폼입니다.

사이트 크롤링, 키워드 순위 추적, AI 콘텐츠 최적화, 대시보드 리포트를 하나의 시스템에서 제공하며, 로그인 없이도 URL 하나로 즉시 SEO 진단이 가능합니다.

### 핵심 가치

| 구분 | 설명 |
|------|------|
| **즉시 진단** | 로그인 없이 URL 입력만으로 SEO 분석 결과 제공 |
| **종합 관리** | 크롤링, 키워드, AI 분석, 리포트를 단일 플랫폼에서 수행 |
| **확장 가능** | 멀티모듈 구조로 설계하여 향후 마이크로서비스 전환 가능 |
| **보안 우선** | JWT 인증, SSRF 방어, Rate Limiting 등 실무 수준 보안 적용 |

---

## 2. 프로젝트 현황 요약

| 지표 | 수치 |
|------|------|
| 총 커밋 수 | 101 |
| 보안/성능/리팩터링 커밋 | 21 (전체의 약 21%) |
| 단위 테스트 수 | 143개 |
| API 엔드포인트 | 41개 (공개 5개 + 인증 36개) |
| Java 소스 파일 | 141개 |
| Gradle 모듈 | 5개 (seo-api, seo-common, seo-crawler, seo-ai, seo-scheduler) |
| 프론트엔드 | React SPA (다크 테마 대시보드) |
| 스크린샷 문서화 | 18장 |

---

## 3. 기술 스택 요약

### 백엔드

| 기술 | 버전 | 선정 사유 |
|------|------|-----------|
| Java | 21 (LTS) | Virtual Threads로 병렬 크롤링 성능 향상, Record 패턴으로 DTO 간결화 |
| Spring Boot | 3.3.7 | 검증된 엔터프라이즈 프레임워크, 멀티모듈 지원 |
| PostgreSQL | 15 | JSONB로 유연한 SEO 데이터 저장, Full-text Search 확장 가능 |
| Redis | 7 | Refresh Token 관리 및 캐시 계층 (세션리스 구조) |
| Gradle (Kotlin DSL) | - | 멀티모듈 빌드 관리, 타입 안전한 빌드 스크립트 |

### 프론트엔드

| 기술 | 선정 사유 |
|------|-----------|
| React | 컴포넌트 기반 UI, 풍부한 생태계 |
| Vite | 빠른 HMR, Next.js 대비 SPA에 적합한 경량 빌드 도구 |
| Recharts | SEO 점수 및 키워드 순위 차트 시각화 |

상세 기술 결정 사항은 [기술 스택 및 설계 결정](./tech-decisions.md) 문서를 참조하십시오.

---

## 4. 시스템 구조

```
                        [React SPA]
                            |
                        [Spring Boot API]
                       /    |    |    \
              [Crawler] [Scheduler] [AI] [Common]
                  |         |        |
                  v         v        v
         [Spring Event 기반 비동기 처리]
                  |         |        |
            [PostgreSQL] [Redis] [AI API]
```

5개 Gradle 모듈이 `seo-common`을 공유하며, 모듈 간 통신은 Spring Event + `@Async` 기반 비동기 처리로 느슨하게 결합되어 있습니다. 향후 Kafka 도입 시 코드 변경을 최소화할 수 있는 구조입니다.

상세 내용은 [시스템 아키텍처](./architecture.md) 문서를 참조하십시오.

---

## 5. 핵심 성과

### 보안

- JWT 기반 무상태 인증 (Access + Refresh Token)
- Refresh Token Rotation 및 재사용 공격 탐지
- SSRF 방어 (사설 IP, localhost, 클라우드 메타데이터 차단)
- IP 기반 Rate Limiting
- 프로덕션 프로파일에서 Swagger 비활성화 및 Actuator 포트 분리

상세 내용은 [보안 아키텍처](./security.md) 문서를 참조하십시오.

### 성능

- N+1 쿼리 해결: 대시보드 조회 시 211개 쿼리를 3개로 축소
- Redis 캐시 도입: 대시보드 30초, 공개 분석 5분, 순위 2분 TTL
- 병렬 크롤링: Virtual Threads + Semaphore 적용으로 3~5배 속도 향상
- 프론트엔드: Recharts(342KB) 코드 스플리팅으로 초기 로딩 최적화

상세 내용은 [성능 최적화](./performance.md) 문서를 참조하십시오.

### 코드 품질

- 143개 단위 테스트 (인증, 크롤러, SEO 분석기, URL 유틸리티, Rate Limiter)
- String 기반 상태값을 Enum으로 전환하여 타입 안전성 확보
- 28개 파라미터 메서드를 Result 객체로 리팩터링
- URL 정규화 로직 3곳 중복 제거

---

## 6. 문서 목차

| 문서 | 설명 |
|------|------|
| [시스템 아키텍처](./architecture.md) | 멀티모듈 구조, 데이터 흐름, 모듈 간 의존 관계 |
| [API 명세서](./api-spec.md) | 41개 REST API 엔드포인트 상세 |
| [데이터베이스 스키마](./database-schema.md) | 10개 테이블 구조, JSONB 활용, ERD |
| [기술 스택 및 설계 결정](./tech-decisions.md) | 기술 선정 근거 및 아키텍처 패턴 |
| [보안 아키텍처](./security.md) | 인증, SSRF 방어, Rate Limiting, 입력 검증 |
| [성능 최적화](./performance.md) | N+1 해결, 캐시, 병렬 크롤링, 코드 스플리팅 |
| [배포 가이드](./deployment-guide.md) | Docker Compose 환경 및 프로덕션 배포 |
| [로컬 개발 환경 설정](./local-setup.md) | 개발 환경 구성 및 실행 방법 |
| [테스트 전략](./testing.md) | 테스트 구조, 커버리지, 실행 방법 |

---

## 7. 개발 이력

| 단계 | 구현 내용 |
|------|-----------|
| Phase 1 | 시스템 설계, 멀티모듈 프로젝트, JWT 인증, 사이트 CRUD |
| Phase 2 | BFS 크롤링 엔진, 6종 SEO 분석기, 비동기 처리 |
| Phase 3 | 키워드 CRUD, 순위 수집(시뮬레이터), 순위 이력 및 트렌드 분석 |
| Phase 4 | AI 콘텐츠 분석, 키워드 밀도/가독성, 메타 생성, Strategy Pattern |
| Phase 5 | 종합/사이트별 대시보드, 리포트, 알림 시스템 |
| Frontend | React 다크 테마 대시보드, 차트, 공개 SEO 진단 페이지 |
| 품질 강화 | 보안 21개 커밋, 성능 최적화, 143개 테스트, 리팩터링 |

---

## 8. 향후 로드맵

- 실제 Google SERP API 연동 (키워드 순위 실시간 수집)
- OpenAI/Claude API 연동 (AI 분석 고도화)
- Kafka 도입 (모듈 간 메시지 큐)
- WebSocket 실시간 알림
- GitHub Actions CI/CD 파이프라인
- Prometheus + Grafana 모니터링
- 테스트 커버리지 80% 이상 확보
