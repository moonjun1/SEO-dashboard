# SEO 올인원 대시보드 - API 설계 명세

## 1. 공통 사항

### 1.1 Base URL
```
https://api.seodashboard.com/api/v1
```
개발 환경: `http://localhost:8080/api/v1`

### 1.2 인증 방식
- JWT (JSON Web Token) 기반 Bearer 인증
- Access Token: 유효기간 30분
- Refresh Token: 유효기간 14일, HttpOnly 쿠키로 전달
- 인증 필요 엔드포인트에는 헤더 필수: `Authorization: Bearer {access_token}`

### 1.3 공통 응답 형식

**성공 응답:**
```json
{
  "success": true,
  "data": { ... },
  "message": null
}
```

**목록 응답 (페이지네이션):**
```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8,
    "first": true,
    "last": false
  },
  "message": null
}
```

**에러 응답:**
```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "SITE_NOT_FOUND",
    "message": "사이트를 찾을 수 없습니다",
    "details": [
      {
        "field": "siteId",
        "message": "존재하지 않는 사이트 ID입니다"
      }
    ],
    "timestamp": "2026-03-16T12:00:00Z"
  }
}
```

### 1.4 페이지네이션 표준

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `page` | Integer | 0 | 페이지 번호 (0-based) |
| `size` | Integer | 20 | 페이지 크기 (최대 100) |
| `sort` | String | 엔드포인트별 상이 | 정렬 기준 (예: `createdAt,desc`) |

### 1.5 에러 코드 체계

| HTTP 상태 | 에러 코드 | 설명 |
|-----------|----------|------|
| 400 | `VALIDATION_ERROR` | 요청 데이터 유효성 검증 실패 |
| 400 | `INVALID_PARAMETER` | 잘못된 파라미터 |
| 401 | `UNAUTHORIZED` | 인증 필요 |
| 401 | `TOKEN_EXPIRED` | 토큰 만료 |
| 401 | `INVALID_TOKEN` | 유효하지 않은 토큰 |
| 403 | `ACCESS_DENIED` | 권한 없음 |
| 404 | `SITE_NOT_FOUND` | 사이트 없음 |
| 404 | `CRAWL_JOB_NOT_FOUND` | 크롤링 작업 없음 |
| 404 | `KEYWORD_NOT_FOUND` | 키워드 없음 |
| 404 | `REPORT_NOT_FOUND` | 리포트 없음 |
| 409 | `DUPLICATE_SITE` | 중복 사이트 등록 |
| 409 | `DUPLICATE_KEYWORD` | 중복 키워드 등록 |
| 409 | `CRAWL_ALREADY_RUNNING` | 크롤링 이미 실행 중 |
| 429 | `RATE_LIMIT_EXCEEDED` | 요청 한도 초과 |
| 500 | `INTERNAL_ERROR` | 서버 내부 오류 |
| 503 | `SERVICE_UNAVAILABLE` | 서비스 일시 불가 |

### 1.6 Rate Limiting

| 엔드포인트 그룹 | 제한 | 윈도우 |
|-----------------|------|--------|
| 인증 API | 10회 | 1분 |
| 크롤링 시작 | 5회 | 1시간 |
| AI 분석 | 20회 | 1시간 |
| 일반 조회 API | 100회 | 1분 |
| 리포트 생성 | 10회 | 1시간 |

---

## 2. 인증 API

### 2.1 회원가입

```
POST /auth/signup
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!",
  "name": "홍길동"
}
```

**유효성 검증:**
- `email`: 이메일 형식, 최대 255자, 중복 불가
- `password`: 최소 8자, 대소문자 + 숫자 + 특수문자 포함
- `name`: 최소 2자, 최대 100자

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "createdAt": "2026-03-16T12:00:00Z"
  }
}
```

### 2.2 로그인

```
POST /auth/login
```

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "SecurePass123!"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 1800,
    "user": {
      "id": 1,
      "email": "user@example.com",
      "name": "홍길동",
      "role": "USER"
    }
  }
}
```

**Refresh Token**: `Set-Cookie: refresh_token=...; HttpOnly; Secure; SameSite=Strict; Path=/api/v1/auth; Max-Age=1209600`

### 2.3 토큰 갱신

```
POST /auth/refresh
```

**Request**: Cookie에 포함된 `refresh_token` 자동 전송

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIs...",
    "tokenType": "Bearer",
    "expiresIn": 1800
  }
}
```

### 2.4 로그아웃

```
POST /auth/logout
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": null,
  "message": "로그아웃 되었습니다"
}
```

### 2.5 내 정보 조회

```
GET /auth/me
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "email": "user@example.com",
    "name": "홍길동",
    "role": "USER",
    "isActive": true,
    "lastLoginAt": "2026-03-16T11:30:00Z",
    "createdAt": "2026-01-15T09:00:00Z"
  }
}
```

---

## 3. 사이트 관리 API

### 3.1 사이트 등록

```
POST /sites
```

**Request Body:**
```json
{
  "url": "https://example.com",
  "name": "내 블로그",
  "description": "기술 블로그",
  "crawlIntervalHours": 168
}
```

**유효성 검증:**
- `url`: 유효한 URL 형식, HTTP/HTTPS 프로토콜 필수, 동일 사용자 내 중복 불가
- `name`: 최소 1자, 최대 255자
- `crawlIntervalHours`: 최소 24, 최대 720

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "url": "https://example.com",
    "name": "내 블로그",
    "description": "기술 블로그",
    "seoScore": null,
    "lastCrawledAt": null,
    "crawlIntervalHours": 168,
    "isActive": true,
    "createdAt": "2026-03-16T12:00:00Z"
  }
}
```

### 3.2 사이트 목록 조회

```
GET /sites?page=0&size=20&sort=createdAt,desc
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "url": "https://example.com",
        "name": "내 블로그",
        "seoScore": 78.5,
        "lastCrawledAt": "2026-03-15T06:00:00Z",
        "isActive": true,
        "keywordCount": 12,
        "createdAt": "2026-01-15T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 3,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 3.3 사이트 상세 조회

```
GET /sites/{siteId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "url": "https://example.com",
    "name": "내 블로그",
    "description": "기술 블로그",
    "seoScore": 78.5,
    "lastCrawledAt": "2026-03-15T06:00:00Z",
    "crawlIntervalHours": 168,
    "isActive": true,
    "stats": {
      "totalPages": 142,
      "totalKeywords": 12,
      "avgPageScore": 72.3,
      "issuesCount": 23,
      "lastCrawlDuration": 45000
    },
    "createdAt": "2026-01-15T09:00:00Z",
    "updatedAt": "2026-03-15T06:00:00Z"
  }
}
```

### 3.4 사이트 수정

```
PUT /sites/{siteId}
```

**Request Body:**
```json
{
  "name": "내 기술 블로그",
  "description": "업데이트된 설명",
  "crawlIntervalHours": 72,
  "isActive": true
}
```

**Response (200 OK):** 사이트 상세와 동일한 구조

### 3.5 사이트 삭제

```
DELETE /sites/{siteId}
```

**Response (204 No Content)**

---

## 4. 크롤링 API

### 4.1 크롤링 시작

```
POST /sites/{siteId}/crawl
```

**Request Body (선택):**
```json
{
  "maxPages": 100,
  "maxDepth": 3
}
```

**유효성 검증:**
- `maxPages`: 최소 1, 최대 500
- `maxDepth`: 최소 1, 최대 5
- 동일 사이트에서 이미 실행 중인 크롤링이 없어야 함

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "jobId": 42,
    "siteId": 1,
    "status": "PENDING",
    "maxPages": 100,
    "maxDepth": 3,
    "triggerType": "MANUAL",
    "createdAt": "2026-03-16T12:00:00Z"
  },
  "message": "크롤링 작업이 대기열에 추가되었습니다"
}
```

### 4.2 크롤링 상태 조회

```
GET /sites/{siteId}/crawl/jobs/{jobId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "jobId": 42,
    "siteId": 1,
    "status": "RUNNING",
    "triggerType": "MANUAL",
    "maxPages": 100,
    "maxDepth": 3,
    "totalPages": 67,
    "errorCount": 2,
    "progress": {
      "percentage": 67,
      "pagesProcessed": 67,
      "pagesRemaining": 33,
      "elapsedTimeMs": 23000,
      "estimatedRemainingMs": 11000
    },
    "startedAt": "2026-03-16T12:00:05Z",
    "completedAt": null,
    "createdAt": "2026-03-16T12:00:00Z"
  }
}
```

### 4.3 크롤링 작업 목록 조회

```
GET /sites/{siteId}/crawl/jobs?page=0&size=10&sort=createdAt,desc
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "jobId": 42,
        "status": "COMPLETED",
        "triggerType": "MANUAL",
        "totalPages": 142,
        "errorCount": 3,
        "startedAt": "2026-03-15T06:00:00Z",
        "completedAt": "2026-03-15T06:00:45Z",
        "durationMs": 45000,
        "createdAt": "2026-03-15T05:59:58Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 4.4 크롤링 결과 조회

```
GET /sites/{siteId}/crawl/jobs/{jobId}/results?page=0&size=20&sort=seoScore,asc
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `statusCode` | Integer | HTTP 상태 코드 필터 (예: 404) |
| `minScore` | Double | 최소 SEO 점수 |
| `maxScore` | Double | 최대 SEO 점수 |
| `hasIssues` | Boolean | 이슈 존재 여부 필터 |
| `isInternal` | Boolean | 내부 페이지 필터 |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "resultId": 1001,
        "url": "https://example.com/blog/post-1",
        "statusCode": 200,
        "responseTimeMs": 320,
        "depth": 1,
        "isInternal": true,
        "analysis": {
          "seoScore": 65.5,
          "titleScore": 80.0,
          "metaDescriptionScore": 45.0,
          "headingScore": 70.0,
          "imageScore": 50.0,
          "linkScore": 85.0,
          "performanceScore": 72.0,
          "issuesCount": 4,
          "topIssues": [
            {"type": "MISSING_ALT", "severity": "WARNING"},
            {"type": "META_DESC_TOO_SHORT", "severity": "WARNING"}
          ]
        }
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 142,
    "totalPages": 8,
    "first": true,
    "last": false
  }
}
```

### 4.5 페이지 분석 상세 조회

```
GET /sites/{siteId}/crawl/results/{resultId}/analysis
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "resultId": 1001,
    "url": "https://example.com/blog/post-1",
    "statusCode": 200,
    "title": "SEO 최적화 가이드 - 내 블로그",
    "metaDescription": "SEO 최적화를 위한 완벽 가이드...",
    "canonicalUrl": "https://example.com/blog/post-1",
    "responseTimeMs": 320,
    "contentLength": 45230,
    "analysis": {
      "seoScore": 65.5,
      "titleScore": 80.0,
      "titleLength": 22,
      "metaDescriptionScore": 45.0,
      "metaDescriptionLength": 85,
      "headingScore": 70.0,
      "headingStructure": {"h1": 1, "h2": 3, "h3": 5, "h4": 0},
      "imageScore": 50.0,
      "imagesTotal": 8,
      "imagesWithoutAlt": 3,
      "linkScore": 85.0,
      "internalLinksCount": 12,
      "externalLinksCount": 5,
      "brokenLinksCount": 1,
      "performanceScore": 72.0,
      "hasOgTags": true,
      "hasTwitterCards": false,
      "hasStructuredData": false,
      "isMobileFriendly": true,
      "issues": [
        {
          "type": "MISSING_ALT",
          "severity": "WARNING",
          "message": "3개 이미지에 alt 속성이 없습니다",
          "details": {"urls": ["img1.png", "img2.png", "img3.png"]}
        },
        {
          "type": "META_DESC_TOO_SHORT",
          "severity": "WARNING",
          "message": "메타 디스크립션이 120자 미만입니다 (현재 85자)",
          "details": {"currentLength": 85, "recommendedMin": 120}
        },
        {
          "type": "BROKEN_LINK",
          "severity": "ERROR",
          "message": "깨진 링크 1건이 발견되었습니다",
          "details": {"url": "https://example.com/old-page", "statusCode": 404}
        },
        {
          "type": "MISSING_TWITTER_CARD",
          "severity": "INFO",
          "message": "Twitter Card 메타태그가 없습니다"
        }
      ]
    }
  }
}
```

### 4.6 크롤링 취소

```
POST /sites/{siteId}/crawl/jobs/{jobId}/cancel
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "jobId": 42,
    "status": "CANCELLED",
    "totalPages": 67,
    "completedAt": "2026-03-16T12:05:00Z"
  },
  "message": "크롤링 작업이 취소되었습니다"
}
```

---

## 5. 키워드 API

### 5.1 키워드 등록

```
POST /sites/{siteId}/keywords
```

**Request Body:**
```json
{
  "keyword": "SEO 최적화",
  "targetUrl": "https://example.com/seo-guide",
  "searchEngine": "GOOGLE",
  "countryCode": "KR",
  "languageCode": "ko"
}
```

**유효성 검증:**
- `keyword`: 최소 1자, 최대 500자
- `searchEngine`: GOOGLE (현재 지원 범위)
- 동일 사이트 + 키워드 + 검색엔진 + 국가 조합 중복 불가

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "id": 1,
    "siteId": 1,
    "keyword": "SEO 최적화",
    "targetUrl": "https://example.com/seo-guide",
    "searchEngine": "GOOGLE",
    "countryCode": "KR",
    "languageCode": "ko",
    "isActive": true,
    "latestRank": null,
    "createdAt": "2026-03-16T12:00:00Z"
  }
}
```

### 5.2 키워드 일괄 등록

```
POST /sites/{siteId}/keywords/batch
```

**Request Body:**
```json
{
  "keywords": [
    {"keyword": "SEO 최적화", "targetUrl": "https://example.com/seo-guide"},
    {"keyword": "검색엔진 마케팅", "targetUrl": null},
    {"keyword": "구글 순위", "targetUrl": null}
  ],
  "searchEngine": "GOOGLE",
  "countryCode": "KR",
  "languageCode": "ko"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "data": {
    "created": 3,
    "skipped": 0,
    "keywords": [ ... ]
  }
}
```

### 5.3 키워드 목록 조회

```
GET /sites/{siteId}/keywords?page=0&size=20&sort=keyword,asc
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `isActive` | Boolean | 활성 상태 필터 |
| `search` | String | 키워드 검색어 |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "keyword": "SEO 최적화",
        "targetUrl": "https://example.com/seo-guide",
        "searchEngine": "GOOGLE",
        "countryCode": "KR",
        "isActive": true,
        "latestRanking": {
          "rank": 5,
          "previousRank": 7,
          "rankChange": 2,
          "recordedAt": "2026-03-16T06:00:00Z"
        },
        "createdAt": "2026-01-15T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 12,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 5.4 키워드 순위 이력 조회

```
GET /sites/{siteId}/keywords/{keywordId}/rankings?period=30d
```

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `period` | String | 30d | 조회 기간 (7d, 30d, 90d, 180d, 1y) |
| `granularity` | String | daily | 집계 단위 (daily, weekly, monthly) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "keywordId": 1,
    "keyword": "SEO 최적화",
    "period": "30d",
    "granularity": "daily",
    "summary": {
      "currentRank": 5,
      "bestRank": 3,
      "worstRank": 12,
      "avgRank": 6.8,
      "rankChange30d": 4,
      "trend": "IMPROVING"
    },
    "rankings": [
      {
        "recordedAt": "2026-03-16T06:00:00Z",
        "rank": 5,
        "previousRank": 6,
        "rankChange": 1,
        "url": "https://example.com/seo-guide",
        "searchVolume": 2400
      },
      {
        "recordedAt": "2026-03-15T06:00:00Z",
        "rank": 6,
        "previousRank": 6,
        "rankChange": 0,
        "url": "https://example.com/seo-guide",
        "searchVolume": 2400
      }
    ]
  }
}
```

### 5.5 키워드 수정

```
PUT /sites/{siteId}/keywords/{keywordId}
```

**Request Body:**
```json
{
  "targetUrl": "https://example.com/new-seo-guide",
  "isActive": true
}
```

**Response (200 OK):** 키워드 상세와 동일한 구조

### 5.6 키워드 삭제

```
DELETE /sites/{siteId}/keywords/{keywordId}
```

**Response (204 No Content)**

### 5.7 경쟁사 키워드 비교

```
GET /sites/{siteId}/keywords/{keywordId}/competitors
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "keywordId": 1,
    "keyword": "SEO 최적화",
    "myRank": 5,
    "competitors": [
      {
        "rank": 1,
        "url": "https://competitor1.com/seo",
        "title": "SEO 최적화 완전 가이드",
        "domain": "competitor1.com"
      },
      {
        "rank": 2,
        "url": "https://competitor2.com/seo-tips",
        "title": "SEO 최적화 팁 10가지",
        "domain": "competitor2.com"
      }
    ],
    "serpFeatures": {
      "featuredSnippet": true,
      "peopleAlsoAsk": true,
      "localPack": false,
      "knowledgePanel": false
    },
    "recordedAt": "2026-03-16T06:00:00Z"
  }
}
```

---

## 6. AI 분석 API

### 6.1 콘텐츠 SEO 분석 요청

```
POST /analysis/content
```

**Request Body:**
```json
{
  "siteId": 1,
  "title": "SEO 최적화 완벽 가이드 2026",
  "content": "검색엔진 최적화(SEO)는 웹사이트의 가시성을 높이기 위한...",
  "targetKeywords": "SEO 최적화, 검색엔진, 구글 순위"
}
```

**유효성 검증:**
- `content`: 최소 100자, 최대 50,000자
- `targetKeywords`: 최대 10개 (쉼표 구분)
- `siteId`: 선택 (NULL 허용)

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "analysisId": 15,
    "status": "PENDING",
    "createdAt": "2026-03-16T12:00:00Z"
  },
  "message": "콘텐츠 분석이 요청되었습니다. 완료 시 알림을 보내드립니다."
}
```

### 6.2 콘텐츠 분석 결과 조회

```
GET /analysis/content/{analysisId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "analysisId": 15,
    "status": "COMPLETED",
    "title": "SEO 최적화 완벽 가이드 2026",
    "seoScore": 72.5,
    "readabilityScore": 85.0,
    "keywordDensity": {
      "SEO 최적화": {"count": 15, "density": 2.3, "status": "OPTIMAL"},
      "검색엔진": {"count": 8, "density": 1.2, "status": "LOW"},
      "구글 순위": {"count": 3, "density": 0.5, "status": "LOW"}
    },
    "structureAnalysis": {
      "wordCount": 2500,
      "paragraphCount": 18,
      "avgSentenceLength": 22,
      "headingCount": {"h2": 5, "h3": 8},
      "hasIntroduction": true,
      "hasConclusion": true,
      "internalLinks": 3,
      "externalLinks": 7
    },
    "suggestions": [
      {
        "category": "KEYWORD",
        "priority": "HIGH",
        "message": "'검색엔진' 키워드 사용 빈도를 높여주세요",
        "currentValue": "1.2%",
        "recommendedValue": "2.0~3.0%"
      },
      {
        "category": "STRUCTURE",
        "priority": "MEDIUM",
        "message": "첫 번째 문단에 주요 키워드를 포함하세요"
      },
      {
        "category": "READABILITY",
        "priority": "LOW",
        "message": "일부 문장이 40자를 초과합니다. 간결하게 수정하세요"
      }
    ],
    "generatedMetaTitle": "SEO 최적화 완벽 가이드 2026 | 검색 순위 올리는 방법",
    "generatedMetaDescription": "2026년 최신 SEO 최적화 전략을 알아보세요. 구글 검색 순위를 높이는 실전 가이드, 키워드 분석부터 기술 SEO까지 단계별로 설명합니다.",
    "aiProvider": "OPENAI",
    "aiModel": "gpt-4",
    "createdAt": "2026-03-16T12:00:00Z",
    "completedAt": "2026-03-16T12:00:12Z"
  }
}
```

### 6.3 메타 태그 생성

```
POST /analysis/meta-generate
```

**Request Body:**
```json
{
  "content": "검색엔진 최적화(SEO)는 웹사이트의 가시성을 높이기 위한...",
  "targetKeywords": "SEO 최적화, 검색엔진",
  "tone": "PROFESSIONAL",
  "count": 3
}
```

**유효성 검증:**
- `tone`: PROFESSIONAL, CASUAL, CREATIVE 중 하나
- `count`: 최소 1, 최대 5 (생성할 메타 태그 세트 수)

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "suggestions": [
      {
        "metaTitle": "SEO 최적화 완벽 가이드 2026 | 검색 순위 올리는 방법",
        "metaTitleLength": 32,
        "metaDescription": "2026년 최신 SEO 최적화 전략을 알아보세요. 구글 검색 순위를 높이는 실전 가이드입니다.",
        "metaDescriptionLength": 52
      },
      {
        "metaTitle": "검색엔진 최적화(SEO) A to Z | 실전 가이드 2026",
        "metaTitleLength": 30,
        "metaDescription": "SEO 전문가가 알려주는 검색엔진 최적화 핵심 전략. 키워드 분석, 기술 SEO, 콘텐츠 최적화까지.",
        "metaDescriptionLength": 50
      },
      {
        "metaTitle": "SEO 최적화로 구글 1페이지 달성하기 | 2026 전략",
        "metaTitleLength": 29,
        "metaDescription": "구글 검색 1페이지에 노출되는 SEO 최적화 비법. 단계별 실전 가이드로 검색 순위를 높여보세요.",
        "metaDescriptionLength": 52
      }
    ]
  }
}
```

### 6.4 콘텐츠 분석 목록 조회

```
GET /analysis/content?page=0&size=20&sort=createdAt,desc
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `siteId` | Long | 사이트 필터 |
| `status` | String | 상태 필터 (PENDING, PROCESSING, COMPLETED, FAILED) |
| `minScore` | Double | 최소 SEO 점수 |
| `maxScore` | Double | 최대 SEO 점수 |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "analysisId": 15,
        "title": "SEO 최적화 완벽 가이드 2026",
        "seoScore": 72.5,
        "readabilityScore": 85.0,
        "status": "COMPLETED",
        "aiProvider": "OPENAI",
        "suggestionsCount": 5,
        "createdAt": "2026-03-16T12:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 8,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

---

## 7. 리포트 API

### 7.1 리포트 생성 요청

```
POST /sites/{siteId}/reports
```

**Request Body:**
```json
{
  "type": "CUSTOM",
  "periodStart": "2026-02-01",
  "periodEnd": "2026-02-28",
  "title": "2월 SEO 분석 리포트"
}
```

**유효성 검증:**
- `type`: WEEKLY, MONTHLY, CUSTOM 중 하나
- `periodStart` < `periodEnd`
- 기간 최대 365일
- WEEKLY: 자동으로 최근 7일, MONTHLY: 자동으로 최근 30일 (직접 지정 불필요)

**Response (202 Accepted):**
```json
{
  "success": true,
  "data": {
    "reportId": 5,
    "siteId": 1,
    "type": "CUSTOM",
    "title": "2월 SEO 분석 리포트",
    "periodStart": "2026-02-01",
    "periodEnd": "2026-02-28",
    "status": "PENDING",
    "createdAt": "2026-03-16T12:00:00Z"
  },
  "message": "리포트 생성이 요청되었습니다"
}
```

### 7.2 리포트 목록 조회

```
GET /sites/{siteId}/reports?page=0&size=10&sort=createdAt,desc
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `type` | String | 리포트 유형 필터 |
| `status` | String | 상태 필터 |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "reportId": 5,
        "type": "MONTHLY",
        "title": "2026년 2월 월간 리포트",
        "periodStart": "2026-02-01",
        "periodEnd": "2026-02-28",
        "status": "COMPLETED",
        "summary": {
          "overallScore": 78.5,
          "scoreChange": 3.2
        },
        "fileSize": 245760,
        "createdAt": "2026-03-01T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 5,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 7.3 리포트 상세 조회

```
GET /sites/{siteId}/reports/{reportId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "reportId": 5,
    "siteId": 1,
    "type": "MONTHLY",
    "title": "2026년 2월 월간 리포트",
    "periodStart": "2026-02-01",
    "periodEnd": "2026-02-28",
    "status": "COMPLETED",
    "summary": {
      "overallScore": 78.5,
      "scoreChange": 3.2,
      "pagesCrawled": 142,
      "issuesFound": 23,
      "issuesResolved": 8,
      "topKeywords": [
        {"keyword": "SEO 도구", "avgRank": 5.2, "change": 2},
        {"keyword": "사이트 분석", "avgRank": 12.8, "change": -1}
      ],
      "scoreBreakdown": {
        "technical": 82.0,
        "content": 75.0,
        "performance": 70.5,
        "links": 88.0
      },
      "priorityActions": [
        "깨진 링크 12건 수정",
        "이미지 alt 태그 15건 추가",
        "메타 디스크립션 누락 8건 작성"
      ]
    },
    "filePath": "/reports/site-1/2026-02-monthly.pdf",
    "fileSize": 245760,
    "createdAt": "2026-03-01T09:00:00Z"
  }
}
```

### 7.4 리포트 다운로드

```
GET /sites/{siteId}/reports/{reportId}/download
```

**Response (200 OK):**
```
Content-Type: application/pdf
Content-Disposition: attachment; filename="seo-report-2026-02.pdf"
Content-Length: 245760

[PDF binary data]
```

---

## 8. 대시보드 API

### 8.1 종합 대시보드

```
GET /dashboard
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "totalSites": 3,
    "avgSeoScore": 75.3,
    "avgSeoScoreChange": 2.1,
    "totalKeywords": 35,
    "keywordsInTop10": 12,
    "totalIssues": 45,
    "criticalIssues": 3,
    "sites": [
      {
        "siteId": 1,
        "name": "내 블로그",
        "url": "https://example.com",
        "seoScore": 78.5,
        "scoreChange": 3.2,
        "keywordsTracked": 12,
        "keywordsInTop10": 5,
        "issuesCount": 23,
        "lastCrawledAt": "2026-03-15T06:00:00Z"
      }
    ],
    "recentActivity": [
      {
        "type": "CRAWL_COMPLETED",
        "siteId": 1,
        "siteName": "내 블로그",
        "message": "크롤링 완료: 142페이지 분석",
        "timestamp": "2026-03-15T06:00:45Z"
      },
      {
        "type": "RANK_UP",
        "siteId": 1,
        "siteName": "내 블로그",
        "message": "'SEO 최적화' 키워드 순위 상승: 7위 → 5위",
        "timestamp": "2026-03-16T06:05:00Z"
      }
    ]
  }
}
```

### 8.2 사이트별 대시보드

```
GET /dashboard/sites/{siteId}
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "siteId": 1,
    "name": "내 블로그",
    "url": "https://example.com",
    "overview": {
      "seoScore": 78.5,
      "scoreChange7d": 1.5,
      "scoreChange30d": 3.2,
      "totalPages": 142,
      "healthyPages": 119,
      "issuePages": 23,
      "avgResponseTimeMs": 450
    },
    "scoreHistory": [
      {"date": "2026-03-16", "score": 78.5},
      {"date": "2026-03-09", "score": 77.0},
      {"date": "2026-03-02", "score": 76.2},
      {"date": "2026-02-23", "score": 75.3}
    ],
    "issuesSummary": {
      "critical": [
        {"type": "BROKEN_LINK", "count": 3, "description": "깨진 링크"},
        {"type": "MISSING_TITLE", "count": 1, "description": "타이틀 태그 누락"}
      ],
      "warning": [
        {"type": "MISSING_ALT", "count": 15, "description": "이미지 alt 속성 누락"},
        {"type": "META_DESC_TOO_SHORT", "count": 8, "description": "메타 디스크립션 너무 짧음"}
      ],
      "info": [
        {"type": "MISSING_STRUCTURED_DATA", "count": 42, "description": "구조화된 데이터 없음"}
      ]
    },
    "topKeywords": [
      {"keyword": "SEO 최적화", "rank": 5, "change": 2, "trend": "IMPROVING"},
      {"keyword": "사이트 분석", "rank": 12, "change": -1, "trend": "DECLINING"},
      {"keyword": "웹 성능", "rank": 8, "change": 0, "trend": "STABLE"}
    ],
    "improvementPriority": [
      {
        "rank": 1,
        "category": "LINKS",
        "action": "깨진 링크 3건 수정",
        "impact": "HIGH",
        "estimatedScoreGain": 2.5
      },
      {
        "rank": 2,
        "category": "IMAGES",
        "action": "이미지 alt 태그 15건 추가",
        "impact": "MEDIUM",
        "estimatedScoreGain": 1.8
      },
      {
        "rank": 3,
        "category": "META",
        "action": "메타 디스크립션 8건 작성/수정",
        "impact": "MEDIUM",
        "estimatedScoreGain": 1.5
      }
    ]
  }
}
```

### 8.3 사이트 통계 (차트 데이터)

```
GET /dashboard/sites/{siteId}/stats?period=30d
```

**Query Parameters:**
| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| `period` | String | 30d | 조회 기간 (7d, 30d, 90d, 180d, 1y) |
| `metrics` | String | all | 조회 메트릭 (쉼표 구분: score, keywords, issues, performance) |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "siteId": 1,
    "period": "30d",
    "scoreTimeline": [
      {"date": "2026-02-15", "score": 72.0},
      {"date": "2026-02-22", "score": 74.5},
      {"date": "2026-03-01", "score": 75.3},
      {"date": "2026-03-08", "score": 77.0},
      {"date": "2026-03-15", "score": 78.5}
    ],
    "keywordRankingDistribution": {
      "top3": 2,
      "top10": 5,
      "top30": 8,
      "top100": 11,
      "notRanked": 1
    },
    "issuesTrend": [
      {"date": "2026-02-15", "critical": 5, "warning": 25, "info": 50},
      {"date": "2026-03-15", "critical": 3, "warning": 18, "info": 42}
    ],
    "performanceTrend": [
      {"date": "2026-02-15", "avgResponseTimeMs": 520, "avgPageSizeKb": 1250},
      {"date": "2026-03-15", "avgResponseTimeMs": 450, "avgPageSizeKb": 1180}
    ],
    "topImprovements": [
      {"metric": "SEO 점수", "change": "+6.5점", "period": "30일"},
      {"metric": "평균 응답 시간", "change": "-70ms", "period": "30일"},
      {"metric": "깨진 링크", "change": "-2건", "period": "30일"}
    ]
  }
}
```

---

## 9. 알림 API

### 9.1 알림 목록 조회

```
GET /notifications?page=0&size=20&sort=createdAt,desc
```

**Query Parameters:**
| 파라미터 | 타입 | 설명 |
|----------|------|------|
| `isRead` | Boolean | 읽음 상태 필터 |
| `type` | String | 알림 유형 필터 |
| `severity` | String | 심각도 필터 |

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 101,
        "type": "RANK_UP",
        "title": "키워드 순위 상승",
        "message": "'SEO 최적화' 키워드가 7위에서 5위로 상승했습니다",
        "referenceType": "KEYWORD",
        "referenceId": 1,
        "severity": "INFO",
        "isRead": false,
        "createdAt": "2026-03-16T06:05:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 15,
    "totalPages": 1,
    "first": true,
    "last": true
  }
}
```

### 9.2 읽지 않은 알림 수 조회

```
GET /notifications/unread-count
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "unreadCount": 5,
    "bySeverity": {
      "CRITICAL": 1,
      "WARNING": 2,
      "INFO": 2
    }
  }
}
```

### 9.3 알림 읽음 처리

```
PATCH /notifications/{notificationId}/read
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "id": 101,
    "isRead": true,
    "readAt": "2026-03-16T12:00:00Z"
  }
}
```

### 9.4 알림 일괄 읽음 처리

```
PATCH /notifications/read-all
```

**Response (200 OK):**
```json
{
  "success": true,
  "data": {
    "updatedCount": 5
  },
  "message": "5건의 알림을 읽음 처리했습니다"
}
```

---

## 10. WebSocket API

### 10.1 실시간 알림 연결

```
WS /ws/notifications
```

**연결 시 헤더:**
```
Authorization: Bearer {access_token}
```

**수신 메시지 형식:**
```json
{
  "type": "NOTIFICATION",
  "data": {
    "id": 102,
    "type": "CRAWL_COMPLETED",
    "title": "크롤링 완료",
    "message": "example.com 크롤링이 완료되었습니다 (142페이지)",
    "severity": "INFO",
    "referenceType": "CRAWL_JOB",
    "referenceId": 42,
    "createdAt": "2026-03-16T12:01:00Z"
  }
}
```

### 10.2 크롤링 진행 상황 구독

```
WS /ws/crawl/{jobId}/progress
```

**수신 메시지 형식:**
```json
{
  "type": "CRAWL_PROGRESS",
  "data": {
    "jobId": 42,
    "status": "RUNNING",
    "pagesProcessed": 67,
    "totalEstimated": 100,
    "percentage": 67,
    "currentUrl": "https://example.com/blog/post-45",
    "elapsedTimeMs": 23000
  }
}
```

---

## 11. API 엔드포인트 요약표

| 메서드 | URL | 인증 | 설명 |
|--------|-----|------|------|
| **인증** | | | |
| POST | `/auth/signup` | X | 회원가입 |
| POST | `/auth/login` | X | 로그인 |
| POST | `/auth/refresh` | Cookie | 토큰 갱신 |
| POST | `/auth/logout` | O | 로그아웃 |
| GET | `/auth/me` | O | 내 정보 조회 |
| **사이트** | | | |
| POST | `/sites` | O | 사이트 등록 |
| GET | `/sites` | O | 사이트 목록 |
| GET | `/sites/{siteId}` | O | 사이트 상세 |
| PUT | `/sites/{siteId}` | O | 사이트 수정 |
| DELETE | `/sites/{siteId}` | O | 사이트 삭제 |
| **크롤링** | | | |
| POST | `/sites/{siteId}/crawl` | O | 크롤링 시작 |
| GET | `/sites/{siteId}/crawl/jobs` | O | 크롤링 작업 목록 |
| GET | `/sites/{siteId}/crawl/jobs/{jobId}` | O | 크롤링 상태 조회 |
| POST | `/sites/{siteId}/crawl/jobs/{jobId}/cancel` | O | 크롤링 취소 |
| GET | `/sites/{siteId}/crawl/jobs/{jobId}/results` | O | 크롤링 결과 목록 |
| GET | `/sites/{siteId}/crawl/results/{resultId}/analysis` | O | 페이지 분석 상세 |
| **키워드** | | | |
| POST | `/sites/{siteId}/keywords` | O | 키워드 등록 |
| POST | `/sites/{siteId}/keywords/batch` | O | 키워드 일괄 등록 |
| GET | `/sites/{siteId}/keywords` | O | 키워드 목록 |
| PUT | `/sites/{siteId}/keywords/{keywordId}` | O | 키워드 수정 |
| DELETE | `/sites/{siteId}/keywords/{keywordId}` | O | 키워드 삭제 |
| GET | `/sites/{siteId}/keywords/{keywordId}/rankings` | O | 순위 이력 |
| GET | `/sites/{siteId}/keywords/{keywordId}/competitors` | O | 경쟁사 비교 |
| **AI 분석** | | | |
| POST | `/analysis/content` | O | 콘텐츠 분석 요청 |
| GET | `/analysis/content/{analysisId}` | O | 분석 결과 조회 |
| GET | `/analysis/content` | O | 분석 목록 |
| POST | `/analysis/meta-generate` | O | 메타 태그 생성 |
| **리포트** | | | |
| POST | `/sites/{siteId}/reports` | O | 리포트 생성 요청 |
| GET | `/sites/{siteId}/reports` | O | 리포트 목록 |
| GET | `/sites/{siteId}/reports/{reportId}` | O | 리포트 상세 |
| GET | `/sites/{siteId}/reports/{reportId}/download` | O | 리포트 다운로드 |
| **대시보드** | | | |
| GET | `/dashboard` | O | 종합 대시보드 |
| GET | `/dashboard/sites/{siteId}` | O | 사이트별 대시보드 |
| GET | `/dashboard/sites/{siteId}/stats` | O | 사이트 통계 |
| **알림** | | | |
| GET | `/notifications` | O | 알림 목록 |
| GET | `/notifications/unread-count` | O | 읽지 않은 수 |
| PATCH | `/notifications/{notificationId}/read` | O | 읽음 처리 |
| PATCH | `/notifications/read-all` | O | 일괄 읽음 |
| **WebSocket** | | | |
| WS | `/ws/notifications` | O | 실시간 알림 |
| WS | `/ws/crawl/{jobId}/progress` | O | 크롤링 진행 상황 |
