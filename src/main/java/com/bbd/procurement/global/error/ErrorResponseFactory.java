package com.bbd.procurement.global.error;

import org.springframework.http.ProblemDetail;

import java.time.OffsetDateTime;

/**
 * ErrorCode를 표준 ProblemDetail 응답 바디로 변환하는 공통 팩토리.
 *
 * 모든 에러 응답(ApiException, catch-all, 검증 실패 등)이 이 메서드를 통해
 * 동일한 스키마(title=코드, detail=메시지, timestamp)를 갖도록 보장한다.
 *
 * - status:    HTTP 상태 코드
 * - title:     비즈니스 에러 코드 식별자 (C001, C999 등)
 * - detail:    사용자에게 보여줄 메시지
 * - timestamp: 서버 발생 시각 (setProperty로 추가)
 */
public final class ErrorResponseFactory {

    private ErrorResponseFactory() {
    }

    public static ProblemDetail create(ErrorCode errorCode) {
        ProblemDetail body = ProblemDetail.forStatus(errorCode.getHttpStatus());
        body.setTitle(errorCode.getCode());
        body.setDetail(errorCode.getMessage());
        body.setProperty("timestamp", OffsetDateTime.now());
        return body;
    }
}
