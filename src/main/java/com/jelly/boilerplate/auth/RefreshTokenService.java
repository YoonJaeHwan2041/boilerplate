package com.jelly.boilerplate.auth;

import com.jelly.boilerplate.auth.domain.RefreshToken;
import com.jelly.boilerplate.auth.domain.RefreshTokenRepository;
import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh Token 서버 저장소 로직.
 *
 * <p>원문이 아니라 SHA-256 해시로 저장/비교한다.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository repository;

    /** 로그인 / 회전 시: 유저의 Refresh 를 저장(있으면 교체). */
    @Transactional
    public void save(Long userId, String rawToken, Instant expiresAt) {
        String hash = sha256(rawToken);
        repository.findByUserId(userId).ifPresentOrElse(
            existing -> existing.rotate(hash, expiresAt),
            () -> repository.save(new RefreshToken(userId, hash, expiresAt)));
    }

    /**
     * {@code /refresh} 시: 쿠키로 온 Refresh 가 서버에 저장된 것과 일치하는지 확인.
     *
     * @throws BusinessException 저장된 게 없거나(로그아웃/폐기), 만료됐거나, 해시 불일치
     */
    @Transactional(readOnly = true)
    public void verify(Long userId, String rawToken) {
        RefreshToken stored = repository.findByUserId(userId)
            .orElseThrow(() -> new BusinessException(AuthExceptionCode.TOKEN_INVALID));

        if (stored.isExpired()) {
            throw new BusinessException(AuthExceptionCode.TOKEN_EXPIRED);
        }
        if (!stored.matches(sha256(rawToken))) {
            throw new BusinessException(AuthExceptionCode.TOKEN_INVALID);
        }
    }

    /** 로그아웃 / 비밀번호 변경 등: 유저의 Refresh 제거 → 이후 재발급 불가. */
    @Transactional
    public void deleteByUserId(Long userId) {
        repository.deleteByUserId(userId);
    }

    private String sha256(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 미지원", e); // 표준 JDK에 항상 존재
        }
    }
}
