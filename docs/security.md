# 보안 아키텍처

본 문서는 SEO Dashboard에 적용된 보안 조치를 정리합니다. 인증, 네트워크 보안, 입력 검증, 운영 환경 보안의 네 가지 영역으로 구분합니다.

---

## 1. 인증 및 인가

### JWT 기반 무상태 인증

서버 측 세션을 사용하지 않는 무상태(Stateless) 인증 구조를 채택하였습니다.

| 항목 | 설정 |
|------|------|
| Access Token | 유효기간 30분, HS512 서명 |
| Refresh Token | 유효기간 14일, Redis 저장 |
| 비밀번호 해싱 | BCrypt (cost factor 기본값 10) |
| 토큰 전달 | Authorization: Bearer 헤더 |

**동작 흐름**

```
1. 로그인 → Access Token + Refresh Token 발급
2. API 요청 → JwtAuthenticationFilter에서 Access Token 검증
3. Access Token 만료 → Refresh Token으로 갱신 요청
4. Refresh Token 만료 → 재로그인 필요
```

`JwtAuthenticationFilter`는 Spring Security 필터 체인에 등록되어 모든 인증 필요 요청의 토큰을 검증합니다. 유효하지 않은 토큰은 401 Unauthorized를 반환합니다.

### JWT 시크릿 검증 강화

비로컬(non-local) 환경에서는 약한 JWT 시크릿 키를 거부합니다. 애플리케이션 시작 시 시크릿 키의 길이와 엔트로피를 검증하여, 기본값이나 쉽게 추측 가능한 키가 프로덕션에 배포되는 것을 방지합니다.

### Refresh Token Rotation

Refresh Token을 사용할 때마다 기존 토큰을 폐기하고 새 토큰을 발급합니다. 이를 통해 탈취된 토큰의 유효 기간을 최소화합니다.

**재사용 공격(Replay Attack) 탐지**

```
1. 정상 사용자: Refresh Token A 사용 → Token A 폐기, Token B 발급
2. 공격자: 탈취한 Token A 사용 시도
3. 시스템: Token A가 이미 사용된 토큰임을 감지
4. 대응: 해당 사용자의 모든 Refresh Token 즉시 폐기 (강제 재로그인)
```

이미 사용된(rotated) Refresh Token이 다시 제시되면, 토큰 탈취가 발생한 것으로 간주하고 해당 사용자의 전체 세션을 무효화합니다.

---

## 2. 네트워크 보안

### SSRF 방어 (Server-Side Request Forgery)

크롤링 기능은 사용자가 제공한 URL로 HTTP 요청을 보내므로, SSRF 공격에 취약할 수 있습니다. `UrlValidator`를 통해 다음 주소로의 요청을 차단합니다.

| 차단 대상 | 예시 |
|-----------|------|
| 사설 IP 대역 | 10.x.x.x, 172.16~31.x.x, 192.168.x.x |
| 루프백 주소 | 127.0.0.1, localhost, [::1] |
| 링크-로컬 주소 | 169.254.x.x |
| 클라우드 메타데이터 | 169.254.169.254 (AWS/GCP/Azure 인스턴스 메타데이터) |

크롤링 요청 시 URL을 DNS 해석한 후, 해석된 IP가 위 차단 목록에 해당하면 요청을 거부합니다.

### Rate Limiting

공개 API에 IP 기반 요청 제한을 적용하여 과도한 요청을 방지합니다.

| 엔드포인트 | 제한 |
|-----------|------|
| 공개 SEO 분석 | IP당 시간 단위 제한 |
| 인증 API | 엔드포인트별 분당 제한 |

Redis의 Atomic 연산(INCR + EXPIRE)을 사용하여 분산 환경에서도 정확한 카운팅을 보장합니다. 제한 초과 시 429 Too Many Requests를 반환합니다.

### CORS 설정 외부화

CORS(Cross-Origin Resource Sharing) 허용 도메인을 `application.yml`이 아닌 환경변수로 관리합니다.

```yaml
cors:
  allowed-origins: ${CORS_ALLOWED_ORIGINS:http://localhost:3001}
```

이를 통해 배포 환경별로 허용 도메인을 코드 변경 없이 제어할 수 있습니다. 로컬 개발 환경에서는 기본값으로 `http://localhost:3001`을 허용합니다.

---

## 3. 입력 검증

### Bean Validation

모든 API 요청 DTO에 Jakarta Bean Validation 어노테이션을 적용합니다.

| 어노테이션 | 적용 대상 | 목적 |
|-----------|----------|------|
| `@Valid` | 컨트롤러 메서드 파라미터 | DTO 내부 검증 규칙 활성화 |
| `@NotBlank` | 필수 문자열 필드 | 빈 문자열 및 null 방지 |
| `@Size` | 문자열 길이 | 과도한 입력 데이터 방지 |
| `@URL` | URL 필드 | 유효한 URL 형식 강제 |
| `@Email` | 이메일 필드 | 유효한 이메일 형식 강제 |

검증 실패 시 400 Bad Request와 함께 필드별 오류 메시지를 반환합니다. `GlobalExceptionHandler`에서 `MethodArgumentNotValidException`을 일괄 처리합니다.

### SQL Injection 방지

Spring Data JPA의 파라미터 바인딩을 사용하여 SQL Injection을 원천적으로 방지합니다.

```java
// 안전: 파라미터 바인딩
@Query("SELECT s FROM Site s WHERE s.url = :url AND s.user = :user")
Optional<Site> findByUrlAndUser(@Param("url") String url, @Param("user") User user);
```

프로젝트 전체에서 문자열 연결 방식의 쿼리 생성을 사용하지 않습니다. JPQL 및 Spring Data JPA 메서드 이름 쿼리만 사용하여 파라미터가 자동으로 바인딩됩니다.

### 응답 크기 제한

크롤링 대상 페이지의 응답 크기를 5MB로 제한합니다. 악의적으로 큰 페이지를 크롤링하여 서버 메모리를 고갈시키는 공격을 방지합니다.

---

## 4. 운영 환경 보안

### 프로덕션 프로파일

`production` 프로파일에서는 개발 편의 기능을 비활성화합니다.

| 항목 | 로컬(local) | 프로덕션(production) |
|------|-------------|---------------------|
| Swagger UI | 활성화 | 비활성화 |
| Actuator 포트 | 메인 포트와 동일 | 별도 포트로 분리 |
| JWT 시크릿 | 기본값 허용 | 강력한 키 필수 |
| CORS | localhost 허용 | 환경변수로 지정한 도메인만 허용 |
| Hibernate SQL 로깅 | 활성화 | 비활성화 |

Actuator 포트를 분리하여 외부에서 `/actuator/health`, `/actuator/metrics` 등의 운영 엔드포인트에 직접 접근하는 것을 방지합니다. 내부 네트워크에서만 모니터링 데이터를 조회할 수 있습니다.

---

## 5. 보안 조치 요약

```
[클라이언트 요청]
       |
       v
  Rate Limiting (IP 기반, Redis)
       |
       v
  CORS 검증 (환경변수 기반 허용 도메인)
       |
       v
  입력 검증 (@Valid, @Size, @URL)
       |
       v
  JWT 인증 (JwtAuthenticationFilter)
       |
       v
  SSRF 검증 (UrlValidator - 크롤링 요청 시)
       |
       v
  SQL Injection 방지 (JPA 파라미터 바인딩)
       |
       v
  비즈니스 로직 실행
       |
       v
  응답 크기 제한 (5MB)
```

---

## 6. 관련 커밋 이력

| 커밋 | 설명 |
|------|------|
| `2aa5c71` | Refresh Token Rotation 및 재사용 공격 탐지 |
| `e06c89e` | SSRF 방어 추가 (private IP, localhost, 클라우드 메타데이터 차단) |
| `8723048` | 공개 API Rate Limiting 추가 (IP 기반 요청 제한) |
| `b632658` | JWT 시크릿 검증 강화 (non-local 환경에서 약한 키 거부) |
| `213a307` | CORS 환경변수화 및 프로덕션 프로파일 추가 |
| `df581c6` | 401 처리 개선 (프론트엔드 CustomEvent 기반) |
| `c4998b4` | JWT claim 불일치 수정 (user_id -> sub) |
