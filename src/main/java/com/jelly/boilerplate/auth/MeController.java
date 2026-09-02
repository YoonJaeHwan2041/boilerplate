package com.jelly.boilerplate.auth;

import com.jelly.boilerplate.global.response.ApiResponse;
import com.jelly.boilerplate.global.security.CustomUserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JWT 인증이 실제로 걸리는지 확인하는 데모.
 *
 * <p>{@code /api/v1/members/**} 는 permitAll 이 아니므로 유효한 토큰이 있어야 200,
 * 없으면 {@code JwtAuthenticationEntryPoint} 가 401, 권한이 모자라면
 * {@code JwtAccessDeniedHandler} 가 403 을 같은 {@code ApiResponse} 포맷으로 반환한다.
 */
@RestController
@RequestMapping("/api/v1/members")
public class MeController {

    /** 토큰만 유효하면 접근 가능 — SecurityContext 의 principal 을 그대로 돌려줌 */
    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal CustomUserDetails principal) {
        return ApiResponse.success(
            new MeResponse(principal.getId(), principal.getUsername(), principal.getRole())
        );
    }

    /** 관리자 전용 — ROLE_ADMIN 이 아니면 403 (메서드 시큐리티 동작 확인용) */
    @GetMapping("/admin-only")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<String> adminOnly() {
        return ApiResponse.success("관리자만 볼 수 있는 데이터");
    }

    public record MeResponse(Long id, String username, String role) {}
}
