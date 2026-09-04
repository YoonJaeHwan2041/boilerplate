package com.jelly.boilerplate.global.exception;

import com.jelly.boilerplate.global.response.ApiResponse;
import com.jelly.boilerplate.global.response.code.AuthExceptionCode;
import com.jelly.boilerplate.global.response.code.FileExceptionCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ============================================================
    // 1) 기본 경로 - 우리가 의도적으로 던지는 모든 비즈니스 예외
    //    (AUTH / REQUEST / ... 어느 도메인 코드든 ExceptionCode 계약만 지키면 여기서 처리)
    //    JwtAuthenticationFilter 가 HandlerExceptionResolver 로 넘긴 토큰 예외도 여기로 온다.
    // ============================================================
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException e) {
        ExceptionCode ec = e.getExceptionCode();
        logByStatus(ec, e);
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(e.getMessage(), ec.getCode()));
    }

    // ============================================================
    // 2) 요청 값 검증 실패 (@Valid @RequestBody)
    // ============================================================
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("[VALIDATION] {}", message);
        return ResponseEntity.badRequest()
                .body(ApiResponse.error(message, "COMMON_INVALID_INPUT"));
    }

    // ============================================================
    // 2-1) 업로드 용량 초과 (multipart max-file-size)
    // ============================================================
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<Void>> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        FileExceptionCode ec = FileExceptionCode.FILE_TOO_LARGE;
        log.warn("[{}] {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getMessage(), ec.getCode()));
    }

    // ============================================================
    // 2-2) JWT 파싱 예외 (주로 /refresh 에서 쿠키의 Refresh Token 검증 실패)
    //      필터 경로는 JwtAuthenticationFilter 가 자체 처리하므로 여기로 안 옴.
    // ============================================================
    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleExpiredJwt(ExpiredJwtException e) {
        AuthExceptionCode ec = AuthExceptionCode.TOKEN_EXPIRED;
        log.warn("[{}] {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getMessage(), ec.getCode()));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ApiResponse<Void>> handleJwt(JwtException e) {
        AuthExceptionCode ec = AuthExceptionCode.TOKEN_INVALID;
        log.warn("[{}] {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getMessage(), ec.getCode()));
    }

    // ============================================================
    // 3) 스프링 시큐리티가 던지는 인증/인가 예외
    //    - 필터 단계에서 터지는 건 EntryPoint / AccessDeniedHandler 가 처리한다.
    //    - 여기서 잡히는 건 주로 메서드 시큐리티(@PreAuthorize) 등 컨트롤러 호출 구간의 예외.
    // ============================================================
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuthentication(AuthenticationException e) {
        AuthExceptionCode ec = AuthExceptionCode.AUTHENTICATION_FAILED;
        log.warn("[{}] AuthenticationException: {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getMessage(), ec.getCode()));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException e) {
        AuthExceptionCode ec = AuthExceptionCode.ACCESS_DENIED;
        log.warn("[{}] AccessDeniedException: {}", ec.getCode(), e.getMessage());
        return ResponseEntity.status(ec.getStatus())
                .body(ApiResponse.error(ec.getMessage(), ec.getCode()));
    }

    // ============================================================
    // 4) 최후 방어선 - 예상하지 못한 모든 예외
    // ============================================================
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnknown(Exception e) {
        log.error("[UNHANDLED] 처리되지 않은 예외", e); // 스택트레이스 전체 로깅
        return ResponseEntity.internalServerError()
                .body(ApiResponse.error("서버 오류가 발생했습니다.", "COMMON_INTERNAL_ERROR")); // 원본 메시지 노출 X
    }

    // ------------------------------------------------------------
    /** 5xx 는 서버 잘못이라 스택트레이스까지, 4xx 는 클라이언트 잘못이라 메시지만 로깅 */
    private void logByStatus(ExceptionCode ec, Exception e) {
        if (ec.getStatus().is5xxServerError()) {
            log.error("[{}] {}", ec.getCode(), e.getMessage(), e);
        } else {
            log.warn("[{}] {}", ec.getCode(), e.getMessage());
        }
    }
}
