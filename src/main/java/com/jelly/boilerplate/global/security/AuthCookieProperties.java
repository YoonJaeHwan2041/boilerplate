package com.jelly.boilerplate.global.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Refresh Token 을 담는 쿠키의 속성. ({@code app.cookie.*})
 *
 * <pre>
 * app:
 *   cookie:
 *     secure: false          # 로컬(http) false / 운영(https) true
 *     same-site: Lax         # 프론트·백이 같은 site 면 Lax(또는 Strict).
 *                            # 완전히 다른 도메인 + https 면 None (그땐 secure:true 필수)
 *     path: /api/v1/auth     # 이 경로 요청에만 쿠키가 실림 (refresh/logout)
 * </pre>
 */
@ConfigurationProperties(prefix = "app.cookie")
public record AuthCookieProperties(
    boolean secure,
    String sameSite,
    String path
) {}
