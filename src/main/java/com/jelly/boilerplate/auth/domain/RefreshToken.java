package com.jelly.boilerplate.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 서버에 저장하는 Refresh Token.
 *
 * <p>목적: 발급한 Refresh 를 <b>서버가 무효화</b>할 수 있게 하기 위함
 * (로그아웃 / 비밀번호 변경 / 계정 정지).
 *
 * <ul>
 *   <li>{@code userId} 1명 = 1행 (deviceId 없음 → 한 계정은 한 곳에서만 로그인 유지.
 *       다른 기기에서 로그인하면 이 행이 갱신되어 기존 Refresh 는 무효화됨)</li>
 *   <li>{@code tokenHash} : 토큰 원문이 아니라 SHA-256 해시(hex, 64자). DB 유출돼도 원문 못 얻음</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
    name = "refresh_token",
    uniqueConstraints = @UniqueConstraint(name = "uk_refresh_token_user", columnNames = "user_id"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public RefreshToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    /** 재로그인 / 회전 시 값 교체 (JPA 변경감지로 UPDATE) */
    public void rotate(String tokenHash, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return expiresAt.isBefore(Instant.now());
    }

    public boolean matches(String tokenHash) {
        return this.tokenHash.equals(tokenHash);
    }
}
