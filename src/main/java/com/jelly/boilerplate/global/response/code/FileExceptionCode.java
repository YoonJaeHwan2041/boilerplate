package com.jelly.boilerplate.global.response.code;

import com.jelly.boilerplate.global.exception.ExceptionCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum FileExceptionCode implements ExceptionCode {

    // 000번대 - 요청/입력 문제
    EMPTY_FILE("FILE000", HttpStatus.BAD_REQUEST, "업로드할 파일이 없습니다."),
    INVALID_FILE_TYPE("FILE001", HttpStatus.BAD_REQUEST, "허용되지 않은 파일 형식입니다."),
    FILE_TOO_LARGE("FILE002", HttpStatus.CONTENT_TOO_LARGE, "파일 크기가 허용 범위를 초과했습니다."),

    // 100번대 - 스토리지(S3) 처리 실패
    UPLOAD_FAILED("FILE100", HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    DELETE_FAILED("FILE101", HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제에 실패했습니다.");

    private final String code;
    private final HttpStatus status;
    private final String message;
}
