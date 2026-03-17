# SEO Dashboard 로컬 개발 환경 설정

처음 프로젝트에 참여하는 개발자를 위한 단계별 설정 가이드입니다.

## 1. 사전 요구사항 설치

아래 도구가 설치되어 있는지 확인합니다.

```bash
# Java 21 확인
java -version
# 출력 예: openjdk version "21.x.x"

# Docker 확인
docker --version
docker compose version

# Node.js 확인 (프론트엔드용)
node -v
# 출력 예: v18.x.x 이상
```

Java 21이 없다면 [Adoptium](https://adoptium.net/) 또는 [SDKMAN](https://sdkman.io/)으로 설치합니다.

```bash
# SDKMAN 사용 시
sdk install java 21-tem
```

## 2. 소스 코드 클론

```bash
git clone <repository-url> seo-dashboard
cd seo-dashboard
```

## 3. 인프라 실행 (PostgreSQL + Redis)

Docker Compose로 데이터베이스와 캐시 서버를 실행합니다.

```bash
cd docker
docker compose up -d
```

정상 가동 확인:

```bash
docker compose ps
```

두 서비스 모두 `healthy` 상태여야 합니다. healthcheck가 설정되어 있어 시작 후 약 10~30초 소요될 수 있습니다.

```
NAME                      STATUS
seo-dashboard-postgres    Up (healthy)
seo-dashboard-redis       Up (healthy)
```

기본 접속 정보:

| 서비스 | 호스트 | 포트 | 사용자 | 비밀번호 | 데이터베이스 |
|--------|--------|------|--------|----------|-------------|
| PostgreSQL | localhost | 5432 | seo_user | seo_password | seo_dashboard |
| Redis | localhost | 6379 | - | _(없음)_ | - |

## 4. 백엔드 실행

프로젝트 루트에서 실행합니다.

```bash
./gradlew :seo-api:bootRun --args='--spring.profiles.active=local'
```

`local` 프로필은 아래 설정을 활성화합니다:
- **DDL 자동 업데이트**: `ddl-auto=update` (스키마 자동 생성/변경)
- **SQL 디버그 로깅**: 실행되는 SQL과 바인딩 파라미터 출력
- **DEBUG 레벨 로깅**: 애플리케이션 및 Spring Security 상세 로그

첫 실행 시 Gradle 의존성 다운로드에 시간이 소요됩니다. `org.gradle.parallel=true`와 `org.gradle.caching=true`가 설정되어 있어 이후 빌드는 빠릅니다.

## 5. 백엔드 정상 동작 확인

```bash
curl http://localhost:8080/actuator/health
```

정상 응답:

```json
{"status":"UP"}
```

## 6. Swagger UI 접속

브라우저에서 API 문서를 확인합니다.

```
http://localhost:8080/swagger-ui.html
```

Swagger UI에서 모든 API 엔드포인트를 조회하고 직접 테스트할 수 있습니다. 태그와 오퍼레이션이 알파벳순으로 정렬됩니다.

## 7. 프론트엔드 실행

별도 터미널에서 실행합니다.

```bash
cd frontend
npm install
npm run dev
```

Vite 개발 서버가 `http://localhost:3000`에서 시작됩니다.

`/api` 경로의 요청은 백엔드(`http://localhost:8080`)로 자동 프록시됩니다. 별도의 CORS 설정 없이 API 호출이 가능합니다.

## 8. 전체 구동 확인

| 구성 요소 | URL | 상태 확인 방법 |
|-----------|-----|---------------|
| 백엔드 API | http://localhost:8080 | `curl localhost:8080/actuator/health` |
| Swagger UI | http://localhost:8080/swagger-ui.html | 브라우저 접속 |
| 프론트엔드 | http://localhost:3000 | 브라우저 접속 |
| PostgreSQL | localhost:5432 | `docker compose ps` |
| Redis | localhost:6379 | `docker compose ps` |

## 자주 발생하는 문제

### PostgreSQL 연결 실패

```
Connection refused: localhost:5432
```

**원인**: Docker 컨테이너가 실행되지 않았거나 아직 준비 중입니다.

**해결**:
```bash
cd docker
docker compose ps          # 상태 확인
docker compose up -d       # 미실행 시 시작
docker compose logs postgres  # 로그 확인
```

### 포트 충돌

```
Port 5432 is already in use
```

**원인**: 로컬에 PostgreSQL이 이미 설치되어 있거나 다른 컨테이너가 포트를 사용 중입니다.

**해결**:
```bash
# 어떤 프로세스가 포트를 사용 중인지 확인
lsof -i :5432

# 기존 프로세스 종료 후 다시 시작하거나,
# docker-compose.yml에서 포트 매핑 변경 (예: "5433:5432")
```

### Gradle 빌드 실패: Java 버전

```
Incompatible because this component declares a component compatible with Java 21
```

**원인**: Java 21이 설치되지 않았거나 JAVA_HOME이 올바르지 않습니다.

**해결**:
```bash
java -version                    # 현재 버전 확인
echo $JAVA_HOME                  # JAVA_HOME 확인
export JAVA_HOME=$(/usr/libexec/java_home -v 21)  # macOS
```

### 프론트엔드 API 호출 실패 (CORS)

```
Access to fetch at 'http://localhost:8080/api/...' has been blocked by CORS policy
```

**원인**: 프론트엔드가 Vite 프록시를 거치지 않고 직접 백엔드를 호출하고 있습니다.

**해결**: API 호출 시 전체 URL이 아닌 상대 경로(`/api/...`)를 사용하십시오. Vite가 자동으로 백엔드로 프록시합니다.

### Redis 연결 실패

```
Unable to connect to Redis
```

**원인**: Redis 컨테이너가 실행되지 않았습니다.

**해결**:
```bash
docker compose ps
docker compose up -d redis
docker compose logs redis
```

### DDL 검증 실패 (기본 프로필)

```
Schema-validation: missing table
```

**원인**: 기본 프로필은 `ddl-auto=validate`이므로 스키마가 이미 존재해야 합니다.

**해결**: 로컬 개발 시 반드시 `local` 프로필을 사용하십시오. `local` 프로필은 `ddl-auto=update`로 스키마를 자동 생성합니다.

```bash
./gradlew :seo-api:bootRun --args='--spring.profiles.active=local'
```

## 유용한 명령어 모음

```bash
# 전체 빌드 (테스트 포함)
./gradlew build

# 테스트만 실행
./gradlew test

# 특정 모듈 테스트
./gradlew :seo-common:test
./gradlew :seo-crawler:test
./gradlew :seo-api:test

# 인프라 중지
cd docker && docker compose down

# 인프라 중지 + 데이터 삭제
cd docker && docker compose down -v

# 프론트엔드 린트
cd frontend && npm run lint

# 프론트엔드 프로덕션 빌드 미리보기
cd frontend && npm run build && npm run preview
```
