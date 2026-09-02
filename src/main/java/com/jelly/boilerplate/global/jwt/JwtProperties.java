package com.jelly.boilerplate.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 {@code jwt.*} 값을 바인딩한다.
 *
 * <pre>
 * jwt:
 *   secret: ...                       # HS256 서명 키 (원문 문자열, 최소 32바이트)
 *   issuer: boilerplate               # 토큰 발급자(iss). 검증 시에도 사용
 *   access-token-validity-seconds: 1800
 * </pre>
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    String issuer,
    long accessTokenValiditySeconds
) {}
