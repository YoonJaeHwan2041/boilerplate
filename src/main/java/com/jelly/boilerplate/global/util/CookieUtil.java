package com.jelly.boilerplate.global.util;

import com.jelly.boilerplate.global.security.AuthCookieProperties;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Refresh Token 쿠키 생성/삭제 헬퍼.
 *
 * <p>{@code jakarta.servlet.http.Cookie}(구식)는 SameSite 설정을 못 하므로
 * 스프링의 {@link ResponseCookie} 를 쓴다. 반환값을
 * {@code response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString())} 로 내려보낸다.
 */
@Component
@RequiredArgsConstructor
public class CookieUtil {

    public static final String REFRESH_TOKEN_COOKIE = "refreshToken";

    private final AuthCookieProperties properties;

    /** Refresh Token 을 담은 쿠키 (로그인 / 회전 시 발급) */
    public ResponseCookie createRefreshCookie(String value, Duration maxAge) {
        return base(value).maxAge(maxAge).build();
    }

    /** 로그아웃 시: 같은 이름 + Max-Age=0 쿠키로 덮어써서 브라우저에서 삭제 */
    public ResponseCookie clearRefreshCookie() {
        return base("").maxAge(0).build();
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
            .httpOnly(true)
            .secure(properties.secure())
            .sameSite(properties.sameSite())
            .path(properties.path());
    }
}
