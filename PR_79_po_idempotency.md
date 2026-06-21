## 👩‍💻 어떤 작업을 하셨나요? (해당되는 항목에 체크)
- [ ] 🚀 기능 추가 (새로운 API, 도메인 로직 구현 등)
- [x] 🐛 버그 수정
- [ ] ♻️ 코드 리팩토링 (기능 변경 없이 코드 구조 개선)
- [ ] ⚙️ 환경 설정, CI/CD 구축 (GitHub Actions, 빌드 파일 등)
- [x] 🗄️ 데이터베이스 스키마 변경 (Flyway SQL 등)
- [ ] 📝 문서화 (Swagger, README 수정)
- [x] 🧪 테스트 코드 작성

> Closes #79

## 📝 상세 작업 내용

**문제**
`POST /api/v1/purchase-orders`에 멱등키가 없어, 더블클릭/네트워크 재시도 시 동일 요청이 두 번 처리되어 **중복 PO**가 생성되고, 후속 `complete` → `StockInRequested` 발행으로 **재고가 이중 입고**되는 문제.

**해결 방향**
inventory의 예약(reservation) 멱등 패턴(`request_id` UNIQUE + `findByRequestId` + replay)을 PO 생성에 동일 적용. 사내에 이미 검증된 패턴을 그대로 차용했습니다.

**변경 내용**
- **DB (Flyway V14)**: `purchase_order`에 `request_id VARCHAR(64)` 컬럼 + `uq_purchase_order_request` UNIQUE 제약 추가. nullable이라 미전송 요청은 영향 없음(Postgres는 NULL을 UNIQUE 중복으로 보지 않음).
- **수신**: `RegisterPurchaseOrderRequest`/`RegisterPurchaseOrderCommand`에 `requestId` 추가(바디 방식). 프론트가 "발주 생성" 클릭당 UUID를 생성해 전송, 재시도 시 동일 값 유지.
- **영속 dedup**: `LoadPurchaseOrderPort.findByRequestId` 추가(포트/JpaRepository/Adapter).
- **서비스 로직**: `register()` 진입 시 `requestId`로 사전 조회 → 이미 있으면 기존 PO 반환(replay). 거의 동시에 들어와 사전 조회를 모두 통과한 경합(TOCTOU)은 DB UNIQUE가 두 번째 INSERT를 거부 → `DataIntegrityViolationException`을 잡아 `409 PO_DUPLICATE_REQUEST`로 응답.
- **에러코드**: `PO_DUPLICATE_REQUEST(409, "P007", "이미 접수된 주문입니다.")` 추가.

**동작 요약**
| 상황 | 결과 |
|---|---|
| 동일 `request_id` 시간차 재시도 | 사전 조회 hit → 기존 PO 반환(중복 생성 없음) |
| 동일 `request_id` 동시 더블클릭 | DB UNIQUE 차단 → `409 이미 접수된 주문입니다.` |
| `request_id` 미전송(레거시) | 사전 조회 건너뛰고 기존대로 생성 (하위 호환) |

## 📸 테스트 결과 (선택)

`PurchaseOrderServiceIdempotencyTest` (Mockito 단위 테스트, DB 불필요) 4케이스:
- `동일_requestId면_기존_PO를_반환하고_새로_생성하지_않는다`
- `requestId가_있고_기존_PO가_없으면_새로_생성한다`
- `requestId가_없으면_사전조회_없이_기존대로_생성한다`
- `동시경합으로_UNIQUE_위반시_409로_응답한다`

> 로컬 실행: `./gradlew test --tests '*PurchaseOrderServiceIdempotencyTest'`

## ✅ 리뷰어에게 부탁할 점 & 고민했던 점

- **헤더 vs 바디**: 결제 API 표준은 `Idempotency-Key` 헤더지만, 본 이슈가 "inventory 패턴 차용"을 명시해 사내 일관성(`request_id`)을 위해 **바디 방식**을 택했습니다. 헤더 선호 시 변경 가능.
- **catch에서 replay vs 409**: catch 안에서 재조회하면 이미 rollback-only로 마킹된 트랜잭션에서 `UnexpectedRollbackException`이 날 수 있어, **재조회 없이 409 응답**으로 처리했습니다(시간차 재시도는 사전 조회가 이미 흡수). "항상 200 + 동일 PO"가 계약상 필요하면 저장 트랜잭션 분리(REQUIRES_NEW) 방식으로 확장 가능.
- **선택 → 필수 2단계**: 이번 PR은 `request_id`를 **선택(nullable)**으로 둬 무중단 배포가 가능합니다. 프론트의 `request_id` 전송 배포가 확인되면 **후속 PR에서 필수(미전송 거부)**로 전환해 사각지대를 닫을 예정.
- **프론트 의존성**: 중복 차단은 프론트가 "클릭당 UUID 1개 생성 + 재시도 시 동일 값 유지"를 지켜야 실제로 작동합니다. 프론트 변경 PR 연동 필요.
- **범위**: 본 PR은 PO 생성(HIGH)만 다룹니다. 동일 갭이 `POST /work-orders`, `POST /vendors`에도 있으나 별도 이슈로 분리 예정.

## 🚨 핵심 셀프 체크리스트
- [x] `application-secret.yml` 등 민감한 환경 변수나 비밀번호가 커밋되지 않았나요?
- [x] 불필요한 콘솔 로그(`System.out.println` 등)나 주석을 모두 제거했나요?
- [ ] API 명세서(Swagger 등)에 변경 사항을 올바르게 반영했나요? → `request_id` 요청 필드 및 `409 P007` 응답을 Swagger에 반영 필요
