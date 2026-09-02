package com.jelly.boilerplate.global.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인별 에러 코드 enum(AuthExceptionCode, RequestExceptionCode ...)이 공통으로 구현하는 계약.
 *
 * <p>이 인터페이스 덕분에 BusinessException 하나 + 핸들러 하나로 모든 도메인의 에러 코드를 처리할 수 있다.
 * (코드 문자열 / HTTP 상태 / 기본 메시지를 코드 enum 이 직접 들고 있으므로 핸들러가 얇아진다.)
 */
public interface ExceptionCode {

    /** 클라이언트-서버 계약용 식별자. 예: "AUTH000" */
    String getCode();

    /** 이 에러에 대응하는 HTTP 응답 상태 */
    HttpStatus getStatus();

    /** 사용자에게 보여줄 기본 메시지 */
    String getMessage();
}
