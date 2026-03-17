# SEO 올인원 대시보드 - 데이터베이스 스키마

## 1. ERD (Entity Relationship Diagram)

```
+------------------+       +------------------+       +------------------+
|      users       |       |      sites       |       |    crawl_jobs    |
+------------------+       +------------------+       +------------------+
| PK id            |<──┐   | PK id            |<──┐   | PK id            |
|    email         |   │   | FK user_id ──────>│   │   | FK site_id ──────>│
|    password_hash  |   │   |    url           |   │   |    status        |
|    name          |   │   |    name          |   │   |    started_at    |
|    role          |   │   |    seo_score     |   │   |    completed_at  |
|    created_at    |   │   |    last_crawled  |   │   |    total_pages   |
|    updated_at    |   │   |    created_at    |   │   |    created_at    |
+------------------+   │   +------------------+   │   +------------------+
                       │           │               │           │
                       │           │               │           │
                       │   +-------+-------+       │   +-------+--------+
                       │   |               |       │   |                |
                       │   v               v       │   v                v
              +------------------+  +------------------+  +------------------+
              |    keywords      |  |  crawl_results   |  | page_analyses    |
              +------------------+  +------------------+  +------------------+
              | PK id            |  | PK id            |  | PK id            |
              | FK site_id ─────>|  | FK crawl_job_id ─>|  | FK crawl_result_id>│
              |    keyword       |  |    url           |  |    seo_score     |
              |    target_url    |  |    status_code   |  |    title_score   |
              |    is_active     |  |    response_time |  |    meta_score    |
              |    created_at    |  |    content_length|  |    heading_score |
              +------------------+  |    created_at    |  |    image_score   |
                       │            +------------------+  |    link_score    |
                       │                                  |    performance   |
                       v                                  |    issues_json   |
              +------------------+                        |    created_at    |
              | keyword_rankings |                        +------------------+
              | (hypertable)     |
              +------------------+
              | FK keyword_id ──>│
              |    rank          |
              |    url           |
              |    search_volume |
              |    recorded_at   | ← TimescaleDB 시간 축
              +------------------+

              +------------------+       +------------------+
              |content_analyses  |       |     reports      |
              +------------------+       +------------------+
              | PK id            |       | PK id            |
              | FK site_id ─────>│       | FK site_id ─────>│
              | FK user_id ─────>│       | FK user_id ─────>│
              |    content       |       |    type          |
              |    seo_score     |       |    period_start  |
              |    suggestions   |       |    period_end    |
              |    meta_title    |       |    file_path     |
              |    meta_desc     |       |    summary_json  |
              |    keyword_density|      |    status        |
              |    readability   |       |    created_at    |
              |    status        |       +------------------+
              |    created_at    |
              +------------------+

              +------------------+
              |  notifications   |
              +------------------+
              | PK id            |
              | FK user_id ─────>│
              |    type          |
              |    title         |
              |    message       |
              |    reference_type|
              |    reference_id  |
              |    is_read       |
              |    created_at    |
              +------------------+
```

## 2. 테이블 상세 설계

### 2.1 users (회원)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 회원 고유 ID |
| `email` | `VARCHAR(255)` | UNIQUE, NOT NULL | 로그인 이메일 |
| `password_hash` | `VARCHAR(255)` | NOT NULL | BCrypt 암호화된 비밀번호 |
| `name` | `VARCHAR(100)` | NOT NULL | 사용자 이름 |
| `role` | `VARCHAR(20)` | NOT NULL, DEFAULT 'USER' | 권한 (USER, ADMIN) |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | 계정 활성 상태 |
| `last_login_at` | `TIMESTAMPTZ` | NULL | 마지막 로그인 시각 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 가입일 |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 수정일 |

**인덱스:**
```sql
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_is_active ON users(is_active);
```

### 2.2 sites (등록 사이트)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 사이트 고유 ID |
| `user_id` | `BIGINT` | FK → users(id), NOT NULL | 소유자 |
| `url` | `VARCHAR(2048)` | NOT NULL | 사이트 URL |
| `name` | `VARCHAR(255)` | NOT NULL | 사이트 이름 |
| `description` | `TEXT` | NULL | 사이트 설명 |
| `seo_score` | `DECIMAL(5,2)` | NULL | 종합 SEO 점수 (0~100) |
| `last_crawled_at` | `TIMESTAMPTZ` | NULL | 마지막 크롤링 시각 |
| `crawl_interval_hours` | `INTEGER` | NOT NULL, DEFAULT 168 | 자동 크롤링 주기 (시간, 기본 7일) |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | 활성 상태 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 등록일 |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 수정일 |

**인덱스:**
```sql
CREATE INDEX idx_sites_user_id ON sites(user_id);
CREATE INDEX idx_sites_is_active ON sites(is_active);
CREATE UNIQUE INDEX idx_sites_user_url ON sites(user_id, url);
```

### 2.3 crawl_jobs (크롤링 작업)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 작업 고유 ID |
| `site_id` | `BIGINT` | FK → sites(id), NOT NULL | 대상 사이트 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'PENDING' | 상태 (PENDING, RUNNING, COMPLETED, FAILED, CANCELLED) - Java enum `CrawlJobStatus`로 매핑 (`@Enumerated(STRING)`) |
| `trigger_type` | `VARCHAR(20)` | NOT NULL | 트리거 유형 (MANUAL, SCHEDULED) |
| `max_pages` | `INTEGER` | NOT NULL, DEFAULT 100 | 최대 크롤링 페이지 수 |
| `max_depth` | `INTEGER` | NOT NULL, DEFAULT 3 | 최대 크롤링 깊이 |
| `total_pages` | `INTEGER` | NULL | 실제 크롤링된 페이지 수 |
| `error_count` | `INTEGER` | DEFAULT 0 | 에러 발생 수 |
| `error_message` | `TEXT` | NULL | 실패 시 에러 메시지 |
| `started_at` | `TIMESTAMPTZ` | NULL | 시작 시각 |
| `completed_at` | `TIMESTAMPTZ` | NULL | 완료 시각 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 생성일 |

**인덱스:**
```sql
CREATE INDEX idx_crawl_jobs_site_id ON crawl_jobs(site_id);
CREATE INDEX idx_crawl_jobs_status ON crawl_jobs(status);
CREATE INDEX idx_crawl_jobs_created_at ON crawl_jobs(created_at DESC);
CREATE INDEX idx_crawl_jobs_site_status ON crawl_jobs(site_id, status);
```

### 2.4 crawl_results (크롤링 결과)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 결과 고유 ID |
| `crawl_job_id` | `BIGINT` | FK → crawl_jobs(id), NOT NULL | 소속 크롤링 작업 |
| `url` | `VARCHAR(2048)` | NOT NULL | 페이지 URL |
| `status_code` | `INTEGER` | NULL | HTTP 상태 코드 |
| `content_type` | `VARCHAR(100)` | NULL | 응답 Content-Type |
| `content_length` | `BIGINT` | NULL | 응답 본문 크기 (bytes) |
| `response_time_ms` | `INTEGER` | NULL | 응답 시간 (ms) |
| `title` | `VARCHAR(512)` | NULL | 페이지 타이틀 |
| `meta_description` | `TEXT` | NULL | 메타 디스크립션 |
| `canonical_url` | `VARCHAR(2048)` | NULL | canonical URL |
| `depth` | `INTEGER` | NOT NULL | 크롤링 깊이 (시작=0) |
| `is_internal` | `BOOLEAN` | NOT NULL | 내부 페이지 여부 |
| `redirect_url` | `VARCHAR(2048)` | NULL | 리다이렉트 대상 URL |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 수집일 |

**인덱스:**
```sql
CREATE INDEX idx_crawl_results_job_id ON crawl_results(crawl_job_id);
CREATE INDEX idx_crawl_results_url ON crawl_results(url);
CREATE INDEX idx_crawl_results_status_code ON crawl_results(status_code);
CREATE INDEX idx_crawl_results_job_internal ON crawl_results(crawl_job_id, is_internal);
```

### 2.5 page_analyses (페이지별 SEO 분석)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 분석 고유 ID |
| `crawl_result_id` | `BIGINT` | FK → crawl_results(id), UNIQUE, NOT NULL | 대상 크롤링 결과 (1:1) |
| `seo_score` | `DECIMAL(5,2)` | NOT NULL | 종합 SEO 점수 (0~100) |
| `title_score` | `DECIMAL(5,2)` | NOT NULL | 타이틀 태그 점수 |
| `title_length` | `INTEGER` | NULL | 타이틀 길이 |
| `meta_description_score` | `DECIMAL(5,2)` | NOT NULL | 메타 디스크립션 점수 |
| `meta_description_length` | `INTEGER` | NULL | 메타 디스크립션 길이 |
| `heading_score` | `DECIMAL(5,2)` | NOT NULL | 헤딩 구조 점수 |
| `heading_structure` | `JSONB` | NULL | 헤딩 구조 상세 {h1: n, h2: n, ...} |
| `image_score` | `DECIMAL(5,2)` | NOT NULL | 이미지 최적화 점수 |
| `images_total` | `INTEGER` | DEFAULT 0 | 전체 이미지 수 |
| `images_without_alt` | `INTEGER` | DEFAULT 0 | alt 없는 이미지 수 |
| `link_score` | `DECIMAL(5,2)` | NOT NULL | 링크 상태 점수 |
| `internal_links_count` | `INTEGER` | DEFAULT 0 | 내부 링크 수 |
| `external_links_count` | `INTEGER` | DEFAULT 0 | 외부 링크 수 |
| `broken_links_count` | `INTEGER` | DEFAULT 0 | 깨진 링크 수 |
| `performance_score` | `DECIMAL(5,2)` | NOT NULL | 성능 점수 |
| `has_og_tags` | `BOOLEAN` | DEFAULT FALSE | Open Graph 태그 유무 |
| `has_twitter_cards` | `BOOLEAN` | DEFAULT FALSE | Twitter Card 유무 |
| `has_structured_data` | `BOOLEAN` | DEFAULT FALSE | 구조화된 데이터 유무 |
| `has_sitemap` | `BOOLEAN` | NULL | sitemap.xml 존재 여부 (루트 페이지만) |
| `has_robots_txt` | `BOOLEAN` | NULL | robots.txt 존재 여부 (루트 페이지만) |
| `is_mobile_friendly` | `BOOLEAN` | NULL | 모바일 친화성 |
| `issues` | `JSONB` | NOT NULL, DEFAULT '[]' | 발견된 이슈 목록 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 분석일 |

`issues` JSONB 구조 예시:
```json
[
  {
    "type": "MISSING_ALT",
    "severity": "WARNING",
    "message": "3개 이미지에 alt 속성이 없습니다",
    "details": {"urls": ["img1.png", "img2.png", "img3.png"]}
  },
  {
    "type": "TITLE_TOO_LONG",
    "severity": "INFO",
    "message": "타이틀이 60자를 초과합니다 (현재 78자)",
    "details": {"current_length": 78, "recommended_max": 60}
  }
]
```

**인덱스:**
```sql
CREATE UNIQUE INDEX idx_page_analyses_crawl_result ON page_analyses(crawl_result_id);
CREATE INDEX idx_page_analyses_seo_score ON page_analyses(seo_score);
CREATE INDEX idx_page_analyses_issues ON page_analyses USING GIN(issues);
```

### 2.6 keywords (등록 키워드)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 키워드 고유 ID |
| `site_id` | `BIGINT` | FK → sites(id), NOT NULL | 대상 사이트 |
| `keyword` | `VARCHAR(500)` | NOT NULL | 트래킹 키워드 |
| `target_url` | `VARCHAR(2048)` | NULL | 목표 랭킹 URL |
| `search_engine` | `VARCHAR(20)` | NOT NULL, DEFAULT 'GOOGLE' | 검색 엔진 |
| `country_code` | `VARCHAR(5)` | NOT NULL, DEFAULT 'KR' | 국가 코드 |
| `language_code` | `VARCHAR(5)` | NOT NULL, DEFAULT 'ko' | 언어 코드 |
| `is_active` | `BOOLEAN` | NOT NULL, DEFAULT TRUE | 트래킹 활성 상태 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 등록일 |
| `updated_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 수정일 |

**인덱스:**
```sql
CREATE INDEX idx_keywords_site_id ON keywords(site_id);
CREATE INDEX idx_keywords_is_active ON keywords(is_active);
CREATE UNIQUE INDEX idx_keywords_site_keyword_engine ON keywords(site_id, keyword, search_engine, country_code);
```

### 2.7 keyword_rankings (키워드 순위 이력 - TimescaleDB Hypertable)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `keyword_id` | `BIGINT` | FK → keywords(id), NOT NULL | 대상 키워드 |
| `recorded_at` | `TIMESTAMPTZ` | NOT NULL | 수집 시각 (TimescaleDB 시간 축) |
| `rank` | `INTEGER` | NULL | 검색 순위 (NULL = 100위 밖) |
| `url` | `VARCHAR(2048)` | NULL | 순위에 노출된 실제 URL |
| `search_volume` | `INTEGER` | NULL | 추정 월간 검색량 |
| `previous_rank` | `INTEGER` | NULL | 이전 수집 시 순위 |
| `rank_change` | `INTEGER` | NULL | 순위 변동 (양수=상승, 음수=하락) |
| `serp_features` | `JSONB` | NULL | SERP 특수 표시 (featured snippet, PAA 등) |

**주의**: PK를 별도로 두지 않고 `(keyword_id, recorded_at)` 복합키를 사용한다.

**TimescaleDB 설정:**
```sql
-- 일반 테이블로 생성 후 hypertable로 변환
CREATE TABLE keyword_rankings (
    keyword_id BIGINT NOT NULL REFERENCES keywords(id),
    recorded_at TIMESTAMPTZ NOT NULL,
    rank INTEGER,
    url VARCHAR(2048),
    search_volume INTEGER,
    previous_rank INTEGER,
    rank_change INTEGER,
    serp_features JSONB
);

-- hypertable 변환 (7일 단위 청크)
SELECT create_hypertable('keyword_rankings', 'recorded_at',
    chunk_time_interval => INTERVAL '7 days'
);

-- 데이터 보존 정책: 1년 이후 자동 삭제
SELECT add_retention_policy('keyword_rankings', INTERVAL '1 year');

-- 연속 집계 뷰: 주간 평균 순위
CREATE MATERIALIZED VIEW keyword_rankings_weekly
WITH (timescaledb.continuous) AS
SELECT
    keyword_id,
    time_bucket('7 days', recorded_at) AS week,
    AVG(rank)::DECIMAL(5,1) AS avg_rank,
    MIN(rank) AS best_rank,
    MAX(rank) AS worst_rank,
    COUNT(*) AS data_points
FROM keyword_rankings
GROUP BY keyword_id, time_bucket('7 days', recorded_at);

-- 연속 집계 자동 갱신 정책
SELECT add_continuous_aggregate_policy('keyword_rankings_weekly',
    start_offset => INTERVAL '1 month',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);

-- 월간 집계 뷰
CREATE MATERIALIZED VIEW keyword_rankings_monthly
WITH (timescaledb.continuous) AS
SELECT
    keyword_id,
    time_bucket('30 days', recorded_at) AS month,
    AVG(rank)::DECIMAL(5,1) AS avg_rank,
    MIN(rank) AS best_rank,
    MAX(rank) AS worst_rank,
    COUNT(*) AS data_points
FROM keyword_rankings
GROUP BY keyword_id, time_bucket('30 days', recorded_at);

SELECT add_continuous_aggregate_policy('keyword_rankings_monthly',
    start_offset => INTERVAL '3 months',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);
```

**인덱스:**
```sql
-- TimescaleDB가 recorded_at에 대한 인덱스를 자동 생성
-- 추가 인덱스:
CREATE INDEX idx_keyword_rankings_keyword_id ON keyword_rankings(keyword_id, recorded_at DESC);
CREATE INDEX idx_keyword_rankings_rank ON keyword_rankings(rank) WHERE rank IS NOT NULL;
```

### 2.8 content_analyses (AI 콘텐츠 분석)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 분석 고유 ID |
| `site_id` | `BIGINT` | FK → sites(id), NULL | 연관 사이트 (선택) |
| `user_id` | `BIGINT` | FK → users(id), NOT NULL | 요청 사용자 |
| `title` | `VARCHAR(500)` | NULL | 분석 대상 콘텐츠 제목 |
| `content` | `TEXT` | NOT NULL | 분석 대상 콘텐츠 원문 |
| `target_keywords` | `VARCHAR(1000)` | NULL | 타겟 키워드 (쉼표 구분) |
| `seo_score` | `DECIMAL(5,2)` | NULL | 종합 SEO 점수 (0~100) |
| `readability_score` | `DECIMAL(5,2)` | NULL | 가독성 점수 (0~100) |
| `keyword_density` | `JSONB` | NULL | 키워드별 밀도 분석 |
| `structure_analysis` | `JSONB` | NULL | 구조 분석 결과 |
| `suggestions` | `JSONB` | NULL | AI 개선 제안 목록 |
| `generated_meta_title` | `VARCHAR(200)` | NULL | AI 생성 메타 타이틀 |
| `generated_meta_description` | `VARCHAR(500)` | NULL | AI 생성 메타 디스크립션 |
| `ai_provider` | `VARCHAR(20)` | NULL | 사용된 AI 제공자 (OPENAI, CLAUDE) |
| `ai_model` | `VARCHAR(50)` | NULL | 사용된 AI 모델 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'PENDING' | 상태 (PENDING, ANALYZING, PROCESSING, COMPLETED, FAILED) - Java enum `AnalysisStatus`로 매핑 (`@Enumerated(STRING)`) |
| `error_message` | `TEXT` | NULL | 실패 시 에러 메시지 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 요청일 |
| `completed_at` | `TIMESTAMPTZ` | NULL | 완료일 |

`keyword_density` JSONB 구조 예시:
```json
{
  "SEO": {"count": 15, "density": 2.3, "status": "OPTIMAL"},
  "검색엔진": {"count": 8, "density": 1.2, "status": "LOW"},
  "최적화": {"count": 22, "density": 3.4, "status": "HIGH"}
}
```

`suggestions` JSONB 구조 예시:
```json
[
  {
    "category": "KEYWORD",
    "priority": "HIGH",
    "message": "'검색엔진' 키워드 사용 빈도를 높여주세요",
    "current_value": "1.2%",
    "recommended_value": "2.0~3.0%"
  },
  {
    "category": "STRUCTURE",
    "priority": "MEDIUM",
    "message": "H2 소제목을 추가하여 콘텐츠를 세분화하세요"
  }
]
```

**인덱스:**
```sql
CREATE INDEX idx_content_analyses_user_id ON content_analyses(user_id);
CREATE INDEX idx_content_analyses_site_id ON content_analyses(site_id);
CREATE INDEX idx_content_analyses_status ON content_analyses(status);
CREATE INDEX idx_content_analyses_created_at ON content_analyses(created_at DESC);
```

### 2.9 reports (리포트)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 리포트 고유 ID |
| `site_id` | `BIGINT` | FK → sites(id), NOT NULL | 대상 사이트 |
| `user_id` | `BIGINT` | FK → users(id), NOT NULL | 소유자 |
| `type` | `VARCHAR(20)` | NOT NULL | 유형 (WEEKLY, MONTHLY, CUSTOM) |
| `title` | `VARCHAR(255)` | NOT NULL | 리포트 제목 |
| `period_start` | `DATE` | NOT NULL | 분석 시작일 |
| `period_end` | `DATE` | NOT NULL | 분석 종료일 |
| `summary` | `JSONB` | NULL | 요약 데이터 |
| `file_path` | `VARCHAR(1024)` | NULL | MinIO 내 파일 경로 |
| `file_size` | `BIGINT` | NULL | 파일 크기 (bytes) |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'PENDING' | 상태 (PENDING, GENERATING, COMPLETED, FAILED) - Java enum `ReportStatus`로 매핑 (`@Enumerated(STRING)`) |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 생성일 |

`summary` JSONB 구조 예시:
```json
{
  "overall_score": 78.5,
  "score_change": 3.2,
  "pages_crawled": 142,
  "issues_found": 23,
  "issues_resolved": 8,
  "top_keywords": [
    {"keyword": "SEO 도구", "avg_rank": 5.2, "change": 2},
    {"keyword": "사이트 분석", "avg_rank": 12.8, "change": -1}
  ],
  "priority_actions": [
    "깨진 링크 12건 수정",
    "이미지 alt 태그 15건 추가",
    "메타 디스크립션 누락 8건 작성"
  ]
}
```

**인덱스:**
```sql
CREATE INDEX idx_reports_site_id ON reports(site_id);
CREATE INDEX idx_reports_user_id ON reports(user_id);
CREATE INDEX idx_reports_type ON reports(type);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);
CREATE INDEX idx_reports_site_period ON reports(site_id, period_start, period_end);
```

### 2.10 notifications (알림)

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 알림 고유 ID |
| `user_id` | `BIGINT` | FK → users(id), NOT NULL | 대상 사용자 |
| `type` | `VARCHAR(30)` | NOT NULL | 알림 유형 |
| `title` | `VARCHAR(255)` | NOT NULL | 알림 제목 |
| `message` | `TEXT` | NOT NULL | 알림 본문 |
| `reference_type` | `VARCHAR(30)` | NULL | 참조 엔티티 유형 (CRAWL_JOB, KEYWORD, REPORT 등) |
| `reference_id` | `BIGINT` | NULL | 참조 엔티티 ID |
| `severity` | `VARCHAR(20)` | NOT NULL, DEFAULT 'INFO' | 심각도 (INFO, WARNING, CRITICAL) |
| `is_read` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | 읽음 여부 |
| `read_at` | `TIMESTAMPTZ` | NULL | 읽은 시각 |
| `created_at` | `TIMESTAMPTZ` | NOT NULL, DEFAULT NOW() | 생성일 |

**알림 유형 (`type`):**
- `CRAWL_COMPLETED` - 크롤링 완료
- `CRAWL_FAILED` - 크롤링 실패
- `RANK_UP` - 키워드 순위 상승
- `RANK_DOWN` - 키워드 순위 하락
- `RANK_LOST` - 키워드 순위 이탈 (100위 밖)
- `REPORT_READY` - 리포트 생성 완료
- `SCORE_DROP` - SEO 점수 급락
- `ANALYSIS_COMPLETED` - AI 분석 완료

**인덱스:**
```sql
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_type ON notifications(type);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON notifications(reference_type, reference_id);
```

### 2.11 public_analyses (공개 SEO 분석)

> V6 마이그레이션(`V6__add_public_analyses.sql`)으로 추가된 테이블. 인증 없이 사용 가능한 공개 SEO 분석 결과를 저장한다.

| 컬럼 | 타입 | 제약조건 | 설명 |
|------|------|---------|------|
| `id` | `BIGSERIAL` | PK | 분석 고유 ID |
| `url` | `VARCHAR(2048)` | NOT NULL | 분석 대상 URL |
| `domain` | `VARCHAR(500)` | NOT NULL | 도메인 (추출값) |
| `seo_score` | `NUMERIC(5,2)` | NULL | 종합 SEO 점수 (0~100) |
| `title_score` | `NUMERIC(5,2)` | NULL | 타이틀 태그 점수 |
| `meta_description_score` | `NUMERIC(5,2)` | NULL | 메타 디스크립션 점수 |
| `heading_score` | `NUMERIC(5,2)` | NULL | 헤딩 구조 점수 |
| `image_score` | `NUMERIC(5,2)` | NULL | 이미지 최적화 점수 |
| `link_score` | `NUMERIC(5,2)` | NULL | 링크 상태 점수 |
| `performance_score` | `NUMERIC(5,2)` | NULL | 성능 점수 |
| `title` | `VARCHAR(1000)` | NULL | 페이지 타이틀 |
| `meta_description` | `TEXT` | NULL | 메타 디스크립션 |
| `canonical_url` | `VARCHAR(2048)` | NULL | canonical URL |
| `response_time_ms` | `INTEGER` | NULL | 응답 시간 (ms) |
| `content_length` | `INTEGER` | NULL | 콘텐츠 길이 (bytes) |
| `total_images` | `INTEGER` | NOT NULL, DEFAULT 0 | 전체 이미지 수 |
| `images_without_alt` | `INTEGER` | NOT NULL, DEFAULT 0 | alt 없는 이미지 수 |
| `internal_links` | `INTEGER` | NOT NULL, DEFAULT 0 | 내부 링크 수 |
| `external_links` | `INTEGER` | NOT NULL, DEFAULT 0 | 외부 링크 수 |
| `broken_links` | `INTEGER` | NOT NULL, DEFAULT 0 | 깨진 링크 수 |
| `total_headings` | `INTEGER` | NOT NULL, DEFAULT 0 | 전체 헤딩 수 |
| `has_og_tags` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | Open Graph 태그 유무 |
| `has_twitter_cards` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | Twitter Card 유무 |
| `has_viewport` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | viewport 메타태그 유무 |
| `has_favicon` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | favicon 유무 |
| `has_robots_txt` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | robots.txt 유무 |
| `has_sitemap` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | sitemap.xml 유무 |
| `has_https` | `BOOLEAN` | NOT NULL, DEFAULT FALSE | HTTPS 사용 여부 |
| `heading_structure` | `JSONB` | NULL | 헤딩 구조 상세 |
| `issues` | `JSONB` | NULL | 발견된 이슈 목록 |
| `link_list` | `JSONB` | NULL | 링크 목록 |
| `meta_tags` | `JSONB` | NULL | 메타 태그 정보 |
| `status` | `VARCHAR(20)` | NOT NULL, DEFAULT 'PENDING' | 상태 - Java enum `AnalysisStatus`로 매핑 (`@Enumerated(STRING)`) |
| `error_message` | `VARCHAR(2000)` | NULL | 실패 시 에러 메시지 |
| `created_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | 생성일 |
| `updated_at` | `TIMESTAMP` | NOT NULL, DEFAULT NOW() | 수정일 |

**인덱스:**
```sql
CREATE INDEX idx_public_analyses_domain ON public_analyses(domain);
CREATE INDEX idx_public_analyses_seo_score ON public_analyses(seo_score DESC);
CREATE INDEX idx_public_analyses_created_at ON public_analyses(created_at DESC);
CREATE INDEX idx_public_analyses_status ON public_analyses(status);
```

**참고**: `public_analyses`는 `users`나 `sites` 테이블과 FK 관계가 없으며, 인증 없는 공개 분석 결과를 독립적으로 저장한다.

### Status 필드 Enum 매핑 참고

모든 status 관련 컬럼은 DB에 `VARCHAR`로 저장되며, 애플리케이션 레벨에서 Java enum과 `@Enumerated(EnumType.STRING)` 어노테이션을 통해 매핑한다. DB ENUM 타입을 사용하지 않는 이유는 다음과 같다:

- DB ENUM 변경 시 `ALTER TYPE`이 필요하여 마이그레이션이 복잡해짐
- JPA와의 매핑에서 타입 불일치 문제 발생 가능
- VARCHAR + 애플리케이션 검증 방식이 유연성과 호환성이 높음

| 테이블 | Java Enum | 값 |
|--------|-----------|-----|
| `crawl_jobs` | `CrawlJobStatus` | PENDING, RUNNING, COMPLETED, FAILED, CANCELLED |
| `content_analyses` | `AnalysisStatus` | PENDING, ANALYZING, PROCESSING, COMPLETED, FAILED |
| `public_analyses` | `AnalysisStatus` | PENDING, ANALYZING, PROCESSING, COMPLETED, FAILED |
| `reports` | `ReportStatus` | PENDING, GENERATING, COMPLETED, FAILED |

## 3. 테이블 관계 요약

| 관계 | 유형 | 설명 |
|------|------|------|
| users → sites | 1:N | 사용자는 여러 사이트 등록 가능 |
| sites → crawl_jobs | 1:N | 사이트당 여러 크롤링 작업 |
| crawl_jobs → crawl_results | 1:N | 크롤링 작업당 여러 페이지 결과 |
| crawl_results → page_analyses | 1:1 | 크롤링 결과와 SEO 분석은 1:1 |
| sites → keywords | 1:N | 사이트당 여러 키워드 등록 |
| keywords → keyword_rankings | 1:N | 키워드당 매일 순위 기록 |
| sites → content_analyses | 1:N | 사이트당 여러 콘텐츠 분석 |
| users → content_analyses | 1:N | 사용자당 여러 콘텐츠 분석 |
| sites → reports | 1:N | 사이트당 여러 리포트 |
| users → notifications | 1:N | 사용자당 여러 알림 |
| (독립) public_analyses | - | 인증 없는 공개 분석 결과 (FK 관계 없음) |

## 4. DDL (전체 스키마 생성 스크립트)

```sql
-- TimescaleDB 확장 활성화
CREATE EXTENSION IF NOT EXISTS timescaledb;

-- ENUM 타입은 사용하지 않고 VARCHAR + 애플리케이션 레벨 Java enum 매핑을 사용한다.
-- DB에는 VARCHAR로 저장하되, JPA 엔티티에서 @Enumerated(EnumType.STRING)으로 매핑한다.
-- 이유: DB ENUM 변경 시 ALTER TYPE이 필요하고, JPA 매핑이 복잡해짐
--
-- 애플리케이션 레벨 Java enum 목록:
--   CrawlJobStatus  : PENDING, RUNNING, COMPLETED, FAILED, CANCELLED
--   AnalysisStatus   : PENDING, ANALYZING, PROCESSING, COMPLETED, FAILED
--   ReportStatus     : PENDING, GENERATING, COMPLETED, FAILED

-- ============================================
-- users
-- ============================================
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    name VARCHAR(100) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'USER',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    last_login_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX idx_users_email ON users(email);

-- ============================================
-- sites
-- ============================================
CREATE TABLE sites (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    url VARCHAR(2048) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    seo_score DECIMAL(5,2),
    last_crawled_at TIMESTAMPTZ,
    crawl_interval_hours INTEGER NOT NULL DEFAULT 168,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sites_user_id ON sites(user_id);
CREATE UNIQUE INDEX idx_sites_user_url ON sites(user_id, url);

-- ============================================
-- crawl_jobs
-- ============================================
CREATE TABLE crawl_jobs (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    trigger_type VARCHAR(20) NOT NULL,
    max_pages INTEGER NOT NULL DEFAULT 100,
    max_depth INTEGER NOT NULL DEFAULT 3,
    total_pages INTEGER,
    error_count INTEGER DEFAULT 0,
    error_message TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crawl_jobs_site_id ON crawl_jobs(site_id);
CREATE INDEX idx_crawl_jobs_status ON crawl_jobs(status);
CREATE INDEX idx_crawl_jobs_created_at ON crawl_jobs(created_at DESC);

-- ============================================
-- crawl_results
-- ============================================
CREATE TABLE crawl_results (
    id BIGSERIAL PRIMARY KEY,
    crawl_job_id BIGINT NOT NULL REFERENCES crawl_jobs(id) ON DELETE CASCADE,
    url VARCHAR(2048) NOT NULL,
    status_code INTEGER,
    content_type VARCHAR(100),
    content_length BIGINT,
    response_time_ms INTEGER,
    title VARCHAR(512),
    meta_description TEXT,
    canonical_url VARCHAR(2048),
    depth INTEGER NOT NULL,
    is_internal BOOLEAN NOT NULL,
    redirect_url VARCHAR(2048),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_crawl_results_job_id ON crawl_results(crawl_job_id);
CREATE INDEX idx_crawl_results_status_code ON crawl_results(status_code);

-- ============================================
-- page_analyses
-- ============================================
CREATE TABLE page_analyses (
    id BIGSERIAL PRIMARY KEY,
    crawl_result_id BIGINT NOT NULL UNIQUE REFERENCES crawl_results(id) ON DELETE CASCADE,
    seo_score DECIMAL(5,2) NOT NULL,
    title_score DECIMAL(5,2) NOT NULL,
    title_length INTEGER,
    meta_description_score DECIMAL(5,2) NOT NULL,
    meta_description_length INTEGER,
    heading_score DECIMAL(5,2) NOT NULL,
    heading_structure JSONB,
    image_score DECIMAL(5,2) NOT NULL,
    images_total INTEGER DEFAULT 0,
    images_without_alt INTEGER DEFAULT 0,
    link_score DECIMAL(5,2) NOT NULL,
    internal_links_count INTEGER DEFAULT 0,
    external_links_count INTEGER DEFAULT 0,
    broken_links_count INTEGER DEFAULT 0,
    performance_score DECIMAL(5,2) NOT NULL,
    has_og_tags BOOLEAN DEFAULT FALSE,
    has_twitter_cards BOOLEAN DEFAULT FALSE,
    has_structured_data BOOLEAN DEFAULT FALSE,
    has_sitemap BOOLEAN,
    has_robots_txt BOOLEAN,
    is_mobile_friendly BOOLEAN,
    issues JSONB NOT NULL DEFAULT '[]',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_page_analyses_seo_score ON page_analyses(seo_score);
CREATE INDEX idx_page_analyses_issues ON page_analyses USING GIN(issues);

-- ============================================
-- keywords
-- ============================================
CREATE TABLE keywords (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    keyword VARCHAR(500) NOT NULL,
    target_url VARCHAR(2048),
    search_engine VARCHAR(20) NOT NULL DEFAULT 'GOOGLE',
    country_code VARCHAR(5) NOT NULL DEFAULT 'KR',
    language_code VARCHAR(5) NOT NULL DEFAULT 'ko',
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_keywords_site_id ON keywords(site_id);
CREATE UNIQUE INDEX idx_keywords_site_keyword_engine
    ON keywords(site_id, keyword, search_engine, country_code);

-- ============================================
-- keyword_rankings (TimescaleDB Hypertable)
-- ============================================
CREATE TABLE keyword_rankings (
    keyword_id BIGINT NOT NULL REFERENCES keywords(id) ON DELETE CASCADE,
    recorded_at TIMESTAMPTZ NOT NULL,
    rank INTEGER,
    url VARCHAR(2048),
    search_volume INTEGER,
    previous_rank INTEGER,
    rank_change INTEGER,
    serp_features JSONB
);

SELECT create_hypertable('keyword_rankings', 'recorded_at',
    chunk_time_interval => INTERVAL '7 days'
);

CREATE INDEX idx_keyword_rankings_keyword_time
    ON keyword_rankings(keyword_id, recorded_at DESC);

-- 보존 정책
SELECT add_retention_policy('keyword_rankings', INTERVAL '1 year');

-- 주간 연속 집계
CREATE MATERIALIZED VIEW keyword_rankings_weekly
WITH (timescaledb.continuous) AS
SELECT
    keyword_id,
    time_bucket('7 days', recorded_at) AS week,
    AVG(rank)::DECIMAL(5,1) AS avg_rank,
    MIN(rank) AS best_rank,
    MAX(rank) AS worst_rank,
    COUNT(*) AS data_points
FROM keyword_rankings
GROUP BY keyword_id, time_bucket('7 days', recorded_at);

SELECT add_continuous_aggregate_policy('keyword_rankings_weekly',
    start_offset => INTERVAL '1 month',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);

-- 월간 연속 집계
CREATE MATERIALIZED VIEW keyword_rankings_monthly
WITH (timescaledb.continuous) AS
SELECT
    keyword_id,
    time_bucket('30 days', recorded_at) AS month,
    AVG(rank)::DECIMAL(5,1) AS avg_rank,
    MIN(rank) AS best_rank,
    MAX(rank) AS worst_rank,
    COUNT(*) AS data_points
FROM keyword_rankings
GROUP BY keyword_id, time_bucket('30 days', recorded_at);

SELECT add_continuous_aggregate_policy('keyword_rankings_monthly',
    start_offset => INTERVAL '3 months',
    end_offset => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day'
);

-- ============================================
-- content_analyses
-- ============================================
CREATE TABLE content_analyses (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT REFERENCES sites(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(500),
    content TEXT NOT NULL,
    target_keywords VARCHAR(1000),
    seo_score DECIMAL(5,2),
    readability_score DECIMAL(5,2),
    keyword_density JSONB,
    structure_analysis JSONB,
    suggestions JSONB,
    generated_meta_title VARCHAR(200),
    generated_meta_description VARCHAR(500),
    ai_provider VARCHAR(20),
    ai_model VARCHAR(50),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ
);

CREATE INDEX idx_content_analyses_user_id ON content_analyses(user_id);
CREATE INDEX idx_content_analyses_site_id ON content_analyses(site_id);
CREATE INDEX idx_content_analyses_status ON content_analyses(status);
CREATE INDEX idx_content_analyses_created_at ON content_analyses(created_at DESC);

-- ============================================
-- reports
-- ============================================
CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    site_id BIGINT NOT NULL REFERENCES sites(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    summary JSONB,
    file_path VARCHAR(1024),
    file_size BIGINT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reports_site_id ON reports(site_id);
CREATE INDEX idx_reports_user_id ON reports(user_id);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);

-- ============================================
-- notifications
-- ============================================
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    type VARCHAR(30) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    reference_type VARCHAR(30),
    reference_id BIGINT,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_user_read ON notifications(user_id, is_read);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_reference ON notifications(reference_type, reference_id);

-- ============================================
-- public_analyses (V6 마이그레이션)
-- ============================================
CREATE TABLE public_analyses (
    id BIGSERIAL PRIMARY KEY,
    url VARCHAR(2048) NOT NULL,
    domain VARCHAR(500) NOT NULL,
    seo_score NUMERIC(5, 2),
    title_score NUMERIC(5, 2),
    meta_description_score NUMERIC(5, 2),
    heading_score NUMERIC(5, 2),
    image_score NUMERIC(5, 2),
    link_score NUMERIC(5, 2),
    performance_score NUMERIC(5, 2),
    title VARCHAR(1000),
    meta_description TEXT,
    canonical_url VARCHAR(2048),
    response_time_ms INTEGER,
    content_length INTEGER,
    total_images INTEGER NOT NULL DEFAULT 0,
    images_without_alt INTEGER NOT NULL DEFAULT 0,
    internal_links INTEGER NOT NULL DEFAULT 0,
    external_links INTEGER NOT NULL DEFAULT 0,
    broken_links INTEGER NOT NULL DEFAULT 0,
    total_headings INTEGER NOT NULL DEFAULT 0,
    has_og_tags BOOLEAN NOT NULL DEFAULT FALSE,
    has_twitter_cards BOOLEAN NOT NULL DEFAULT FALSE,
    has_viewport BOOLEAN NOT NULL DEFAULT FALSE,
    has_favicon BOOLEAN NOT NULL DEFAULT FALSE,
    has_robots_txt BOOLEAN NOT NULL DEFAULT FALSE,
    has_sitemap BOOLEAN NOT NULL DEFAULT FALSE,
    has_https BOOLEAN NOT NULL DEFAULT FALSE,
    heading_structure JSONB,
    issues JSONB,
    link_list JSONB,
    meta_tags JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    error_message VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_public_analyses_domain ON public_analyses(domain);
CREATE INDEX idx_public_analyses_seo_score ON public_analyses(seo_score DESC);
CREATE INDEX idx_public_analyses_created_at ON public_analyses(created_at DESC);
CREATE INDEX idx_public_analyses_status ON public_analyses(status);
```

## 5. 데이터 보존 및 관리 정책

| 데이터 | 보존 기간 | 정책 |
|--------|----------|------|
| keyword_rankings | 1년 | TimescaleDB retention policy 자동 삭제 |
| crawl_results | 최근 5회 크롤링 | 애플리케이션 로직으로 오래된 결과 삭제 |
| page_analyses | crawl_results와 동일 | CASCADE 삭제 |
| content_analyses | 영구 보존 | 사용자 데이터 |
| reports | 영구 보존 | MinIO 파일은 1년 후 아카이브 |
| notifications | 90일 | 스케줄러로 주기적 삭제 |
| public_analyses | 90일 | 비인증 공개 분석 결과, 스케줄러로 주기적 삭제 |
