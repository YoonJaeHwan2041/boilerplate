package com.jelly.boilerplate.global.security;

import java.util.Collection;
import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * SecurityContext 에 저장되는 인증 주체(principal).
 *
 * <p>컨트롤러에서 {@code @AuthenticationPrincipal CustomUserDetails principal} 로 꺼내 쓴다.
 * JWT 필터가 토큰 클레임으로 이 객체를 만들 때는 {@code password} 가 null 이다(비밀번호는 로그인 시에만 필요).
 */
@Getter
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {

    private final Long id;
    private final String username;
    private final String password; // 로그인 인증 시에만 채워짐. JWT 로 복원할 땐 null
    private final String role;      // 예: "ROLE_USER", "ROLE_ADMIN"

    public boolean isAdmin() {
        return "ROLE_ADMIN".equals(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    // 계정 상태 플래그 — 필요해지면 도메인 규칙에 맞게 교체
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
