package com.jelly.boilerplate.global.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * JWT 생성 / 파싱 담당. (jjwt 0.12.x API)
 *
 * <p>Access / Refresh 를 <b>서로 다른 키</b>로 서명한다. Access 키가 노출돼도 Refresh 는 안전.
 *
 * <p>payload 구성 (Access·Refresh 동일)
 * <ul>
 *   <li>{@code iss} : 발급자 (jwt.issuer)</li>
 *   <li>{@code sub} : 회원 식별자(userId)</li>
 *   <li>{@code username}, {@code role} : 커스텀 클레임</li>
 *   <li>{@code iat}, {@code exp}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties properties;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    void init() {
        this.accessKey = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
        this.refreshKey = Keys.hmacShaKeyFor(properties.refreshSecret().getBytes(StandardCharsets.UTF_8));
    }

    // ============================================================
    // 생성
    // ============================================================

    /** 로그인 성공 시. API 호출용 Access Token. */
    public String createAccessToken(Long userId, String username, String role) {
        return build(userId, username, role, properties.accessTokenValiditySeconds(), accessKey);
    }

    /** 로그인 성공 시. Access 재발급용 Refresh Token (HttpOnly 쿠키에 담김). */
    public String createRefreshToken(Long userId, String username, String role) {
        return build(userId, username, role, properties.refreshTokenValiditySeconds(), refreshKey);
    }

    private String build(Long userId, String username, String role, long validitySeconds, SecretKey key) {
        Instant now = Instant.now();
        return Jwts.builder()
            .issuer(properties.issuer())
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(validitySeconds)))
            .signWith(key)
            .compact();
    }

    // ============================================================
    // 파싱 / 검증
    // ============================================================

    /**
     * Access Token 검증 + 클레임 반환.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료
     * @throws io.jsonwebtoken.JwtException 서명 불일치 / 형식 오류 / issuer 불일치
     * @throws IllegalArgumentException null / 빈 문자열
     */
    public Claims parse(String token) {
        return parseWith(token, accessKey);
    }

    /** Refresh Token 검증 + 클레임 반환. 예외 종류는 {@link #parse} 와 동일. */
    public Claims parseRefresh(String token) {
        return parseWith(token, refreshKey);
    }

    private Claims parseWith(String token, SecretKey key) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
