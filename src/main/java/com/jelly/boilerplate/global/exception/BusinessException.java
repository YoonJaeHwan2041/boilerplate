package com.jelly.boilerplate.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 의도적으로 던지는 예외.
 *
 * <p>대부분의 에러는 이 예외 하나로 처리한다. 상태 코드 / 응답 코드 / 메시지는 전달한 {@link ExceptionCode}가 들고 있으므로
 * throw 하는 쪽은 상황에 맞는 코드만 고르면 된다.
 *
 * <pre>
 *   throw new BusinessException(AuthExceptionCode.TOKEN_EXPIRED);
 *   throw new BusinessException(AuthExceptionCode.INVALID_CREDENTIALS, "5회 이상 실패하여 잠겼습니다.");
 * </pre>
 *
 * <p>RuntimeException 계열이라 메서드 시그니처에 throws 를 붙일 필요가 없고, 스프링 트랜잭션도 기본 롤백된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    private final ExceptionCode exceptionCode;

    public BusinessException(ExceptionCode exceptionCode) {
        super(exceptionCode.getMessage());
        this.exceptionCode = exceptionCode;
    }

    /** 기본 메시지 대신 상황에 맞는 메시지를 내보내고 싶을 때 */
    public BusinessException(ExceptionCode exceptionCode, String message) {
        super(message);
        this.exceptionCode = exceptionCode;
    }
}
