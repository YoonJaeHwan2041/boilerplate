# boilerplate

프로젝트를 새로 시작할 때마다 반복해서 만드는 **Spring Security / JWT / 전역 예외 처리 / 공통 응답** 세팅을
미리 만들어 두는 보일러플레이트.

코드는 한 조각씩 직접 이해하고 주석을 달면서 쌓아 올린다. 다른 프로젝트(`arcsorna_back`)에서 쓰던 패턴을
가져와 다듬은 것.

---

## 브랜치 구성 (단계별)

| 브랜치 | 내용 |
|--------|------|
| `main` | 기본 Spring Security 설정 (CORS / CSRF / STATELESS / 인가 규칙) |
| `jwt` | JWT 발급·검증 필터, `CustomUserDetails`, EntryPoint/AccessDeniedHandler |
| `global-error` | `@RestControllerAdvice` 전역 예외 처리 + 공통 `ApiResponse` + `ErrorCode` 체계 |
| `s3` | AWS S3 파일 업로드 (예정) |
| `full` | 위 브랜치를 전부 합친 최종본 |

> 현재 작업 트리에는 `main` + `jwt` + `global-error` 가 함께 올라가 있고 서로 연동된 상태로 검증되어 있다.

---

## 기술 스택

| | |
|--|--|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Security 7, Jackson 3) |
| Build | Gradle (Kotlin DSL), Gradle Wrapper 9.7.1 |
| JWT | `io.jsonwebtoken:jjwt` 0.12.6 (HS256) |
| DB | PostgreSQL 16 (docker-compose) / 테스트는 인메모리 H2 |
| Docs | springdoc-openapi (Swagger UI) |
| 기타 | Lombok, Bean Validation |

---

## 패키지 구조

```
com.jelly.boilerplate
├─ BoilerplateApplication.java
│
├─ auth/                              # 데모 인증 API (실제 프로젝트에선 domain/* 로 교체)
│  ├─ AuthController.java             #   POST /api/v1/auth/login  (인메모리 유저)
│  └─ MeController.java               #   GET  /api/v1/members/me, /admin-only
│
└─ global/
   ├─ exception/
   │  ├─ ExceptionCode.java           # 인터페이스: getCode() / getStatus() / getMessage()
   │  ├─ BusinessException.java       # 의도적으로 던지는 예외 (ExceptionCode 를 들고 다님)
   │  └─ GlobalExceptionHandler.java  # @RestControllerAdvice — 모든 예외를 ApiResponse 로 변환
   │
   ├─ response/
   │  ├─ ApiResponse.java             # 공통 응답 봉투 { success, code, message, data }
   │  └─ code/
   │     └─ AuthExceptionCode.java    # AUTH* 에러 코드 (implements ExceptionCode)
   │
   ├─ jwt/
   │  ├─ JwtProperties.java           # @ConfigurationProperties("jwt")
   │  ├─ JwtProvider.java             # 토큰 생성 / 파싱 (jjwt 0.12.x)
   │  └─ JwtAuthenticationFilter.java # OncePerRequestFilter — Bearer 헤더 → SecurityContext
   │
   └─ security/
      ├─ SecurityConfig.java          # SecurityFilterChain, CORS, PasswordEncoder
      ├─ CustomUserDetails.java       # SecurityContext 에 저장되는 principal
      ├─ JwtAuthenticationEntryPoint.java  # 401 (인증 안 됨)
      └─ JwtAccessDeniedHandler.java       # 403 (권한 부족)
```

---

## 실행

### 1. DB 기동 (PostgreSQL)

```bash
cp .env.example .env          # 최초 1회
docker compose up -d
docker compose ps             # STATUS 가 healthy 될 때까지 대기
```

| 명령 | 설명 |
|------|------|
| `docker compose up -d` | 백그라운드 기동 |
| `docker compose down` | 중지 (데이터는 볼륨에 유지) |
| `docker compose down -v` | 중지 + 데이터 삭제 |

### 2. 애플리케이션 실행

```bash
./gradlew bootRun
```

기본 포트 `8080`. 포트가 사용 중이면 `./gradlew bootRun --args='--server.port=18080'`.

### 3. 테스트

도커 없이 인메모리 H2 로 돈다 (`src/test/resources/application.yaml`).

```bash
./gradlew test
```

### 환경 변수 (`.env`)

| 변수 | 사용처 | 설명 |
|------|--------|------|
| `JWT_SECRET` | 앱 | HS256 서명 키(원문, **최소 32바이트**). 운영에선 반드시 주입 |
| `DB_NAME` / `DB_USERNAME` / `DB_PASSWORD` / `DB_PORT` | docker compose | postgres 컨테이너 생성 값 |
| `DB_URL` | 앱 | JDBC 접속 URL (앱도 컨테이너로 띄우면 `localhost` → `postgres`) |
| `JPA_DDL_AUTO` | 앱 | `update`(dev) / `validate`(운영) |

> `docker compose` 는 `.env` 를 자동으로 읽는다. `./gradlew bootRun` 에 넘기려면
> `export $(grep -v '^#' .env | xargs) && ./gradlew bootRun`. 아무것도 export 하지 않으면
> `application.yaml` 의 개발용 기본값(위 `.env.example` 과 동일)으로 뜬다.

---

## API

### 데모 계정 (인메모리 — `AuthController` 에서 주입)

| username | password | role |
|----------|----------|------|
| `user` | `password` | `ROLE_USER` |
| `admin` | `password` | `ROLE_ADMIN` |

### 엔드포인트

| Method | Path | 인증 | 설명 |
|--------|------|------|------|
| `POST` | `/api/v1/auth/login` | 불필요 | 로그인 → Access Token 발급 |
| `GET` | `/api/v1/members/me` | 필요 | 내 정보 (토큰 principal 반환) |
| `GET` | `/api/v1/members/admin-only` | `ROLE_ADMIN` | 메서드 시큐리티(`@PreAuthorize`) 확인용 |
| `GET` | `/swagger-ui.html` | 불필요 | API 문서 |

### 예시

```bash
# 1) 로그인
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"password"}'
# → { "success": true, "code": "0000", "message": "",
#     "data": { "accessToken": "eyJ...", "tokenType": "Bearer" } }

# 2) 토큰으로 보호된 API 호출
curl -s http://localhost:8080/api/v1/members/me \
  -H 'Authorization: Bearer eyJ...'
# → { "success": true, "code": "0000", "data": { "id":1, "username":"user", "role":"ROLE_USER" } }
```

### 동작 검증표 (구현 완료)

| 상황 | HTTP | code |
|------|------|------|
| 토큰 없이 보호 API | 401 | `AUTH200` 인증 토큰이 필요합니다 |
| 잘못된 아이디/비번 | 401 | `AUTH001` |
| 필수 값 누락 (`@Valid`) | 400 | `COMMON_INVALID_INPUT` |
| 정상 로그인 | 200 | `0000` |
| 만료 토큰 | 401 | `AUTH202` |
| 위조/깨진 토큰 | 401 | `AUTH201` |
| 권한 부족 (`ROLE_USER` → admin API) | 403 | `AUTH100` |

---

## 공통 응답 포맷

성공 / 실패가 **항상 같은 모양**이다. 프론트는 `success` 로 분기하고, `code` 로 세부 처리한다.

```jsonc
// 성공
{ "success": true,  "code": "0000",           "message": "",                       "data": { ... } }
// 실패
{ "success": false, "code": "AUTH202",        "message": "토큰이 만료되었습니다.",   "data": null    }
```

| 필드 | 의미 |
|------|------|
| `success` | 성공 여부 (프론트 인터셉터가 `if (!res.success)` 로 분기) |
| `code` | 성공 시 `"0000"`, 실패 시 상황별 코드(`AUTH*`, `COMMON_*` …) |
| `message` | 사용자에게 보여줄 문구 (실패 시 채움) |
| `data` | 실제 데이터 (실패 시 `null`) |

---

## 에러 코드 & 예외 처리

### 구조

```
서비스/컨트롤러
   └─ throw new BusinessException(AuthExceptionCode.TOKEN_EXPIRED)
        │
        ▼
GlobalExceptionHandler (@RestControllerAdvice)
   - ExceptionCode 에서 HttpStatus / code / message 를 꺼내 ApiResponse 로 변환
   - 4xx = log.warn(스택X) / 5xx = log.error(스택O)
   - 마지막 방어선: @ExceptionHandler(Exception.class) → 500 (원본 메시지 마스킹)
```

- **`ExceptionCode`** : 도메인별 에러 코드 enum 이 구현하는 인터페이스 (`code` / `HttpStatus` / `message`).
  이 계약 덕분에 `BusinessException` 하나 + 핸들러 하나로 모든 도메인을 처리한다.
- **`BusinessException`** : 90% 케이스는 이 예외 하나. `throw new BusinessException(코드)` 또는
  `throw new BusinessException(코드, "상황에 맞는 메시지")`.
- **전용 예외** : 로깅/알림/재시도처럼 **처리 방식이 근본적으로 다른** 소수만 `BusinessException` 을
  상속해 별도 핸들러를 둔다. (예: 해킹 의심 → `log.error` + 알림)

### 필터 단계 예외

시큐리티 필터에서 터지는 401/403(토큰 만료·위조, 권한 부족)은 `@RestControllerAdvice` 가 못 잡는다.
→ `JwtAuthenticationFilter` / `JwtAuthenticationEntryPoint` / `JwtAccessDeniedHandler` 가
`HandlerExceptionResolver` 로 예외를 넘겨 **`GlobalExceptionHandler` 가 동일한 `ApiResponse` 포맷**으로 응답한다.

> Boot 4 는 Jackson 3(`tools.jackson.*`) 을 기본으로 쓰므로 `com.fasterxml.jackson.databind.ObjectMapper`
> 빈이 없다. 필터/핸들러에서 `ObjectMapper` 를 직접 주입하지 말고 위처럼 리졸버에 위임할 것.

### AUTH 에러 코드

| 코드 | HTTP | 의미 |
|------|------|------|
| `AUTH000` | 401 | 인증 실패(원인 불명 포함) |
| `AUTH001` | 401 | 아이디/비밀번호 불일치 |
| `AUTH002` | 401 | 계정 잠김 |
| `AUTH003` | 401 | 비활성화된 계정 |
| `AUTH005` | 401 | 만료된 계정 |
| `AUTH006` | 401 | 추가 인증 필요 |
| `AUTH100` | 403 | 접근 권한 없음 |
| `AUTH101` | 401 | 세션 만료 |
| `AUTH200` | 401 | 토큰 누락 |
| `AUTH201` | 401 | 유효하지 않은 토큰 |
| `AUTH202` | 401 | 토큰 만료 |

---

## 보안 설정 요약 (`SecurityConfig`)

| 설정 | 값 | 이유 |
|------|-----|------|
| CSRF | `disable` | 토큰을 헤더로 전송 → CSRF 공격 성립 안 함 |
| Session | `STATELESS` | 서버가 세션을 만들지 않음 |
| CORS | `localhost:3000`, `localhost:3001` 허용, `allowCredentials(true)` | 로컬 프론트 개발 서버 |
| 공개 경로 | `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**` | 나머지는 유효한 JWT 필요 |
| 필터 | `JwtAuthenticationFilter` 를 `UsernamePasswordAuthenticationFilter` 앞에 추가 | 토큰 먼저 검사 |
| 메서드 시큐리티 | `@EnableMethodSecurity` | `@PreAuthorize("hasRole('ADMIN')")` 사용 가능 |

---

## 데모 코드 → 실제 코드로 바꿀 부분

- `auth/AuthController` 의 **인메모리 유저 맵** → `MemberRepository.findByUsername()` 조회 +
  엔티티의 BCrypt 해시 비밀번호와 `passwordEncoder.matches()` 비교로 교체
- DB 스키마 관리: 지금은 `ddl-auto: update`. 운영 전환 시 `validate` + Flyway/Liquibase 도입 권장
- `GlobalExceptionHandler` 의 하드코딩된 `"COMMON_INVALID_INPUT"` / `"COMMON_INTERNAL_ERROR"` 문자열 →
  `CommonExceptionCode` enum 을 만들어 상수로 교체
- 리프레시 토큰 / 토큰 재발급 엔드포인트는 아직 없음 (필요 시 추가)
