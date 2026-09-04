package com.jelly.boilerplate.auth.domain;

import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByUserId(Long userId);

    void deleteByUserId(Long userId);

    /** 만료된 행 일괄 삭제 (스케줄러에서 호출) */
    void deleteByExpiresAtBefore(Instant time);
}
