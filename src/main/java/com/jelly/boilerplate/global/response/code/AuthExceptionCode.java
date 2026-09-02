package com.jelly.boilerplate.global.response.code;

import com.jelly.boilerplate.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum AuthExceptionCode implements ExceptionCode {

    // 000번대 - 인증(Authentication) 관련  → 대부분 401 Unauthorized
    AUTHENTICATION_FAILED("AUTH000", HttpStatus.UNAUTHORIZED, "인증에 실패했습니다."), // 인증 실패(원인 불명 포함)
    INVALID_CREDENTIALS("AUTH001", HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."), // 자격 증명 불일치
    ACCOUNT_LOCKED("AUTH002", HttpStatus.UNAUTHORIZED, "계정이 잠겨 있습니다. 관리자에게 문의하세요."), // 계정 잠김 (423 Locked 로 둬도 됨)
    ACCOUNT_DISABLED("AUTH003", HttpStatus.UNAUTHORIZED, "비활성화된 계정입니다."), // 계정 비활성화
    ACCOUNT_EXPIRED("AUTH005", HttpStatus.UNAUTHORIZED, "만료된 계정입니다."), // 계정/자격 증명 만료
    MULTI_FACTOR_REQUIRED("AUTH006", HttpStatus.UNAUTHORIZED, "추가 인증이 필요합니다."), // 2차 인증 필요

    // 100번대 - 권한(Authorization) 관련
    ACCESS_DENIED("AUTH100", HttpStatus.FORBIDDEN, "접근 권한이 없습니다."), // 인증은 됐으나 권한 부족 → 403
    SESSION_EXPIRED("AUTH101", HttpStatus.UNAUTHORIZED, "세션이 만료되었습니다. 다시 로그인해 주세요."), // 세션 만료

    // 200번대 - 토큰(Token) 관련  → 재로그인/토큰 재발급 유도, 401
    TOKEN_MISSING("AUTH200", HttpStatus.UNAUTHORIZED, "인증 토큰이 필요합니다."), // 토큰 누락
    TOKEN_INVALID("AUTH201", HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다."), // 서명 불일치/형식 오류
    TOKEN_EXPIRED("AUTH202", HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."); // 만료

    private final String code;
    private final HttpStatus status;
    private final String message;
}
