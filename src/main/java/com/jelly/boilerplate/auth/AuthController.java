package com.jelly.boilerplate.auth;

import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.jwt.JwtProvider;
import com.jelly.boilerplate.global.response.ApiResponse;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import jakarta.annotation.PostConstruct;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 로그인 → Access Token 발급 데모.
 *
 * <p><b>데모용:</b> 사용자 저장소가 인메모리 {@link #users} 다. 실제 프로젝트에서는
 * {@code MemberRepository.findByUsername(...)} 조회 + 엔티티의 해시 비밀번호와 비교로 교체할 것.
 * (이 컨트롤러 경로 {@code /api/v1/auth/**} 는 SecurityConfig 에서 permitAll)
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    // ===== 데모용 인메모리 유저 (실제로는 DB) =====
    private final Map<String, DemoUser> users = new ConcurrentHashMap<>();

    @PostConstruct
    void seedDemoUsers() {
        users.put("user", new DemoUser(1L, "user", passwordEncoder.encode("password"), "ROLE_USER"));
        users.put("admin", new DemoUser(2L, "admin", passwordEncoder.encode("password"), "ROLE_ADMIN"));
    }

    @PostMapping("/login")
    public ApiResponse<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        DemoUser user = users.get(request.username());
        if (user == null || !passwordEncoder.matches(request.password(), user.password())) {
            // 존재 여부를 노출하지 않기 위해 동일한 코드로 응답
            throw new BusinessException(AuthExceptionCode.INVALID_CREDENTIALS);
        }
        String accessToken = jwtProvider.createAccessToken(user.id(), user.username(), user.role());
        return ApiResponse.success(new TokenResponse(accessToken, "Bearer"));
    }

    // ===== 요청/응답 DTO =====
    public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password
    ) {}

    public record TokenResponse(String accessToken, String tokenType) {}

    private record DemoUser(Long id, String username, String password, String role) {}
}
