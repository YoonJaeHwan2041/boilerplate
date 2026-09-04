package com.jelly.boilerplate.global.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yaml 의 {@code jwt.*} 값을 바인딩한다.
 *
 * <pre>
 * jwt:
 *   secret: ...                          # Access Token HS256 서명 키 (원문, 최소 32바이트)
 *   issuer: boilerplate                  # 토큰 발급자(iss). 검증 시에도 사용
 *   access-token-validity-seconds: 1800  # Access 만료 (30분)
 *   refresh-secret: ...                  # Refresh Token 전용 서명 키 (Access 와 다른 키)
 *   refresh-token-validity-seconds: 1209600  # Refresh 만료 (14일)
 *   refresh-rotation: false              # /refresh 때 Refresh 도 재발급할지 (기본 off)
 * </pre>
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(
    String secret,
    String issuer,
    long accessTokenValiditySeconds,
    String refreshSecret,
    long refreshTokenValiditySeconds,
    boolean refreshRotation
) {}
