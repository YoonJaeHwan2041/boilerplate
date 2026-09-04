# boilerplate

프로젝트를 새로 시작할 때마다 반복해서 만드는 **Spring Security / JWT / 전역 예외 처리 / 공통 응답** 세팅을
미리 만들어 두는 보일러플레이트.

코드는 한 조각씩 직접 이해하고 주석을 달면서 쌓아 올린다. 다른 프로젝트에서 쓰던 패턴을
가져와 다듬은 것.

---

## 브랜치 구성 (단계별)

| 브랜치 | 내용 |
|--------|------|
| `main` | 기본 Spring Security 설정 (CORS / CSRF / STATELESS / 인가 규칙) |
| `jwt` | JWT 발급·검증 필터, `CustomUserDetails`, EntryPoint/AccessDeniedHandler |
| `jwt-refresh` | Access(body) + Refresh(HttpOnly 쿠키 + DB 저장) 하이브리드, `/refresh` · `/logout` |
| `global-error` | `@RestControllerAdvice` 전역 예외 처리 + 공통 `ApiResponse` + `ErrorCode` 체계 |
| `s3` | AWS S3 파일 업로드 (SDK v2) — 업로드 / 삭제 |
| `full` | 위 브랜치를 전부 합친 최종본 |

> 현재 작업 트리에는 `main` + `jwt` + `jwt-refresh` + `global-error` + `s3` 가 함께 올라가 있고 서로 연동된 상태로 검증되어 있다.

---

## 기술 스택

| | |
|--|--|
| Language | Java 25 |
| Framework | Spring Boot 4.1.1 (Spring Security 7, Jackson 3) |
| Build | Gradle (Kotlin DSL), Gradle Wrapper 9.7.1 |
| JWT | `io.jsonwebtoken:jjwt` 0.12.6 (HS256). Access(헤더) + Refresh(HttpOnly 쿠키 + DB) |
| DB | PostgreSQL 16 (docker-compose) / 테스트는 인메모리 H2 |
| Storage | AWS S3 (`software.amazon.awssdk:s3` 2.31) |
| Docs | springdoc-openapi (Swagger UI) |
| 기타 | Lombok, Bean Validation |

---

## 패키지 구조

```
com.jelly.boilerplate
├─ BoilerplateApplication.java
│
├─ auth/                              # 데모 인증 API (실제 프로젝트에선 domain/* 로 교체)
│  ├─ AuthController.java             #   POST /api/v1/auth/login · /refresh · /logout
│  ├─ MeController.java               #   GET  /api/v1/members/me, /admin-only
│  ├─ RefreshTokenService.java        #   Refresh 저장/대조/삭제 (SHA-256 해시)
│  ├─ RefreshTokenCleanupScheduler.java  #   만료된 Refresh 행 매일 정리
│  └─ domain/
│     ├─ RefreshToken.java            #   @Entity  (user_id 1명 = 1행)
│     └─ RefreshTokenRepository.java
│
├─ file/                             # 데모 파일 API
│  └─ FileController.java             #   POST/DELETE /api/v1/files
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
   │     ├─ AuthExceptionCode.java    # AUTH* 에러 코드 (implements ExceptionCode)
   │     └─ FileExceptionCode.java    # FILE* 에러 코드
   │
   ├─ jwt/
   │  ├─ JwtProperties.java           # @ConfigurationProperties("jwt")  Access/Refresh 키·만료·회전
   │  ├─ JwtProvider.java             # Access/Refresh 생성·파싱 (서로 다른 키, jjwt 0.12.x)
   │  └─ JwtAuthenticationFilter.java # OncePerRequestFilter — Bearer 헤더 → SecurityContext
   │
   ├─ security/
   │  ├─ SecurityConfig.java          # SecurityFilterChain, CORS, PasswordEncoder
   │  ├─ AuthCookieProperties.java    # @ConfigurationProperties("app.cookie")  secure/sameSite/path
   │  ├─ CustomUserDetails.java       # SecurityContext 에 저장되는 principal
   │  ├─ JwtAuthenticationEntryPoint.java  # 401 (인증 안 됨)
   │  └─ JwtAccessDeniedHandler.java       # 403 (권한 부족)
   │
   ├─ util/
   │  └─ CookieUtil.java              # Refresh 쿠키 생성/삭제 (ResponseCookie)
   │
   └─ s3/
      ├─ S3Properties.java            # @ConfigurationProperties("aws.s3")
      ├─ S3Config.java                # S3Client / S3Presigner 빈
      └─ S3Uploader.java             # 업로드 / 삭제
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
| `JWT_SECRET` / `JWT_REFRESH_SECRET` | 앱 | HS256 서명 키(원문, **최소 32바이트**). Access·Refresh 는 서로 다른 키. 운영에선 반드시 주입 |
| `JWT_REFRESH_ROTATION` | 앱 | `/refresh` 때 Refresh 도 재발급할지 (기본 `false`) |
| `COOKIE_SECURE` / `COOKIE_SAME_SITE` / `COOKIE_PATH` | 앱 | Refresh 쿠키 속성. 로컬은 `false` / `Lax` / `/api/v1/auth` |
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
| `POST` | `/api/v1/auth/login` | 불필요 | 로그인 → Access(body) + Refresh(HttpOnly 쿠키) |
| `POST` | `/api/v1/auth/refresh` | 쿠키 | Refresh 쿠키 → 새 Access 발급 |
| `POST` | `/api/v1/auth/logout` | 쿠키 | 서버의 Refresh 삭제 + 쿠키 제거 |
| `GET` | `/api/v1/members/me` | 필요 | 내 정보 (토큰 principal 반환) |
| `GET` | `/api/v1/members/admin-only` | `ROLE_ADMIN` | 메서드 시큐리티(`@PreAuthorize`) 확인용 |
| `POST` | `/api/v1/files` | 필요 | `multipart/form-data` (`file`, `dir?`) → S3 업로드 |
| `DELETE` | `/api/v1/files?key=...` | 필요 | S3 객체 삭제 |
| `GET` | `/swagger-ui.html` | 불필요 | API 문서 |

### 예시

```bash
B=http://localhost:8080

# 1) 로그인 — Access 는 body, Refresh 는 Set-Cookie 로 옴 (-c 로 쿠키 저장)
curl -s -c cookies.txt -X POST $B/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"user","password":"password"}'
# → data.accessToken 저장.  cookies.txt 에 refreshToken 쿠키 저장됨

# 2) Access 로 보호된 API
curl -s $B/api/v1/members/me -H 'Authorization: Bearer <accessToken>'

# 3) Access 만료 시 재발급 — 쿠키 자동 전송 (-b), 새 Access 를 받음
curl -s -b cookies.txt -X POST $B/api/v1/auth/refresh

# 4) 로그아웃 — 서버의 Refresh 삭제 + 쿠키 만료
curl -s -b cookies.txt -c cookies.txt -X POST $B/api/v1/auth/logout
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
| 쿠키로 `/refresh` | 200 | `0000` (새 Access) |
| 쿠키 없이 `/refresh` | 401 | `AUTH200` |
| 위조 Refresh 쿠키 | 401 | `AUTH201` |
| 로그아웃 후 같은(미만료) Refresh 재사용 | 401 | `AUTH201` (DB 행이 삭제돼 대조 실패) |

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
| CSRF | `disable` | 아래 [CSRF](#csrf) 참고 |
| Session | `STATELESS` | 서버가 세션을 만들지 않음 |
| CORS | `localhost:3000`, `localhost:3001` 허용, `allowCredentials(true)` | 로컬 프론트 개발 서버 |
| 공개 경로 | `/api/v1/auth/**`, `/swagger-ui/**`, `/v3/api-docs/**` | 나머지는 유효한 JWT 필요 |
| 필터 | `JwtAuthenticationFilter` 를 `UsernamePasswordAuthenticationFilter` 앞에 추가 | 토큰 먼저 검사 |
| 메서드 시큐리티 | `@EnableMethodSecurity` | `@PreAuthorize("hasRole('ADMIN')")` 사용 가능 |

---

## Refresh Token (하이브리드)

### 왜 이렇게

토큰 하나로는 **"편함(재로그인 안 함)"** 과 **"안전(탈취 시 짧게 만료·즉시 무효화)"** 을 동시에 못 잡는다.
→ 역할을 둘로 나눈다.

| | Access Token | Refresh Token |
|--|--------------|---------------|
| 용도 | 매 API 호출 신분증 | Access **재발급**받을 때만 |
| 수명 | 짧다 (30분) | 길다 (14일) |
| 전송 | `Authorization: Bearer` 헤더 | HttpOnly 쿠키 (JS 접근 불가) |
| 클라 저장 | localStorage / 메모리 | 브라우저가 쿠키로 관리 |
| 서버 저장 | 안 함 (stateless) | **DB `refresh_token` 테이블에 SHA-256 해시로** |

서버가 Refresh 를 저장하는 이유 = **로그아웃 / 비밀번호 변경 시 삭제하여 재발급을 끊기 위해서**.
(Access 하나만 쓸 땐 만료 전 취소가 불가능했다.)

### 흐름

```
로그인    POST /login  → Access(body) + Refresh(Set-Cookie HttpOnly) + DB 저장
API 호출  Authorization: Bearer <Access>
Access 만료(401 AUTH202)
  → POST /refresh  (쿠키 자동 전송, body 없음)
      · 쿠키 Refresh 서명·만료 검증
      · DB의 해시와 대조 (없으면/불일치면 401 → 로그아웃됐거나 폐기됨)
      · 새 Access 발급 (회전 ON 이면 Refresh 도 교체)
  → 클라가 실패한 요청 재시도 (사용자는 못 느낌)
로그아웃  POST /logout → DB의 Refresh 삭제 + 빈 쿠키(Max-Age=0)
Refresh 만료 → /refresh 도 401 → 진짜 재로그인
```

### 설정 (`jwt.*`, `app.cookie.*`)

| 키 | 기본값 | 설명 |
|----|--------|------|
| `jwt.refresh-secret` | (더미) | Refresh 전용 서명 키. **Access 키와 달라야** 함 |
| `jwt.refresh-token-validity-seconds` | `1209600` (14일) | Refresh 만료 |
| `jwt.refresh-rotation` | `false` | `/refresh` 때 Refresh 도 재발급할지. 켜면 탈취 탐지 가능하지만 DB 쓰기↑ + 동시요청 레이스 대비 필요 |
| `jwt.refresh-cleanup-cron` | 매일 04:00 | 만료된 `refresh_token` 행 정리 (RDB 는 TTL 자동삭제 없음) |
| `app.cookie.secure` | `false` | 운영(HTTPS) `true` |
| `app.cookie.same-site` | `Lax` | 아래 [CSRF](#csrf) 참고 |
| `app.cookie.path` | `/api/v1/auth` | Refresh 쿠키가 실릴 경로 (다른 요청엔 안 실림) |

### 설계 결정 (이 보일러플레이트)

- **저장소: RDB(JPA)** — 인프라 0. 트래픽 커지면 Redis 로 교체(TTL 자동, `RefreshTokenService` 만 갈아끼움)
- **키: `user_id` 1개 (deviceId 없음)** — 한 계정은 한 곳에서만 로그인 유지. 다른 기기 로그인 시 기존 Refresh 는 무효화됨. "기기별 세션"이 필요하면 `device_id` 컬럼 추가
- **회전: OFF** — 옵션(`jwt.refresh-rotation`)으로만 존재. 금융·관리자처럼 탈취 탐지가 중요하면 `true`
- **Refresh 토큰이 스스로 `username`·`role` 을 claim 으로 들고 있음** — 재발급 때 DB 유저 조회 없이 새 Access 생성 (데모 특성). 실제 프로젝트는 `/refresh` 에서 최신 유저 정보를 DB 에서 읽는 게 정확

---

## CSRF

현재 `csrf.disable()`. 이유와, 언제 다시 켜야 하는지.

### 왜 꺼도 되나 (지금 구성)

- **Access(API 호출)**: `Authorization` 헤더로 전송 → 브라우저가 자동으로 안 실어줌 → 다른 사이트가 남의 토큰을 못 넣음 → **CSRF 불가**
- **Refresh 쿠키**: `SameSite=Lax` + `Path=/api/v1/auth` → 다른 사이트에서 시작된 요청엔 쿠키가 거의 안 실림. 쿠키를 쓰는 요청도 `/refresh`·`/logout` 둘뿐이고, 강제 `/refresh` 는 응답을 못 읽어 무해, 강제 `/logout` 은 성가심 수준

→ **프론트·백이 같은 site 이거나 로컬(http)이면 `disable` 유지로 충분하다.**

### 언제 다시 켜야 하나

**프론트와 백엔드를 완전히 다른 도메인 + HTTPS 로 나눌 때** (예: `front.example.com` ↔ `api.example.com`, 또는 프론트를 Vercel 등에).

이 경우 쿠키가 cross-site 로 오가야 하므로:

1. `app.cookie.same-site` → `None`, `app.cookie.secure` → `true` (HTTPS 필수)
2. cross-site 쿠키 자동 전송 = CSRF 노출 ↑ → **`/refresh`·`/logout` 에만** CSRF 토큰 추가:

```java
http.csrf(csrf -> csrf
    .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
    .ignoringRequestMatchers(
        "/api/v1/auth/login",
        // Authorization 헤더로 인증하는 요청은 CSRF 안전 → 제외
        request -> request.getHeader("Authorization") != null
    )
);
```

> 헤더(`Authorization`)로 인증하는 API 는 **절대 CSRF 토큰을 요구하면 안 된다** — 안전한데 프론트만 불편해진다.

### 도메인 형태별 정리

| 배포 형태 | `same-site` | `secure` | CSRF |
|-----------|-------------|----------|------|
| 로컬 (`localhost:3000` ↔ `:8080`) | `Lax` | `false` | disable |
| 한 서버 / 리버스 프록시 한 주소 | `Lax` | `false`(http) / `true`(https) | disable |
| 서브도메인 (`app.com` ↔ `api.app.com`) | `Lax` | `true` | disable |
| **완전히 다른 도메인** | `None` | `true` | `/refresh`·`/logout` 에 토큰 |

---

## S3 파일 업로드

### 설정 (`aws.s3.*`)

| 키 | 환경변수 | 설명 |
|----|----------|------|
| `region` | `AWS_REGION` | 예: `ap-northeast-2` |
| `bucket` | `AWS_S3_BUCKET` | 버킷 이름 |
| `access-key` / `secret-key` | `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | **비우면** DefaultCredentialsProvider (env → `~/.aws` → IAM Role) |
| `endpoint` | `AWS_S3_ENDPOINT` | LocalStack/MinIO 주소. 지정 시 path-style 강제 |
| `path-style-access` | `AWS_S3_PATH_STYLE` | virtual-host 대신 path-style 사용 |

### 동작

- 저장 키: `<dir>/yyyy/MM/dd/<uuid><.ext>` — 원본 파일명은 키에 안 쓴다(한글/공백/중복/경로조작 방지).
  원본 파일명이 필요하면 DB에 별도 저장.
- 업로드 응답: `{ "key": "...", "url": "..." }` — `key` 를 엔티티에 저장하고, 화면에는 `url` 사용
  (**공개 버킷 기준**. 비공개 버킷이면 presigned URL 발급 로직을 추가해야 함).
- 업로드 용량 초과(`spring.servlet.multipart.max-file-size`, 기본 10MB) → `413` `FILE002`.

### LocalStack 으로 로컬 테스트

```bash
docker run -d -p 4566:4566 localstack/localstack
aws --endpoint-url=http://localhost:4566 s3 mb s3://boilerplate-local
# .env
AWS_S3_ENDPOINT=http://localhost:4566
AWS_S3_PATH_STYLE=true
AWS_ACCESS_KEY_ID=test
AWS_SECRET_ACCESS_KEY=test
```

### 예시

```bash
TOKEN=... # 로그인해서 받은 accessToken
curl -s -X POST http://localhost:8080/api/v1/files \
  -H "Authorization: Bearer $TOKEN" \
  -F 'file=@/path/to/img.png' -F 'dir=profile'
# → { "success": true, "code": "0000", "data": { "key": "profile/2026/09/02/ab12...png", "url": "https://..." } }
```

### FILE 에러 코드

| 코드 | HTTP | 의미 |
|------|------|------|
| `FILE000` | 400 | 업로드할 파일 없음 |
| `FILE001` | 400 | 허용되지 않은 파일 형식 |
| `FILE002` | 413 | 파일 크기 초과 |
| `FILE100` | 500 | 업로드 실패 (S3) |
| `FILE101` | 500 | 삭제 실패 (S3) |

---

## 데모 코드 → 실제 코드로 바꿀 부분

- `auth/AuthController` 의 **인메모리 유저 맵** → `MemberRepository.findByUsername()` 조회 +
  엔티티의 BCrypt 해시 비밀번호와 `passwordEncoder.matches()` 비교로 교체
- `/refresh` 에서 새 Access 를 만들 때 지금은 **Refresh claim** 의 `username`·`role` 을 그대로 씀 →
  실제로는 `MemberRepository` 에서 최신 유저 정보를 읽어 반영 (권한 변경·정지 즉시 반영)
- DB 스키마 관리: 지금은 `ddl-auto: update`. 운영 전환 시 `validate` + Flyway/Liquibase 도입 권장
- `GlobalExceptionHandler` 의 하드코딩된 `"COMMON_INVALID_INPUT"` / `"COMMON_INTERNAL_ERROR"` 문자열 →
  `CommonExceptionCode` enum 을 만들어 상수로 교체
- `S3Uploader` 에 파일 형식/확장자 화이트리스트 검증(`FILE001`) 추가 — 지금은 크기만 제한
- 트래픽·다중 인스턴스면 `RefreshTokenService` 를 **Redis 구현**으로 교체 (TTL 자동, 청소 스케줄러 불필요)
- "여러 기기 동시 로그인 / 기기별 로그아웃" 필요하면 `RefreshToken` 에 `device_id` 컬럼 추가
- 완전히 다른 도메인 + HTTPS 배포 시: `app.cookie.same-site=None`, `secure=true` + [CSRF](#csrf) 토큰
