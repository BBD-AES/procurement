## 👩‍💻 어떤 작업을 하셨나요? (해당되는 항목에 체크)
- [ ] 🚀 기능 추가 (새로운 API, 도메인 로직 구현 등)
- [ ] 🐛 버그 수정
- [x] ♻️ 코드 리팩토링 (기능 변경 없이 코드 구조 개선)
- [ ] ⚙️ 환경 설정, CI/CD 구축 (GitHub Actions, 빌드 파일 등)
- [ ] 🗄️ 데이터베이스 스키마 변경 (Flyway SQL 등)
- [ ] 📝 문서화 (Swagger, README 수정)
- [ ] 🧪 테스트 코드 작성

> Closes #61

## 📝 상세 작업 내용

**문제**
`GlobalExceptionHandler`에 `@ExceptionHandler(ApiException.class)` 단 하나만 정의되어 있어 두 가지 갭이 있었습니다.

1. **catch-all 부재**: 서비스 계층의 일반 `RuntimeException`(예: `PurchaseOrderService`의 `IllegalStateException`)이 표준 바디 없이 스프링 기본 처리로 새어나가 **raw 500**으로 노출. 정의돼 있던 `ErrorCode.INTERNAL_ERROR(C999)`는 사용처가 없어 미활용.
2. **응답 스키마 불일치**: `@Valid` 검증 실패 시 발생하는 `MethodArgumentNotValidException`이 부모 `ResponseEntityExceptionHandler`의 기본 처리로 응답되어, `ApiException`이 만드는 표준 바디(`title`/`detail`/`timestamp`)와 달리 `title`·`timestamp`가 빠진 형태로 나감 → 클라이언트가 일관된 에러 스키마를 신뢰할 수 없음.

**해결 방향**
모든 에러 응답이 `ApiException`과 **동일한 스키마**(`title=코드`, `detail=메시지`, `timestamp`)를 갖도록 응답 바디 생성을 공통 팩토리로 추출하고, 핸들러 2개를 추가했습니다. 기존 `ApiException` 흐름은 건드리지 않아 **추가만 발생**하는 안전한 변경입니다.

**변경 내용**
- **신규 `ErrorResponseFactory`**: `ApiException`에 `private static`으로 있던 `createBody` 로직을 `public static create(ErrorCode)` 팩토리로 추출. 모든 에러 응답이 이 한 곳을 거쳐 스키마 일관성을 보장.
- **`ApiException` 수정**: 자체 `createBody` 제거 후 생성자에서 `ErrorResponseFactory.create()` 호출로 교체. **동작·응답 형태 변화 없음**(중복 제거).
- **`GlobalExceptionHandler` 핸들러 추가** (`@Slf4j` 부착):
  - `handleMethodArgumentNotValid` 오버라이드 → `@Valid` 실패를 `ErrorCode.INVALID_REQUEST(C001)`로 매핑.
  - `@ExceptionHandler(Exception.class)` catch-all → 미처리 예외를 `ErrorCode.INTERNAL_ERROR(C999)`로 변환. **내부 메시지·스택은 `log.error`로만 남기고 응답에 노출하지 않음**.

**동작 요약**
| 상황 | 변경 전 | 변경 후 |
|---|---|---|
| `ApiException` 발생 | 표준 바디 (C0xx 등) | 동일 (변화 없음) |
| 일반 `RuntimeException`(예: `IllegalStateException`) | raw 500 (바디 없음) | `500` + 표준 바디 `C999` |
| `@Valid` 검증 실패 | `title`/`timestamp` 없는 기본 응답 | `400` + 표준 바디 `C001` |

## 📸 테스트 결과 (선택)

- 변경 파일은 소스 3개(`ErrorResponseFactory` 신규, `ApiException`·`GlobalExceptionHandler` 수정)이며 **기존 테스트 갱신은 불필요**.
  - 레포의 기존 테스트 2개(`PurchaseRequestedListenerIT`, `PurchaseOrderServiceIdempotencyTest`)는 에러 **응답 바디 스키마/HTTP 상태에 의존하지 않음**(Mockito `verify` 및 자바 객체 수준의 `ApiException.getErrorCode()` 검증만 수행)을 확인.
  - MockMvc + `jsonPath`로 응답 형태를 검증하는 통합 테스트는 현재 레포에 없음.

> 로컬 검증: `./gradlew build`

## ✅ 리뷰어에게 부탁할 점 & 고민했던 점

- **팩토리 추출 방식**: `ApiException.createBody`를 `public`으로 여는 대신, 책임이 명확한 별도 `ErrorResponseFactory`로 추출했습니다. `ApiException`·catch-all·검증 실패 세 경로가 같은 메서드를 공유해 스키마 드리프트를 원천 차단하려는 의도입니다.
- **보안(내부 정보 비노출)**: catch-all에서 예외 메시지·스택을 응답에 절대 싣지 않고 `C999` 고정 메시지만 반환, 상세는 서버 로그로만 남깁니다.
- **검증 핸들러 위치**: `@Valid` 처리는 별도 `@ExceptionHandler` 대신 부모 `ResponseEntityExceptionHandler.handleMethodArgumentNotValid`를 **오버라이드**해, 프레임워크의 다른 `MethodArgument*` 처리 흐름과 충돌 없이 일관되게 동작하도록 했습니다.
- **확장 여지**: 필요 시 `ConstraintViolationException`(쿼리/패스 파라미터 검증)도 동일하게 `C001`로 매핑하는 핸들러를 후속 추가 가능. 이번 PR 범위에는 미포함.
- **클라이언트 영향**: 그동안 raw 500/비표준 검증 응답에 의존하던 클라이언트가 있다면 새 표준 스키마(`title`/`detail`/`timestamp`)에 맞춰 갱신 필요.

## 🚨 핵심 셀프 체크리스트
- [x] `application-secret.yml` 등 민감한 환경 변수나 비밀번호가 커밋되지 않았나요?
- [x] 불필요한 콘솔 로그(`System.out.println` 등)나 주석을 모두 제거했나요?
- [ ] API 명세서(Swagger 등)에 변경 사항을 올바르게 반영했나요? → 공통 에러 응답 스키마(`C001` 검증 실패, `C999` 서버 오류)를 Swagger 에러 응답 예시에 반영 권장
