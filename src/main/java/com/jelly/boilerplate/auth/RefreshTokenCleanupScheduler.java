package com.jelly.boilerplate.auth;

import com.jelly.boilerplate.auth.domain.RefreshTokenRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 만료된 Refresh Token 행 정리.
 *
 * <p>RDB 는 TTL 자동삭제가 없어서 주기적으로 지워줘야 한다. (Redis 로 바꾸면 이 클래스는 불필요)
 * 주기는 {@code jwt.refresh-cleanup-cron} 으로 조정 (기본: 매일 04:00).
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RefreshTokenCleanupScheduler {

    private final RefreshTokenRepository repository;

    @Scheduled(cron = "${jwt.refresh-cleanup-cron:0 0 4 * * *}")
    @Transactional
    public void purgeExpired() {
        repository.deleteByExpiresAtBefore(Instant.now());
        log.info("[refresh-token] 만료된 리프레시 토큰 정리 완료");
    }
}
