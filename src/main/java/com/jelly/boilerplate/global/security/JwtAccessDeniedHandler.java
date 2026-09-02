package com.jelly.boilerplate.global.security;

import com.jelly.boilerplate.global.exception.BusinessException;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;

/**
 * 인증은 됐지만 권한이 부족할 때 호출된다(예: ROLE_USER 가 관리자 전용 API 호출).
 *
 * <p>필터 체인에서 발생하는 403 을 {@link HandlerExceptionResolver} 로 넘겨
 * GlobalExceptionHandler 가 컨트롤러 에러와 같은 {@code ApiResponse} 포맷으로 내려준다.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final HandlerExceptionResolver resolver;

    public JwtAccessDeniedHandler(
        @Qualifier("handlerExceptionResolver") HandlerExceptionResolver resolver
    ) {
        this.resolver = resolver;
    }

    @Override
    public void handle(
        HttpServletRequest request,
        HttpServletResponse response,
        AccessDeniedException accessDeniedException
    ) {
        resolver.resolveException(
            request, response, null,
            new BusinessException(AuthExceptionCode.ACCESS_DENIED)
        );
    }
}
