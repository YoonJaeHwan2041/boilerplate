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
 * JWT(Access Token) 생성 / 파싱 담당. (jjwt 0.12.x API)
 *
 * <p>토큰 payload 구성
 * <ul>
 *   <li>{@code iss} : 발급자 (jwt.issuer)</li>
 *   <li>{@code sub} : 회원 식별자(userId)</li>
 *   <li>{@code username} : 로그인 아이디 (커스텀 클레임)</li>
 *   <li>{@code role} : 권한 문자열 "ROLE_USER" 등 (커스텀 클레임)</li>
 *   <li>{@code iat}, {@code exp}</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class JwtProvider {

    private final JwtProperties properties;

    /** HS256 서명 키. 원문 secret 을 바이트로 변환해 생성 (최소 32바이트 필요) */
    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    /** 로그인 성공 시 호출. Access Token 문자열을 만든다. */
    public String createAccessToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.accessTokenValiditySeconds());

        return Jwts.builder()
            .issuer(properties.issuer())
            .subject(String.valueOf(userId))
            .claim("username", username)
            .claim("role", role)
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .signWith(key)
            .compact();
    }

    /**
     * 토큰을 검증하고 클레임을 반환한다.
     *
     * @throws io.jsonwebtoken.ExpiredJwtException 만료
     * @throws io.jsonwebtoken.JwtException 서명 불일치 / 형식 오류 / issuer 불일치 등
     * @throws IllegalArgumentException 토큰이 null 또는 빈 문자열
     */
    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .requireIssuer(properties.issuer())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }
}
