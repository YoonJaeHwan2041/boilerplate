package com.jelly.boilerplate.auth;

import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.jwt.JwtProperties;
import com.jelly.boilerplate.global.jwt.JwtProvider;
import com.jelly.boilerplate.global.response.ApiResponse;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import com.jelly.boilerplate.global.util.CookieUtil;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API — 로그인 / 토큰 재발급 / 로그아웃.
 *
 * <p><b>토큰 전략 (하이브리드)</b>
 * <ul>
 *   <li>Access Token  → 응답 body. 클라가 저장(localStorage/메모리)하고 매 요청 {@code Authorization} 헤더로</li>
 *   <li>Refresh Token → HttpOnly 쿠키. JS 접근 불가. {@code /refresh}, {@code /logout} 요청 때만 자동 전송</li>
 *   <li>Refresh 는 서버(DB)에도 해시로 저장 → 로그아웃 시 삭제하여 재발급 차단</li>
 * </ul>
 *
 * <p><b>데모용:</b> 사용자 저장소가 인메모리 {@link #users}. 실제로는
 * {@code MemberRepository.findByUsername(...)} + 엔티티 해시 비밀번호 비교로 교체.
 * (경로 {@code /api/v1/auth/**} 는 SecurityConfig 에서 permitAll)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final CookieUtil cookieUtil;

    // ===== 데모용 인메모리 유저 (실제로는 DB) =====
    private final Map<String, DemoUser> users = new ConcurrentHashMap<>();

    @PostConstruct
    void seedDemoUsers() {
        users.put("user", new DemoUser(1L, "user", passwordEncoder.encode("password"), "ROLE_USER"));
        users.put("admin", new DemoUser(2L, "admin", passwordEncoder.encode("password"), "ROLE_ADMIN"));
    }

    // ============================================================
    // 로그인 — Access(body) + Refresh(HttpOnly 쿠키 + DB 저장)
    // ============================================================
    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(
        @Valid @RequestBody LoginRequest request,
        HttpServletResponse response
    ) {
        DemoUser user = users.get(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.password())) {
            // 존재 여부를 노출하지 않기 위해 동일한 코드로 응답
            throw new BusinessException(AuthExceptionCode.INVALID_CREDENTIALS);
        }

        String accessToken = jwtProvider.createAccessToken(user.id(), user.username(), user.role());
        String refreshToken = jwtProvider.createRefreshToken(user.id(), user.username(), user.role());

        long refreshSeconds = jwtProperties.refreshTokenValiditySeconds();
        refreshTokenService.save(user.id(), refreshToken, Instant.now().plusSeconds(refreshSeconds));
        addRefreshCookie(response, refreshToken, refreshSeconds);

        return ApiResponse.success(new TokenResponse(accessToken, "Bearer"));
    }

    // ============================================================
    // 재발급 — 쿠키의 Refresh 로 새 Access
    // ============================================================
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(
        @CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(AuthExceptionCode.TOKEN_MISSING);
        }

        // 1) 서명·만료 검증 (실패 시 GlobalExceptionHandler 가 JwtException 처리 → 401)
        Claims claims = jwtProvider.parseRefresh(refreshToken);
        Long userId = Long.valueOf(claims.getSubject());
        String username = claims.get("username", String.class);
        String role = claims.get("role", String.class);

        // 2) 서버에 저장된 Refresh 와 대조 (로그아웃/폐기 여부)
        refreshTokenService.verify(userId, refreshToken);

        // 3) 새 Access 발급
        String newAccess = jwtProvider.createAccessToken(userId, username, role);

        // 4) 회전이 켜져 있으면 Refresh 도 교체
        if (jwtProperties.refreshRotation()) {
            String newRefresh = jwtProvider.createRefreshToken(userId, username, role);
            long refreshSeconds = jwtProperties.refreshTokenValiditySeconds();
            refreshTokenService.save(userId, newRefresh, Instant.now().plusSeconds(refreshSeconds));
            addRefreshCookie(response, newRefresh, refreshSeconds);
        }

        return ApiResponse.success(new TokenResponse(newAccess, "Bearer"));
    }

    // ============================================================
    // 로그아웃 — DB의 Refresh 삭제 + 쿠키 제거
    // ============================================================
    @PostMapping("/logout")
    public ApiResponse<Void> logout(
        @CookieValue(value = CookieUtil.REFRESH_TOKEN_COOKIE, required = false) String refreshToken,
        HttpServletResponse response
    ) {
        if (refreshToken != null && !refreshToken.isBlank()) {
            try {
                Long userId = Long.valueOf(jwtProvider.parseRefresh(refreshToken).getSubject());
                refreshTokenService.deleteByUserId(userId);
            } catch (RuntimeException ignored) {
                // 이미 만료/위조된 토큰이면 서버엔 지울 게 없음 → 쿠키만 제거
            }
        }
        response.addHeader(HttpHeaders.SET_COOKIE, cookieUtil.clearRefreshCookie().toString());
        return ApiResponse.noContentSuccess();
    }

    // ------------------------------------------------------------
    private void addRefreshCookie(HttpServletResponse response, String token, long seconds) {
        response.addHeader(
            HttpHeaders.SET_COOKIE,
            cookieUtil.createRefreshCookie(token, Duration.ofSeconds(seconds)).toString());
    }

    // ===== 요청/응답 DTO =====
    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {}

    public record TokenResponse(String accessToken, String tokenType) {}

    private record DemoUser(Long id, String username, String password, String role) {}
}
