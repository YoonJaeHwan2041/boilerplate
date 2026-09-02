package com.jelly.boilerplate.global.jwt;

import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import com.jelly.boilerplate.global.security.CustomUserDetails;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 요청 1건마다 한 번 실행되며, {@code Authorization: Bearer <token>} 헤더를 읽어 인증을 세팅한다.
 *
 * <ul>
 *   <li>토큰 없음 → 그냥 통과 (permitAll 경로면 그대로 처리, 보호 경로면 EntryPoint 가 401)</li>
 *   <li>토큰 유효 → {@link CustomUserDetails} 를 만들어 SecurityContext 에 저장</li>
 *   <li>토큰 만료/위조 → {@link HandlerExceptionResolver} 로 넘겨 GlobalExceptionHandler 가
 *       평소와 동일한 {@code ApiResponse} 포맷으로 응답</li>
 * </ul>
 *
 * <p>{@code UsernamePasswordAuthenticationFilter} 앞에 등록된다(SecurityConfig).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtProvider jwtProvider;
    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationFilter(
        JwtProvider jwtProvider,
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.jwtProvider = jwtProvider;
        this.resolver = resolver;
    }

    /** 공개 경로는 토큰 검사 자체를 건너뛴다(만료 토큰이 남아 있어도 로그인/문서는 되도록). */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/api/v1/auth/")
            || path.startsWith("/swagger-ui")
            || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request, HttpServletResponse response, FilterChain filterChain
    ) throws ServletException, IOException {

        String token = resolveToken(request);
        if (token == null) {
            filterChain.doFilter(request, response); // 익명으로 계속 진행
            return;
        }

        try {
            Claims claims = jwtProvider.parse(token);

            CustomUserDetails principal = new CustomUserDetails(
                Long.valueOf(claims.getSubject()),
                claims.get("username", String.class),
                null,
                claims.get("role", String.class)
            );

            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);

            filterChain.doFilter(request, response);

        } catch (ExpiredJwtException e) {
            reject(request, response, AuthExceptionCode.TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException e) {
            reject(request, response, AuthExceptionCode.TOKEN_INVALID);
        }
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, AuthExceptionCode code) {
        SecurityContextHolder.clearContext();
        // 필터 단계 예외를 @RestControllerAdvice 로 위임 → 응답 포맷 통일
        resolver.resolveException(request, response, null, new BusinessException(code));
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }
        String token = header.substring(PREFIX.length()).trim();
        if (token.isEmpty() || "null".equals(token) || "undefined".equals(token)) {
            return null;
        }
        return token;
    }
}
