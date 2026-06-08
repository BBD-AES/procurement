package com.bbd.procurement.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "C001", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "C002", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "C003", "권한이 없습니다."),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C999", "서버 오류가 발생했습니다."),

    // 인증·인가
    AUTH_HEADER_REQUIRED(HttpStatus.UNAUTHORIZED, "AUTH001", "인증 헤더가 필요합니다."),
    AUTH_ROLE_INVALID(HttpStatus.BAD_REQUEST, "AUTH002", "알 수 없는 역할입니다."),

    // Vendor
    VENDOR_NOT_FOUND(HttpStatus.NOT_FOUND, "V001", "공급사를 찾을 수 없습니다."),
    VENDOR_CODE_DUPLICATED(HttpStatus.CONFLICT, "V002", "이미 존재하는 공급사 코드입니다."),
    VENDOR_INACTIVE(HttpStatus.BAD_REQUEST, "V003", "비활성화된 공급사입니다."),
    VENDOR_CODE_INVALID(HttpStatus.BAD_REQUEST, "V004", "공급사 코드 형식이 올바르지 않습니다."),
    VENDOR_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "V005", "공급사명은 필수입니다.");


    private final HttpStatus httpStatus;
    private final String code;
    private final String message;
}
