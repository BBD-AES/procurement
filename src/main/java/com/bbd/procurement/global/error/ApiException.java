package com.bbd.procurement.global.error;

import lombok.Getter;
import org.springframework.web.ErrorResponseException;

@Getter
public class ApiException extends ErrorResponseException {
    // 커스텀 detail 메시지 오버라이드는 팀 컨벤션상 사용하지 않음

   private final ErrorCode errorCode;

   public ApiException(ErrorCode errorCode) {
       super(errorCode.getHttpStatus(), ErrorResponseFactory.create(errorCode), null);
       this.errorCode = errorCode;
   }
}
