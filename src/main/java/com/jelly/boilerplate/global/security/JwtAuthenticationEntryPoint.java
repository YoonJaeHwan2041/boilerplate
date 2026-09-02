package com.jelly.boilerplate.global.security;

import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증이 안 된 상태로 보호된 자원에 접근했을 때 호출된다(토큰 미첨부 등).
 *
 * <p>시큐리티 필터 체인 안에서 발생하므로 {@code @RestControllerAdvice} 가 직접 잡지는 못한다.
 * 대신 {@link HandlerExceptionResolver} 로 예외를 넘겨 GlobalExceptionHandler 가
 * 평소와 동일한 {@code ApiResponse} 포맷(401)으로 응답하게 한다.
 * (토큰 만료/위조처럼 "토큰은 있으나 잘못된" 경우는 JwtAuthenticationFilter 가 먼저 처리한다.)
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final HandlerExceptionResolver resolver;

    public JwtAuthenticationEntryPoint(
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.resolver = resolver;
    }

    @Override
    public void commence(
        HttpServletRequest request,
        HttpServletResponse response,
        AuthenticationException authException
    ) {
        resolver.resolveException(
            request, response, null,
            new BusinessException(AuthExceptionCode.TOKEN_MISSING)
        );
    }
}
