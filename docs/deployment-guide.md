# SEO Dashboard 배포 가이드

## 사전 요구사항

| 구성 요소 | 최소 버전 | 비고 |
|-----------|----------|------|
| Java (JDK) | 21 | `java -version`으로 확인. Gradle toolchain이 자동 관리 |
| Docker & Docker Compose | 20.10+ / 2.0+ | PostgreSQL, Redis 컨테이너 실행용 |
| Node.js | 18+ | 프론트엔드 빌드 전용. npm 포함 |
| Gradle | 포함됨 | `./gradlew` wrapper 사용. 별도 설치 불필요 |

## 환경 변수

아래 환경 변수로 애플리케이션 동작을 제어합니다. `${}` 안의 값은 기본값입니다.

### 데이터베이스 (PostgreSQL)

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `DB_HOST` | `localhost` | PostgreSQL 호스트 주소 |
| `DB_PORT` | `5432` | PostgreSQL 포트 |
| `DB_NAME` | `seo_dashboard` | 데이터베이스 이름 |
| `DB_USERNAME` | `seo_user` | 데이터베이스 사용자 |
| `DB_PASSWORD` | `seo_password` | 데이터베이스 비밀번호. 운영 환경에서 반드시 변경 |

### Redis

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `REDIS_HOST` | `localhost` | Redis 호스트 주소 |
| `REDIS_PORT` | `6379` | Redis 포트 |
| `REDIS_PASSWORD` | _(빈 문자열)_ | Redis 비밀번호. 운영 환경에서 설정 권장 |

### 인증 (JWT)

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `JWT_SECRET` | 개발용 Base64 키 | JWT 서명 키. **운영 환경에서 반드시 강력한 값으로 교체** |

Access Token 유효기간은 30분, Refresh Token 유효기간은 14일로 설정되어 있습니다.

### CORS

| 변수명 | 기본값 | 설명 |
|--------|--------|------|
| `CORS_ALLOWED_ORIGINS` | `http://localhost:3000,http://localhost:3001` | 허용할 프론트엔드 도메인. 쉼표로 구분 |

## Docker Compose 인프라 구성

PostgreSQL과 Redis를 Docker Compose로 실행합니다.

```bash
cd docker
docker compose up -d
```

구성 내용:

- **PostgreSQL 15 Alpine**: 포트 5432, 데이터베이스 `seo_dashboard`, 사용자 `seo_user`
- **Redis 7 Alpine**: 포트 6379
- 두 서비스 모두 healthcheck가 설정되어 있어 정상 가동을 자동으로 확인합니다
- 데이터는 Docker named volume(`postgres_data`, `redis_data`)에 영속적으로 저장됩니다

상태 확인:

```bash
docker compose ps
docker compose logs postgres
docker compose logs redis
```

## 백엔드 빌드 및 실행

### 프로필 구조

| 프로필 | 용도 | DDL 모드 | SQL 로깅 | Swagger |
|--------|------|----------|----------|---------|
| _(기본)_ | 공통 설정 | `validate` | 포맷팅만 | 활성화 |
| `local` | 로컬 개발 | `update` | DEBUG + TRACE | 활성화 |
| `prod` | 운영 배포 | `validate` | WARN | 비활성화 |

### 로컬 개발 실행

```bash
./gradlew :seo-api:bootRun --args='--spring.profiles.active=local'
```

### 운영 환경 빌드 및 실행

```bash
# JAR 빌드
./gradlew :seo-api:bootJar

# 환경 변수 설정 후 실행
export SPRING_PROFILES_ACTIVE=prod
export JWT_SECRET=$(openssl rand -base64 64)
export CORS_ALLOWED_ORIGINS=https://yourdomain.com
export DB_HOST=your-db-host
export DB_PASSWORD=your-secure-password
export REDIS_HOST=your-redis-host

java -jar seo-api/build/libs/seo-api-0.0.1-SNAPSHOT.jar
```

## 프론트엔드 빌드 및 배포

### 빌드

```bash
cd frontend
npm install
npm run build
```

빌드 결과물은 `frontend/dist/` 디렉토리에 생성됩니다.

### 정적 파일 서빙

빌드된 파일을 웹 서버(Nginx, Caddy 등)로 서빙합니다. SPA이므로 모든 경로를 `index.html`로 fallback해야 합니다.

Nginx 설정 예시:

```nginx
server {
    listen 80;
    server_name yourdomain.com;
    root /path/to/frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }

    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }
}
```

개발 시에는 Vite dev server가 `/api` 경로를 백엔드(8080 포트)로 자동 프록시합니다.

## 운영 배포 체크리스트

배포 전 아래 항목을 반드시 확인하십시오.

### 필수 (미충족 시 보안 위험)

- [ ] **JWT_SECRET 설정**: 강력한 랜덤 값 사용
  ```bash
  openssl rand -base64 64
  ```
  기본 개발용 키를 운영 환경에서 절대 사용하지 마십시오.

- [ ] **CORS_ALLOWED_ORIGINS 설정**: 실제 프론트엔드 도메인만 허용
  ```
  CORS_ALLOWED_ORIGINS=https://yourdomain.com,https://www.yourdomain.com
  ```

- [ ] **spring.profiles.active=prod 활성화**: 운영 프로필이 적용되어야 합니다

- [ ] **DB_PASSWORD 변경**: 기본 비밀번호(`seo_password`) 사용 금지

### 운영 프로필 자동 적용 사항

`prod` 프로필이 활성화되면 아래 설정이 자동으로 적용됩니다:

- **Swagger/OpenAPI 비활성화**: `springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`
- **Actuator 별도 포트**: 관리 엔드포인트가 9090 포트에서 서비스됨. 외부 접근 차단 필요
- **HikariCP 풀 확장**: `maximum-pool-size=20`, `minimum-idle=10`
- **로깅 레벨 WARN**: 불필요한 로그 출력 최소화
- **SQL 로깅 비활성화**: `show-sql=false`

### 권장 사항

- [ ] Actuator 포트(9090) 방화벽에서 외부 접근 차단
- [ ] HTTPS 적용 (TLS 인증서 설정)
- [ ] Redis에 비밀번호 설정 (`REDIS_PASSWORD`)
- [ ] 데이터베이스 정기 백업 구성
- [ ] 로그 수집 및 모니터링 시스템 연동

## 멀티 모듈 구조

이 프로젝트는 Gradle 멀티 모듈로 구성되어 있습니다.

```
seo-dashboard/
  seo-common/    -- 공통 유틸리티, 엔티티
  seo-crawler/   -- SEO 크롤링 및 분석 엔진
  seo-scheduler/ -- 스케줄링 로직
  seo-ai/        -- AI 분석 연동
  seo-api/       -- Spring Boot 메인 애플리케이션 (위 모듈을 모두 포함)
  frontend/      -- React 프론트엔드
  docker/        -- Docker Compose 인프라 설정
```

`seo-api` 모듈이 나머지 모듈을 의존성으로 포함하므로, `:seo-api:bootJar` 또는 `:seo-api:bootRun`만 실행하면 됩니다.
