# CLAUDE.md — Procurement Service

> Hyundai AutoEver 3기 4차 프로젝트 / **Procurement(구매) 도메인** 백엔드
> 이 문서는 Claude Code가 이 서비스를 작업할 때 반드시 따라야 할 컨텍스트와 규칙이다.
> `⚠️ 확인 필요` 표시는 아직 팀 결정이 안 된/내가 확정 못 한 항목이니 임의로 코드에 박지 말 것.
>
> **연관 문서 (이 문서보다 우선하는 조직 계약):**
> - [BBD-AES EDA 이벤트 계약](./eda-event-spec.md) v3 — 토픽·DTO·컨슈머 규칙의 단일 진실원천. **스펙 변경은 그 문서 PR로만.**
> - [가격 정책](./price-policy.md) v3 — 기준단가 · 거래가 스냅샷 · 재고 단가 3종 구분 원칙.
>
> 충돌 시 우선순위: **eda-event-spec.md / price-policy.md > 이 문서**. 이 문서에는 Procurement 관점 요약만 둔다.
> ⚠️ 위 두 문서는 **이 레포에 없는 외부(조직 공유) 문서**다. 링크만 두며, 레포에서 파일을 찾지 말 것.

> **최근 큰 변경 요약 (최신순):**
> - **(이슈 #78)** Swagger 최신화 — `SwaggerConfig`/`@Operation` 정리, JWT 인증 헤더 반영.
> - **(인증 재전환)** `X-User-Id` 헤더 방식 폐기 → **`bbd-security-core` 라이브러리 도입.** JWT에서 현재 사용자(`userId`)를 `GetCurrentUserSnapshotUseCase`로 주입받고, API 권한은 **`@RequireRole(UserRole.…)` 어노테이션으로 서비스에서 직접 강제**. (이슈 #40의 "자체 인가 제거 + 헤더 신뢰" 모델을 대체)
> - **(이슈 #75)** API별 역할(`@RequireRole`) 설정 적용.
> - **(이슈 #63)** yaml 로깅 레벨 warn 조정 + **Item 단건 N회 조회 → 다건 1회 조회로 변경(N+1 제거)** (`loadItemPort.findBySkus`).
> - **(이슈 #62)** 외부 HTTP 클라이언트(Item/Sales) **connect/read timeout 설정 추가**.
> - **(이슈 #72)** Item 응답 `name` 필드를 `partName`에 매핑 (필드명 불일치로 라인 생성 실패하던 버그 수정).
> - **(이슈 #70)** PO/WorkOrder 생성 시 같은 `soNumber`의 구매/생산요청 알림을 `PENDING → DONE`으로 마감.
> - **(이슈 #60)** **WorkOrder(작업지시) 도메인 신규** — 생산(MAKE) 요청 처리. 생성/착수/완료/조회 web API.
> - **(Sales 구매요청 수신 컨슈머 구현)** `sales.purchase-requested` 토픽 **KafkaListener 구현 완료** — 수신 이벤트를 `SourcingResolver`로 **BUY/MAKE 분기** → BUY는 구매요청 알림(`po_request_notification`), MAKE는 생산요청 알림(`work_order_request_notification`)으로 라우팅. **멱등 인박스(`processed_event`) 가드 + DLQ(`DeadLetterPublishingRecoverer`) + 백오프** 적용.
> - **(이슈 #46)** MessageRelay — **Kafka 발행 ack 확인 후** outbox `processed_at` 기록 (send().get() 성공 시에만 markProcessed → at-least-once 보장).
> - **(이슈 #44)** local / public 프로파일 설정 정합성 확보.
> - **(이슈 #42)** Item·Sales 연동을 **RestClient → Spring HTTP Interface(`@HttpExchange`)** 로 전환.
> - **(이슈 #40)** 자체 인증/인가 코드 전면 삭제 — `@HasRole`·AUTH001/AUTH002 제거. (※ 이후 `bbd-security-core` 도입으로 인가가 다시 들어옴 — 위 "인증 재전환" 참고)
> - **(이슈 #38)** 헥사고날 포트-어댑터 분리 완료.
> - **(이슈 #36)** PO 변경이력(History) 기능 추가.
> - **(이슈 #31)** Sales SO 상세 조회 중계 API + SalesHttpService.
> - **(이슈 #29)** Item 연동 — PO 작성 시 SKU별 단가·부품명 자동 조회.
> - **(이슈 #28)** Kafka 연동 — `spring-boot-starter-kafka` 추가.
> - **(이슈 #27)** `VendorCreated`·`DomainEventOutboxRelay`·`DomainEvent` 제거.
> - **(이슈 #25)** `CONFIRMED` 상태 제거 → 상태 머신 **DRAFT → RECEIVED** 2단계. `/complete` 1개로 통합.

---

## 1. 이 서비스가 하는 일 (한 줄 요약)

**"어떤 공급사에게 + 어떤 부품을 + 몇 개 + 얼마에 주문했고 + 입고했는지"** + **"외주로 살지(BUY) 자체 생산할지(MAKE)"** 를 관리하는 서비스.

- 사용자: 발주처(본사) 직원
- Procurement가 직접 책임지는 것:
  - 공급사(Vendor) 정보 관리 — 코드, 이름, 연락처, 거래 조건, 활성 여부
  - 구매 주문(PO) 작성(DRAFT) · 완료(RECEIVED) · 취소
  - 완료 처리 — PO를 RECEIVED로 전이(동기)하면서 **`StockInRequested` 이벤트 발행 예약(outbox)** → 재고 반영은 Inventory가 비동기로 수행
  - 구매 단가(vendor 협상가)의 **거래 시점 스냅샷** 보존
  - **PO 변경이력 기록·조회** — 모든 변경(생성/헤더수정/라인교체/완료/취소)을 전/후 스냅샷으로 남김 (이슈 #36)
  - SO 연계 발주 시 `soNumber`를 이벤트에 실어 **Sales 백오더 충족 트리거** 제공
  - **Sales SO 상세 조회 중계** — 발주 요청 기반 PO 작성을 돕기 위해 Sales의 SO 상세를 동기 조회해 전달 (이슈 #31)
  - **Sales 구매요청(백오더) 수신 → 조달유형 분기** — `sales.purchase-requested` 컨슈머가 받아 라인별로 BUY/MAKE 판정 후 **구매요청 알림 / 생산요청 알림**으로 분리 저장. 실제 PO/WorkOrder는 알림을 보고 사람이 생성(현재는 자동 생성 아님).
  - **WorkOrder(작업지시) 관리** — 자체 생산(MAKE) 품목의 작업지시 생성(PLANNED) · 착수(IN_PRODUCTION) · 완료(COMPLETED) · 취소 (이슈 #60)

---

## 2. 아키텍처 컨텍스트

- 6개 마이크로서비스, **서비스마다 별도 DB** (공유 DB 금지, 직접 테이블 접근 금지)
- Client(React) → **Gateway**(Spring Cloud Gateway, On-Premise) → 각 서비스(ECS Fargate)
- 메시징: **Kafka** (plain spring-kafka, SCS 미사용 — 4팀 합의)
- **CI/CD: GitHub Actions** (`.github/workflows/workflow.yml`) — main push 시 Docker 이미지 빌드 → Docker Hub push → infra 레포(`BBD-AES/infra`) deploy 트리거. ECS 자동 배포 단계 추가됨.
- 브로커: `kafka.inwoohub.com:9092` (PLAINTEXT) · UI: https://kafka-ui.inwoohub.com/
- 다른 서비스 데이터가 필요하면 **반드시 그 서비스의 API/이벤트를 통해서**만 접근

### 인터페이스 (다른 서비스와의 관계)

| 방향 | 대상 | 목적 | 통신 방식 |
|---|---|---|---|
| Procurement → Inventory | 재고 증가 요청 | 완료 처리(DRAFT → RECEIVED 전이) 시 | **순수 비동기 Kafka — `procurement.stock-in-requested` (Outbox 경유)** |
| Procurement → Sales | 백오더 충족 트리거 | SO 연계 PO 완료 시 (`soNumber` 포함) | 같은 토픽 — sales가 별도 그룹(`sales-backorder`)으로 구독. 발행 코드 동일 |
| Sales → Procurement | **구매/생산요청(백오더) 수신** | Sales 백오더 발생 시 알림 → BUY/MAKE 분기 저장 | **Kafka 컨슈머 구현됨** — `sales.purchase-requested` 구독(`@KafkaListener`), 멱등 인박스(`processed_event`) + DLQ |
| Procurement → Item | 부품명·현재 기준단가·조달유형 조회 | PO 작성/라인 수정, 조달유형 판정 시 | **Spring HTTP Interface 동기 호출**(`@HttpExchange`) — `GET /api/v1/items/{sku}` (`item.base-url`). N+1 방지 위해 다건 조회 사용 |
| Procurement → Sales | SO 상세 조회 (중계) | 발주 요청 기반 PO 작성 보조 | **Spring HTTP Interface 동기 호출** — `GET /api/v1/sales-orders/{soNumber}` (`sales.base-url`) |
| Gateway/Client → Procurement | 인증된 사용자 식별 + 역할 인가 | 모든 요청 | **JWT 기반.** `bbd-security-core`가 토큰에서 현재 사용자(`userId`) 주입(`GetCurrentUserSnapshotUseCase`), API 권한은 **`@RequireRole`로 서비스가 직접 강제** |

> **변경 이력 주의:**
> - Inventory 연동: "동기 Feign" → "이벤트 기반(v3)" → (이슈 #25) **순수 비동기 Kafka**로 확정. Inventory에 동기 HTTP 코드 작성 금지.
> - Item 연동: (이슈 #25) "도입 안 함" → (이슈 #29) "RestClient 도입" → (이슈 #42) **Spring HTTP Interface로 확정**. (이슈 #63) 단건 N회 → 다건 1회.
> - Sales 연동: ① 비동기 백오더 트리거(발행) ② (이슈 #31) 동기 SO 조회 중계 ③ **구매요청 수신 컨슈머(`sales.purchase-requested`)** — 3개 별개 채널.
> - 인증: (이슈 #40) 자체 인가 제거 → `X-User-Id` 헤더 → **(현재) `bbd-security-core` 라이브러리 기반 JWT userId 주입 + `@RequireRole` 인가로 재전환.** `X-User-Id`/`X-User-Role` 헤더는 더 이상 쓰지 않는다.

---

## 3. 도메인 모델

### Vendor (공급사)
```
Vendor
├ code        // 유일 식별자
├ name
├ contact     // 연락처
├ terms       // 거래 조건(지불 기한 등)
└ active      // 활성 여부
```

### PurchaseOrder (PO)
```
PurchaseOrder
├ id                 // PK (IDENTITY)
├ poNumber           // 유일. 형식 PO-YYYY-NNNNNN (이벤트 메시지 key로도 사용, updatable=false)
├ vendorCode         // Vendor 참조 (FK 아님, 코드 참조)
├ warehouseCode      // 입고 창고 (inventory Warehouse.code 형식, 예: WH-HQ-001) — 이벤트 라인에 실림
├ status             // DRAFT / RECEIVED / CANCELED  ← CONFIRMED 없음 (이슈 #25)
├ totalAmount        // 라인 subtotal 합 (BigDecimal, 자동 재계산)
├ expectedArrival    // 예상 입고일 (nullable, LocalDate)
├ note               // 비고 (nullable, TEXT)
├ soNumber           // nullable. SO 연계 발주일 때만 보유 → StockInRequested.soNumber로 전달
├ createdBy / receivedBy   // 사번 (BIGINT — V14에서 varchar → bigint, confirmedBy/At는 V5에서 제거)
├ createdAt / receivedAt   // createdAt/updatedAt은 BaseTimeEntity 상속
└ lines (1:N)              // PurchaseOrderLine, lineOrder ASC 정렬
```

> ❌ `confirmedBy` / `confirmedAt` 컬럼과 `po.confirm()` 도메인 메서드는 **제거됨** — 새 코드에서 참조 금지.
> ℹ️ 완료 전이 도메인 메서드명은 **`po.markReceived(receivedBy)`** (서비스의 `complete()`가 호출). 취소는 `po.cancel()`.

### PurchaseOrderLine (주문 항목 한 줄)
```
PurchaseOrderLine
├ id
├ lineOrder        // 라인 순서 (int)
├ sku / partName   // 작성 시점 스냅샷
├ unitPrice        // 작성 시점 vendor 협상가 스냅샷 (BigDecimal)
├ quantity
└ subtotal         // unitPrice × quantity (생성 시 계산)
```
> line의 부품명/단가는 마스터를 참조하지 않고 **작성 시점 값을 자체 저장**한다.

### PurchaseOrderHistory (변경이력 — 이슈 #36)
```
PurchaseOrderHistory
├ id
├ poNumber
├ changeType       // CREATED / HEADER_UPDATED / LINES_REPLACED / COMPLETED / CANCELED
├ beforePayload    // 변경 전 PO 전체 스냅샷 JSON (CREATED는 NULL)
├ afterPayload     // 변경 후 PO 전체 스냅샷 JSON (text — V13)
├ changedBy        // 사번 (BIGINT — V14)
└ changedAt
```
> 모든 PO 변경 유스케이스가 `recordHistory()`로 같은 트랜잭션 안에서 이력을 남긴다. 직렬화는 `PurchaseOrderSnapshot` + ObjectMapper.

### WorkOrder (작업지시 — 이슈 #60)
```
WorkOrder
├ id
├ workOrderNumber  // 유일 (work_order_number_seq 채번)
├ soNumber         // 연계 SO
├ warehouseCode
├ status           // PLANNED / IN_PRODUCTION / COMPLETED / CANCELED
├ createdBy / completedBy   // 사번 (BIGINT — V14)
└ lines (1:N)      // WorkOrderLine
```
> 도메인 전이 메서드: `start()`(PLANNED→IN_PRODUCTION), `markCompleted(completedBy)`, `cancel()`(COMPLETED는 취소 불가, CANCELED는 멱등).

### 요청 알림 / 멱등 인박스 (Sales 구매요청 수신 흐름)
```
ProcessedEvent (processed_event, V9)          // 멱등 가드 — 처리한 eventId 저장. 중복 수신 차단
PurchaseRequestNotification (po_request_notification, V9)         // BUY 라인 알림. status PENDING/DONE
WorkOrderRequestNotification (work_order_request_notification, V10) // MAKE 라인 알림. status PENDING/DONE
```
> 수신 흐름: `PurchaseRequestedListener`(@KafkaListener) → `PurchaseRequestNotificationService.handle()` → `existsByEventId`로 멱등 체크 → `SourcingResolver`로 라인별 BUY/MAKE 판정 → BUY/MAKE 알림 각각 저장 → `processed_event` 기록. PO/WorkOrder 생성 시 같은 `soNumber` 알림을 `PENDING→DONE` 마감(이슈 #70).
> `SourcingResolver`: ① 이벤트 hint(BUY/MAKE) 우선 → ② null이면 Item 마스터의 `sourcingType` 재해석 → ③ 그래도 불명이면 **BUY로 degrade**.

### 가격 — 3종 구분 (→ [가격 정책](./price-policy.md))

| 가격 | 위치 | Procurement 관점 |
|---|---|---|
| 현재 기준단가 | `item.unitPrice` (int, 진실원천) | PO 작성/라인수정 시 HTTP Interface로 조회 → 라인 스냅샷. 이후 마스터 변경과 무관 |
| **거래가 스냅샷** | **PO 라인 `unitPrice`** | **우리가 책임지는 값.** vendor 협상가, 작성 후 불변 (감사·3-way match 근거) |
| 재고 단가 | `stock.unitPrice` | inventory 소관. 우리 거래가로 덮어쓰이지 않음 |

- **와이어(이벤트)의 단가는 원화 정수(int)** — PO 라인 BigDecimal은 `intValueExact()` 변환.

---

## 4. 상태 머신 (State Machine)

### PurchaseOrder — 이슈 #25 기준
```
DRAFT ──완료(complete / markReceived)──> RECEIVED
  │
  └────── 취소(cancel) ──────> CANCELED
(RECEIVED, CANCELED 모두 종료 상태)
```

| 상태 | 설명 | 비고 |
|---|---|---|
| DRAFT | 임시 저장 (작성·수정·삭제 가능) | 헤더/라인 수정은 DRAFT에서만 (`ensureDraft()`) |
| RECEIVED | 완료 — 입고 확정 + `StockInRequested` outbox 기록 완료 | `markReceived()`는 DRAFT일 때만, 라인 1개 이상 + receivedBy 필수 |
| CANCELED | DRAFT에서 취소 종료 | RECEIVED는 취소 불가, 이미 CANCELED면 멱등(no-op) |

### WorkOrder — 이슈 #60
```
PLANNED ──start──> IN_PRODUCTION ──markCompleted──> COMPLETED
   │
   └────── cancel ──────> CANCELED   (COMPLETED는 취소 불가, CANCELED는 멱등)
```

**규칙**
- 상태 전이는 **도메인 객체가 스스로 검증**한다. 서비스/컨트롤러에서 `if (status == ...)` 식 우회 금지.
- 잘못된 PO 전이 예외: RECEIVED 재완료 → `PO_ALREADY_RECEIVED`, 그 외 비정상 전이 → `PO_INVALID_STATE_TRANSITION`, DRAFT 아닌 수정 → `PO_NOT_EDITABLE`.
- `CONFIRMED`라는 단어가 enum/메서드/테스트/주석 어디에도 새로 등장하면 안 된다.

---

## 5. 핵심 비즈니스 규칙 (절대 깨지면 안 됨)

1. **단가 스냅샷** — PO 라인 단가는 작성 시점 값을 *복사*. 이후 마스터가 바뀌어도 PO 단가는 불변.
2. **멱등성** — 같은 PO를 **중복 완료 처리할 수 없다**. 재고가 두 번 늘어나면 안 됨. 수신 이벤트도 `processed_event`로 중복 차단.
3. **원자성(로컬)** — 완료 처리는 "PO 상태 변경(DRAFT→RECEIVED) + outbox INSERT + 이력 기록"이 **한 DB 트랜잭션**으로 성립. ✓ `complete()`가 `@Transactional`.

### 완료 처리 흐름 (`/complete`)

```
① 사전 검증: PO가 DRAFT인지 도메인이 확인(markReceived). RECEIVED/CANCELED면 예외
② 한 트랜잭션 안에서 (동기):
   - po.markReceived(receivedBy)  → 상태 RECEIVED + receivedBy/At
   - StockInRequested 직렬화 → outbox_event INSERT (topic, key=poNumber, payload)
   - recordHistory(COMPLETED)     → purchase_order_history INSERT
   - 커밋 → 응답
③ MessageRelay(1초 폴링)가 outbox 행을 KafkaTemplate.send → ack(.get) 성공 시에만 markProcessed (이슈 #46)
④ inventory 수신 → 재고 증가 ; sales(sales-backorder) 수신 → soNumber 있으면 백오더 충족
```

**멱등성 구조**
- 로컬 트랜잭션이 원자성 보장, Kafka 전달은 at-least-once.
- 생산자측 1차 방어: RECEIVED인 PO는 complete 재호출 시 예외 → 새 이벤트 안 생김.
- **이벤트 1건당 eventId는 outbox INSERT 시점에 한 번만 생성**(relay 재발행해도 동일 eventId).
- **수신측(우리 컨슈머):** `sales.purchase-requested`는 `processed_event` 멱등 가드 + DLQ로 보호.
- **발행측 견고성(이슈 #46):** ack 성공 시에만 `markProcessed()`. ⚠️ **retry cap / DLQ / failed_count 없음** — poison 이벤트 무한 재시도 (운영 보강 후보, 미구현).

### `StockInRequested` 발행 스펙 (정본은 [이벤트 계약 §3-2](./eda-event-spec.md))

- 토픽 `procurement.stock-in-requested` · key = `poNumber` · JSON 문자열
- 발행 시점: PO RECEIVED 전이(`/complete` 트랜잭션)
- envelope: `eventId`(UUID) / `source`="procurement" / `eventType`="STOCK_IN_REQUESTED" / `occurredAt`=**UTC Instant 문자열** (Jackson 3 기본값으로 ISO-8601 직렬화)
- body: `poNumber`, `soNumber`(nullable), `lines[]`(sku, quantity, warehouseCode, unitPrice **원화 int**)
- DTO record는 계약 문서에서 **그대로 복붙**.

---

## 6. 권한 (현재 `@RequireRole`로 서비스에서 강제됨)

| 역할 | 권한 |
|---|---|
| HQ_MANAGER | PO 작성·수정·**완료(complete)**·취소 + 공급사 관리 + WorkOrder 전체(착수/완료 포함) |
| HQ_STAFF | PO 작성·수정·취소·조회, WorkOrder 생성·조회. **PO complete / WO start·complete 불가** |
| BRANCH | 구매 권한 없음 (구매는 본사 영역) |

> 인가는 `bbd-security-core`의 **`@RequireRole(UserRole.…)`** 로 각 컨트롤러 메서드에 적용된다(이슈 #75). 사용자(`userId`)는 JWT에서 `GetCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId()`로 주입.
> ⚠️ 일부 메서드의 정확한 역할 조합은 컨트롤러의 `@RequireRole`/`@Operation`을 정본으로 확인할 것(이 표는 의도된 정책 요약).

---

## 7. API 명세

> Base path: `/api/v1` · 담당: Dayoung
> 사용자 식별은 **JWT**(`bbd-security-core`). `X-User-Id` 헤더는 더 이상 사용하지 않는다.

### Vendor (공급사)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/v1/vendors` | 공급사 등록 | HQ_MANAGER |
| PATCH | `/api/v1/vendors/{code}` | 공급사 수정 | HQ_MANAGER |
| PATCH | `/api/v1/vendors/{code}/active` | 활성/비활성 전환 | HQ_MANAGER |
| GET | `/api/v1/vendors/{code}` | 상세 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/vendors` | 목록 조회 | HQ_MANAGER, HQ_STAFF |

### PurchaseOrder (PO)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/v1/purchase-orders` | PO 작성 (DRAFT) | HQ_MANAGER, HQ_STAFF |
| PATCH | `/api/v1/purchase-orders/{poNumber}` | 헤더 수정 (DRAFT만) | HQ_MANAGER, HQ_STAFF |
| PUT | `/api/v1/purchase-orders/{poNumber}/lines` | 라인 교체 (DRAFT만) | HQ_MANAGER, HQ_STAFF |
| POST | `/api/v1/purchase-orders/{poNumber}/complete` | **완료 (RECEIVED + StockInRequested outbox + 이력)** | **HQ_MANAGER** |
| POST | `/api/v1/purchase-orders/{poNumber}/cancel` | 취소 (DRAFT → CANCELED) | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/purchase-orders/{poNumber}` | 상세 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/purchase-orders` | 목록 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/purchase-orders/{poNumber}/history` | 변경이력 조회 (이슈 #36) | HQ_MANAGER, HQ_STAFF |

### 구매요청 알림 / SalesOrder 중계

| 메서드 | 엔드포인트 | 설명 | 권한 |
|---|---|---|---|
| GET | `/api/v1/purchase-requests` | 구매요청(BUY) 알림 목록 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/sales-orders/{soNumber}` | Sales SO 상세 동기 조회·중계 (이슈 #31) | HQ_MANAGER, HQ_STAFF |

### WorkOrder (작업지시 — 이슈 #60)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/v1/work-orders` | 작업지시 생성 (PLANNED) | HQ_MANAGER, HQ_STAFF |
| POST | `/api/v1/work-orders/{workOrderNumber}/start` | 착수 (→ IN_PRODUCTION) | HQ_MANAGER |
| POST | `/api/v1/work-orders/{workOrderNumber}/complete` | 완료 (→ COMPLETED) | HQ_MANAGER |
| GET | `/api/v1/work-orders/{workOrderNumber}` | 단건 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/work-orders` | 목록 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/work-order-requests` | 생산요청(MAKE) 알림 목록 조회 | HQ_MANAGER, HQ_STAFF |

> ❌ `/confirm`, `/receive` 엔드포인트는 만들지 않는다 (이슈 #25).

**API ↔ 규칙 연결 메모**
- 수정 계열(`PATCH`/`PUT .../lines`)은 DRAFT에서만 허용 (§4).
- `complete`/`cancel`은 status를 직접 바꾸지 않고 도메인 전이 메서드를 호출.
- Item·Sales 호출 실패 시 `ITEM_NOT_FOUND`/`ITEM_SERVICE_ERROR`, `SO_NOT_FOUND`/`SALES_SERVICE_ERROR`로 매핑.

---

## 8. 기술 스택 / 컨벤션

- Language / Framework: Java 21 + Spring Boot 4.0.6
- Build: **Gradle** (사내 라이브러리 `com.bbd:bbd-security-core:0.0.6`는 **GitHub Packages**에서 받음 — `GITHUB_USERNAME`/`GITHUB_TOKEN` 또는 `gpr.user`/`gpr.key` 필요)
- DB: **PostgreSQL** · 스키마는 Flyway 관리
  - V1 vendor / V2 outbox / V3 purchase_order / V4 so_id / V5 confirmed 제거 / V6 outbox topic / V7 so_id→so_number / V8 po_history / **V9 processed_event + po_request_notification(인박스)** / **V10 work_order_request_notification** / **V11 work_order(+seq, line)** / **V12 PO varchar 확장** / **V13 po_history.after_payload→text** / **V14 actor 컬럼 varchar→BIGINT**
- 아키텍처: **헥사고날(포트-어댑터)** — `adapter/in/{web,messaging}`, `adapter/out/{persistence,external,messaging}`, `application/{port,service}`, `domain`. 도메인별 패키징(vendor / purchaseorder / workorder / shared.outbox / shared.inbox).
- 동기 HTTP 클라이언트: **Spring HTTP Interface(`@HttpExchange`)** — `HttpServiceConfig`에서 `ItemHttpService`·`SalesHttpService` 빈 생성. **connect/read timeout 설정됨(이슈 #62)**. Item은 다건 조회로 N+1 제거(이슈 #63). **Inventory 연동은 Kafka only**.
  - ⚠️ `InventoryHttpService` / `UserHttpService`는 **빈 껍데기 클래스(미사용)** — 정리(삭제) 대상.
- Kafka: **`spring-boot-starter-kafka`** — **producer(발행) + consumer(수신) 모두 구성됨**. 컨슈머는 `KafkaConsumerConfig`(`DefaultErrorHandler` + `DeadLetterPublishingRecoverer` + `FixedBackOff(1s,2회)`). 현재 구독 토픽: `sales.purchase-requested`.
- 직렬화: JSON 문자열(String serializer). ObjectMapper는 starter-json 기본(Jackson 3, `tools.jackson`).
- 인증/인가: **`bbd-security-core` 라이브러리**. JWT에서 `userId` 주입 + `@RequireRole` 인가. (자체 인증/보안 코드 임의 생성 금지 — 라이브러리 API 사용)
- Redis: `spring-boot-starter-data-redis` 의존성 추가됨. ⚠️ **용도/설정 확정 필요**(security-core 토큰·캐시 추정) — 관련 코드 임의 작성 전 확인.

### 프로파일 (설정)

- `application.yaml` (기본) — gitignore, 로컬 개인용. `${ENV}` placeholder, `ddl-auto: update`.
- `application-local.yaml` — `local`, 추적됨. localhost DB, `ddl-auto: validate`, Flyway·Kafka 포함.
- `application-public.yml` — `public`(운영), 추적됨. 전부 `${ENV}`, `ddl-auto: validate`.

---

## 9. 구현 현황 / 체크리스트 (procurement 분)

- [x] `outbox_event` + MessageRelay(1초 폴링) / ack 후 markProcessed (이슈 #28, #46)
- [x] 상태 머신 DRAFT→RECEIVED + `/complete` 통합 (이슈 #25)
- [x] Item 연동(단가·부품명·조달유형) + 다건 조회 N+1 제거 (이슈 #29, #63, #72)
- [x] Sales SO 상세 조회 중계 API (이슈 #31)
- [x] PO 변경이력 (이슈 #36)
- [x] 헥사고날 포트-어댑터 분리 (이슈 #38)
- [x] Item·Sales HTTP Interface 전환 + timeout (이슈 #42, #62)
- [x] local/public 프로파일 정합성 (이슈 #44)
- [x] **Sales 구매요청 수신 컨슈머 + 멱등 인박스(`processed_event`) + DLQ** — `sales.purchase-requested` → BUY/MAKE 분기 라우팅
- [x] **WorkOrder 도메인 + web API(생성/착수/완료/조회)** (이슈 #60)
- [x] **PO/WO 생성 시 같은 soNumber 알림 PENDING→DONE 마감** (이슈 #70)
- [x] **인증/인가 재전환 — `bbd-security-core`(JWT userId 주입) + `@RequireRole`** (이슈 #75 외)
- [x] Swagger 최신화 (이슈 #78)
- [ ] **테스트 거의 전무** — 도메인 상태기계/Outbox/멱등 인박스/MessageRelay 단위 테스트 필요 (현재 `PurchaseRequestedListenerIT` 1개뿐). ⚠️ 최우선
- [ ] (운영 보강) MessageRelay retry cap / DLQ / failed_count — poison 무한 재시도 방지
- [ ] `InventoryHttpService`/`UserHttpService` 빈 클래스 제거, `GlobalExceptionHandler` catch-all/검증 핸들러 보강
- [ ] Redis 용도 확정

### 빌드 / 실행 / 테스트 명령

```bash
./gradlew build                                            # 빌드 (GitHub Packages 인증 필요 — bbd-security-core)
docker-compose up -d                                       # 로컬 PostgreSQL 기동
./gradlew test                                             # 테스트 (DB 필요)
./gradlew bootRun --args='--spring.profiles.active=local'  # 로컬 실행 (local 프로파일 권장)
```

> ⚠️ 프로파일 없이 돌리면 기본 `application.yaml`(localhost 기본값) → 로컬 DB가 떠 있어야 한다.
> ⚠️ `bbd-security-core`는 GitHub Packages에서 받으므로 `GITHUB_USERNAME`/`GITHUB_TOKEN`(또는 `gpr.user`/`gpr.key`) 설정이 없으면 의존성 해석 실패.

---

## 10. 작업 시 주의사항 (Claude에게 주는 규칙)

- 상태 머신은 **PO: DRAFT/RECEIVED/CANCELED**, **WorkOrder: PLANNED/IN_PRODUCTION/COMPLETED/CANCELED**. CONFIRMED 재도입 금지.
- 상태 전이 로직은 **반드시 도메인 객체 내부**에 둔다. 서비스에서 상태 직접 변경 금지.
- PO 라인의 부품명·단가는 **항상 스냅샷**. Item을 런타임 join하지 않는다.
- 다른 서비스 DB 직접 접근 금지. API/이벤트만.
- **Inventory 연동에 동기 HTTP 코드 금지** — 반드시 outbox → Kafka. HTTP Interface는 Item/Sales 조회 전용.
- 완료 처리는 "상태 변경 + outbox INSERT + 이력"이 **한 트랜잭션**. Kafka 발행은 relay가 ack 확인 후 markProcessed.
- `eventId`는 outbox INSERT 시점 1회 생성. relay가 재생성 금지.
- 이벤트 DTO는 [계약 문서](./eda-event-spec.md) record를 **그대로 복붙**. 필드 추가는 nullable만.
- `occurredAt`은 UTC `Instant` 문자열. LocalDateTime 직렬화 금지.
- 이벤트 단가는 원화 정수(int) — BigDecimal은 `intValueExact()`.
- **인증/인가는 `bbd-security-core` API만 사용**(자체 보안 코드 임의 생성 금지). 사용자 식별은 `GetCurrentUserSnapshotUseCase`, 권한은 `@RequireRole`. `X-User-Id` 헤더 부활 금지.
- **Kafka 컨슈머 신규 추가 시** at-least-once 대비 `processed_event` 멱등 가드를 반드시 함께 둘 것(기존 `sales.purchase-requested` 패턴 따르기).
- SCS(Spring Cloud Stream) 도입 금지.
- `⚠️ 확인 필요` 항목(Redis 용도 등)은 추측해서 코드에 박지 말고 먼저 질문할 것.
