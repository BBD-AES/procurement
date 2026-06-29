package com.bbd.procurement.global.error;


import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 전역 예외처리
 * 사용법 :
 * throw new ApiException(HttpStatus.NOT_FOUND, "404", "NOT_FOUND", "유저를 찾을 수 없습니다");
 *
 *  HttpServletRequest?
 *  현재 들어온 HTTP 요청 자체를 담고있는 객체임
 *  요청 URL, HTTP method, 헤더, 쿼리 파라미터, 클라이언트 정보 조회 가능
 *
 *  모든 에러 응답은 ErrorResponseFactory를 통해 동일한 스키마
 *  (title=코드, detail=메시지, timestamp)로 통일된다.
 */

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ProblemDetail> handleApiException(ApiException e) {
        return ResponseEntity.status(e.getStatusCode()).body(e.getBody());
    }

    /**
     * @Valid 검증 실패(MethodArgumentNotValidException)를 INVALID_REQUEST(C001)로 매핑한다.
     * 부모 ResponseEntityExceptionHandler의 기본 처리를 오버라이드하여
     * ApiException과 동일한 응답 스키마로 통일한다.
     */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatusCode status,
            WebRequest request) {
        ErrorCode errorCode = ErrorCode.INVALID_REQUEST;
        ProblemDetail body = ErrorResponseFactory.create(errorCode);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /**
     * 낙관적 락 충돌(OptimisticLockingFailureException)을 409 CONCURRENT_MODIFICATION으로 매핑한다.
     * 같은 PO를 동시에 전이(예: complete 더블클릭)하면 둘째 트랜잭션이 커밋 시점에 실패하는데,
     * 이 예외는 @Transactional 서비스 메서드가 반환된 뒤 커밋에서 터지므로 서비스 내부 try/catch로는
     * 잡히지 않는다. 전역 핸들러에서 처리해야 신뢰성 있게 409로 응답된다.
     */
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ProblemDetail> handleOptimisticLock(OptimisticLockingFailureException e) {
        log.warn("Optimistic lock conflict", e);
        ErrorCode errorCode = ErrorCode.CONCURRENT_MODIFICATION;
        ProblemDetail body = ErrorResponseFactory.create(errorCode);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

    /**
     * 처리되지 않은 모든 예외(catch-all)를 INTERNAL_ERROR(C999)로 변환한다.
     * 서버 내부 메시지/스택은 응답에 노출하지 않고 로깅만 한다.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleException(Exception e) {
        log.error("Unhandled exception", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        ProblemDetail body = ErrorResponseFactory.create(errorCode);
        return ResponseEntity.status(errorCode.getHttpStatus()).body(body);
    }

}
