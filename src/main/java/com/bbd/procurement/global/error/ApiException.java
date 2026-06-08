package com.bbd.procurement.global.error;

import lombok.Getter;
import org.springframework.http.ProblemDetail;
import org.springframework.web.ErrorResponseException;

import java.time.OffsetDateTime;

@Getter
public class ApiException extends ErrorResponseException {
    // 커스텀 detail 메시지 오버라이드는 팀 컨벤션상 사용하지 않음

   private final ErrorCode errorCode;

   public ApiException(ErrorCode errorCode) {
       super(errorCode.getHttpStatus(), createBody(errorCode), null);
       this.errorCode = errorCode;
   }

    /**
     * ErrorCode를 표준 ProblemDetail 응답으로 변환한다.
     *
     * - status:    HTTP 상태 코드
     * - title:     비즈니스 에러 코드 식별자 (V001, T001 등)
     * - detail:    사용자에게 보여줄 메시지
     * - timestamp: 서버 발생 시각 (실무 유용 필드, setProperty로 추가)
     */
    private static ProblemDetail createBody(ErrorCode errorCode) {
        ProblemDetail body = ProblemDetail.forStatus(errorCode.getHttpStatus());
        body.setTitle(errorCode.getCode());
        body.setDetail(errorCode.getMessage());
        body.setProperty("timestamp", OffsetDateTime.now());
        return body;
    }
}