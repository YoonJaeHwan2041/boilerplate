package com.jelly.boilerplate.global.response;

import lombok.*;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ApiResponse<T> {

    private static final String DEFAULT_CODE = "0000";

    private boolean success;
    private String code;
    private String message;
    private T data;

    // 성공 , 데이터만 보냄
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder().success(true).code(DEFAULT_CODE).message("").data(data).build();
    }
    //성공 , 데이터와 메시지 보냄
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder().success(true).code(DEFAULT_CODE).message(message).data(data).build();
    }
    //성공, 데이터와 메시지 보내지 않음
    public static ApiResponse<Void> noContentSuccess() {
        return ApiResponse.<Void>builder().success(true).code(DEFAULT_CODE).message("").data(null).build();
    }
    //성공 메시지만 보냄
    public static ApiResponse<Void> noContentSuccess(String message) {
        return ApiResponse.<Void>builder().success(true).code(DEFAULT_CODE).message(message).data(null).build();
    }
    //에러 메시지, 데이터, 코드를 보냄
    public static <T> ApiResponse<T> error(String message, String code, T data){
        return ApiResponse.<T>builder().success(false).code(code).message(message).data(data).build();
    }
    //에러 메시지, 코드 보냄
    public static ApiResponse<Void> error(String message, String code){
        return ApiResponse.<Void>builder().success(false).code(code).message(message).data(null).build();
    }

}
