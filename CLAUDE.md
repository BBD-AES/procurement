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
>
> **최근 큰 변경 요약:**
> - **(이슈 #25)** `CONFIRMED` 상태 제거 → 상태 머신 **DRAFT → RECEIVED** 2단계. `/confirm` + `/receive` → **`/complete` 1개** 통합.
> - **(이슈 #27)** `VendorCreated` · `DomainEventOutboxRelay` · `DomainEvent` 인터페이스 제거 — Vendor 이벤트 Kafka 발행 없음.
> - **(이슈 #28)** Kafka 연동 완료 — `spring-boot-starter-kafka` 추가, `MessageRelay` `KafkaTemplate.send()` 구현.
> - **(이슈 #29 진행 중)** Item RestClient 연동 — PO 작성 시 SKU별 단가·부품명 자동 조회 (소스 확정).

---

## 1. 이 서비스가 하는 일 (한 줄 요약)

**"어떤 공급사에게 + 어떤 부품을 + 몇 개 + 얼마에 주문했고 + 입고했는지"** 를 관리하는 서비스.

- 사용자: 발주처(본사) 직원
- Procurement가 직접 책임지는 것:
  - 공급사(Vendor) 정보 관리 — 코드, 이름, 연락처, 거래 조건, 활성 여부
  - 구매 주문(PO) 작성(DRAFT) · 완료(RECEIVED) · 취소
  - 완료 처리 — PO를 RECEIVED로 전이(동기)하면서 **`StockInRequested` 이벤트 발행 예약(outbox)** → 재고 반영은 Inventory가 비동기로 수행
  - 구매 단가(vendor 협상가)의 **거래 시점 스냅샷** 보존
  - SO 연계 발주 시 `soNumber`를 이벤트에 실어 **Sales 백오더 충족 트리거** 제공
  - (확장 예약) 안전재고 미달 시 PO 자동 생성 — `inventory.purchase-requested`는 아직 비계약. 현재 PO는 사람이 생성

---

## 2. 아키텍처 컨텍스트

- 6개 마이크로서비스, **서비스마다 별도 DB** (공유 DB 금지, 직접 테이블 접근 금지)
- Client(React) → **Gateway**(Spring Cloud Gateway, On-Premise) → 각 서비스(ECS Fargate)
- 메시징: **Kafka** (plain spring-kafka, SCS 미사용 — 4팀 합의) / CI/CD: **GitLab**
- 브로커: `kafka.inwoohub.com:9092` (PLAINTEXT) · UI: https://kafka-ui.inwoohub.com/
- 다른 서비스 데이터가 필요하면 **반드시 그 서비스의 API/이벤트를 통해서**만 접근

### 인터페이스 (다른 서비스와의 관계)

| 방향 | 대상 | 목적 | 통신 방식                                                                 |
|---|---|---|-----------------------------------------------------------------------|
| Procurement → Inventory | 재고 증가 요청 | 완료 처리(DRAFT → RECEIVED 전이) 시 | **순수 비동기 Kafka — `procurement.stock-in-requested` (Outbox 경유)**       |
| Procurement → Sales | 백오더 충족 트리거 | SO 연계 PO 완료 시 (`soNumber` 포함) | 같은 토픽 — sales가 별도 그룹(`sales-backorder`)으로 구독. **발행 코드는 동일, 추가 작업 없음** |
| Procurement → Item | 부품명·현재 기준단가 조회 | PO 작성 시 (라인별 SKU) | **Spring RestClient 동기 호출** — `GET /api/v1/items/{sku}` (Item 서비스 `localhost:8082/item`) |
| Procurement → User/Auth | 권한 · 소속 검증 | 요청 처리 시 | ⚠️ 확인 필요                                                              |
| Sales → Procurement | 발주할 물품 리스트 요청 | PO 생성 트리거 | ⚠️ 확인 필요 — 단, PO가 `soNumber`를 보유하는 SO 연계 발주 자체는 계약에 반영됨               |

> **변경 이력 주의:**
> - Inventory 연동: "동기 Feign" → "이벤트 기반(v3)" → (이슈 #25) **순수 비동기 Kafka**로 확정. Inventory에 동기 HTTP 코드 작성 금지.
> - Item 연동: (이슈 #25) "도입 안 함" → (이슈 #29) **RestClient로 확정**. PO 작성 시 SKU별 단가·부품명 조회에만 사용.

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
├ number         // 유일. 형식 PO-YYYY-NNNNNN (이벤트 메시지 key로도 사용)
├ vendorCode     // Vendor 참조 (FK 아님, 코드 참조)
├ status         // DRAFT / RECEIVED / CANCELED  ← CONFIRMED 없음 (이슈 #25)
├ soNumber       // nullable. SO 연계 발주일 때만 보유 → StockInRequested.soNumber로 전달 (백오더 트리거)
├ warehouseCode  // 입고 창고 (inventory Warehouse.code 형식, 예: WH-HQ-001) — 이벤트 라인에 실림
├ createdBy / receivedBy                 // 사번 (confirmedBy/At는 V6 마이그레이션으로 제거)
├ createdAt / receivedAt
└ lines (1:N)
```

> ❌ `confirmedBy` / `confirmedAt` 컬럼과 `po.confirm()` 도메인 메서드는 **제거됨** — 새 코드에서 참조 금지. 잔재 발견 시 정리 대상.

### PurchaseOrderLine (주문 항목 한 줄)
```
PurchaseOrderLine
├ sku / partName   // 작성 시점 스냅샷
├ quantity
├ unitPrice        // 작성 시점 vendor 협상가 스냅샷 (BigDecimal)
└ subtotal
```
> PO status = **PO 전체의 단계**, line = **개별 품목 줄**. 둘을 헷갈리지 말 것.
> line의 부품명/단가는 마스터를 참조하지 않고 **작성 시점 값을 자체 저장**한다.

### 가격 — 3종 구분 (→ [가격 정책](./price-policy.md))

| 가격 | 위치 | Procurement 관점 |
|---|---|---|
| 현재 기준단가 | `item.unitPrice` (int, 진실원천) | PO 작성 시 RestClient(`GET /api/v1/items/{sku}`)로 조회 → 라인 스냅샷. 이후 마스터 변경과 무관 |
| **거래가 스냅샷** | **PO 라인 `unitPrice`** | **우리가 책임지는 값.** vendor 협상가, 작성 후 불변 (상사문서 — 감사·3-way match 근거) |
| 재고 단가 | `stock.unitPrice` | inventory 소관. 우리 거래가로 절대 덮어쓰이지 않음 (이벤트의 unitPrice는 movement 이력용) |

- **와이어(이벤트)의 단가는 원화 정수(int)** — PO 라인 BigDecimal은 `intValueExact()` 변환 (원화라 안전).

---

## 4. 상태 머신 (State Machine) — 이슈 #25 기준

```
DRAFT ──완료(complete)──> RECEIVED
  │
  └────── 취소 ──────> CANCELED

(RECEIVED, CANCELED 모두 종료 상태 — 이후 어떤 전이도 불가)
```

| 상태 | 설명 | 전이 가능 권한 |
|---|---|---|
| DRAFT | 임시 저장 (작성·수정·삭제 가능) | 작성·수정: HQ_STAFF, HQ_MANAGER |
| RECEIVED | 완료 — 입고 확정 + `StockInRequested` outbox 기록 완료. 재고는 inventory가 비동기 반영 (최종 일관성) | **complete: HQ_MANAGER만** |
| CANCELED | DRAFT에서 취소 종료 | HQ_STAFF, HQ_MANAGER (**둘 다 DRAFT에서만**) |

**규칙**
- 상태 전이는 **도메인 객체가 스스로 검증**한다. 서비스/컨트롤러에서 `if (status == ...)` 식으로 우회하는 구조 금지.
- 잘못된 전이는 예외를 던진다 (예: RECEIVED → CANCELED 불가, RECEIVED 재완료 불가).
- 취소는 **DRAFT에서만** 가능 — 역할 무관 동일 (이전의 "STAFF=DRAFT만 / MANAGER=CONFIRMED까지" 분기는 폐기).
- PO 수정(헤더/라인)은 DRAFT에서만.
- `CONFIRMED`라는 단어가 enum, 메서드, 테스트, 주석 어디에도 새로 등장하면 안 된다.

---

## 5. 핵심 비즈니스 규칙 (절대 깨지면 안 됨)

1. **단가 스냅샷** — PO 라인 단가는 작성 시점 값을 *복사*. 이후 마스터가 바뀌어도 PO 단가는 불변.
2. **멱등성** — 같은 PO를 **중복 완료 처리할 수 없다**. 재고가 두 번 늘어나면 안 됨.
3. **원자성(로컬)** — 완료 처리는 "PO 상태 변경(DRAFT→RECEIVED) + 이벤트 발행 예약(outbox INSERT)"이 **한 DB 트랜잭션**으로 성립해야 한다. 어느 한쪽만 커밋되는 경우는 존재할 수 없다.

### 완료 처리 흐름 (`/complete`) — 상태 변경은 동기, 이벤트 발행은 비동기

```
① 사전 검증: PO가 DRAFT인지 도메인이 확인. RECEIVED/CANCELED면 예외 (재진입 방어)
② 한 트랜잭션 안에서 (동기 — API 응답 전에 완료):
   - po.complete()  → 상태 RECEIVED + receivedBy/At 기록
   - StockInRequested DTO 직렬화 → outbox_event INSERT (topic, key=poNumber, payload)
   - 커밋 → 클라이언트에 응답
③ MessageRelay(1초 폴링, 비동기)가 outbox 행을 읽어 KafkaTemplate.send(topic, key, payload)
④ inventory가 수신 → 재고 증가 + movement(IN, 거래가) 기록
   sales(sales-backorder 그룹)가 수신 → soNumber 있으면 백오더 충족 흐름
```

**멱등성 구조**
- 동기 호출 실패 → 롤백 모델이 아니다. **로컬 트랜잭션이 원자성을 보장**하고(상태 변경과 outbox INSERT가 같이 성공/실패), Kafka 전달은 at-least-once.
- 중복 방어는 양쪽에 건다:
  - **Procurement(생산자):** 상태 머신이 1차 방어 — RECEIVED인 PO는 complete 재호출 시 예외 → 새 이벤트가 만들어지지 않는다.
  - **컨슈머(inventory/sales):** at-least-once로 같은 이벤트가 중복 배달될 수 있으므로 `eventId` 기반 `processed_event` 멱등 가드 필수 (계약 의무, 컨슈머 책임).
- relay 재시도로 같은 outbox 행이 두 번 발행돼도 `eventId`가 동일하므로 컨슈머에서 무시된다 — **이벤트 1건당 eventId는 outbox INSERT 시점에 한 번만 생성**할 것 (발행 시점 생성 금지).
- "완벽한 분산 원자성"이 아니라 **로컬 원자성(outbox) + at-least-once + 컨슈머 멱등 = 최종 일관성**이 목표임을 기억할 것.

### `StockInRequested` 발행 스펙 (요약 — 정본은 [이벤트 계약 §3-2](./eda-event-spec.md))

- 토픽 `procurement.stock-in-requested` · key = `poNumber` · JSON 문자열 직렬화
- 발행 시점: **PO RECEIVED 전이 시점** (= `/complete` 트랜잭션) — 계약 문구와 일치, CONFIRMED 제거와 무관하게 유효
- envelope: `eventId`(UUID) / `source`="procurement" / `eventType`="STOCK_IN_REQUESTED" / `occurredAt`=**UTC Instant 문자열** (`Instant.toString()`, LocalDateTime 금지)
- body: `poNumber`, `soNumber`(nullable — SO 연계 발주만), `lines[]`(sku, quantity, warehouseCode, unitPrice **원화 int**)
- DTO record는 계약 문서에서 **그대로 복붙** (공유 jar 없음, 구독 레포와 동일 정의 유지)
- `DomainEvent` 인터페이스는 이슈 #27에서 제거됨 — DTO를 직접 직렬화해 payload 저장.

---

## 6. 권한

| 역할 | 권한 |
|---|---|
| HQ_MANAGER | PO 작성·수정·**완료(complete)**·취소(DRAFT만) + 공급사 관리 |
| HQ_STAFF | PO 작성·수정·취소(DRAFT만). **complete 불가** |
| BRANCH | 구매 권한 없음 (구매는 본사 영역) |

> 이전 설계의 "STAFF 취소는 매니저 승인 필요"는 **폐기** — 취소 규칙은 두 역할 모두 "DRAFT에서만"으로 단순화됨.

---

## 7. API 명세

> Base path: `/api/v1` · 담당: Dayoung
> 경로 변수는 `{poNumber}`로 통일.

### Vendor (공급사)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/v1/vendors` | 공급사 등록 | HQ_MANAGER |
| PATCH | `/api/v1/vendors/{code}` | 공급사 수정 | HQ_MANAGER |
| PATCH | `/api/v1/vendors/{code}/active` | 공급사 활성/비활성 전환 | HQ_MANAGER |
| GET | `/api/v1/vendors/{code}` | 공급사 상세 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/vendors` | 공급사 목록 조회 | HQ_MANAGER, HQ_STAFF |

### PurchaseOrder (PO)

| 메서드 | 엔드포인트 | 설명 | 권한 |
|---|---|---|---|
| POST | `/api/v1/purchase-orders` | PO 작성 (DRAFT) | HQ_MANAGER, HQ_STAFF |
| PATCH | `/api/v1/purchase-orders/{poNumber}` | PO 헤더 수정 (DRAFT만) | HQ_MANAGER, HQ_STAFF |
| PUT | `/api/v1/purchase-orders/{poNumber}/lines` | PO 라인 수정 (DRAFT만) | HQ_MANAGER, HQ_STAFF |
| POST | `/api/v1/purchase-orders/{poNumber}/complete` | **완료 (DRAFT → RECEIVED + StockInRequested outbox)** | **HQ_MANAGER** |
| POST | `/api/v1/purchase-orders/{poNumber}/cancel` | PO 취소 (DRAFT → CANCELED) | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/purchase-orders/{poNumber}` | PO 상세 조회 | HQ_MANAGER, HQ_STAFF |
| GET | `/api/v1/purchase-orders` | PO 목록 조회 | HQ_MANAGER, HQ_STAFF |

> ❌ `/confirm`, `/receive` 엔드포인트는 **만들지 않는다** (이슈 #25). 기존 명세/스프레드시트에 남아 있어도 무시.

**API ↔ 규칙 연결 메모**
- `PATCH .../{poNumber}` · `PUT .../{poNumber}/lines` (수정 계열)는 **DRAFT 상태에서만** 허용 (§4). 다른 상태면 예외.
- `complete` / `cancel`은 직접 status를 바꾸는 게 아니라 **도메인 객체의 전이 메서드**를 호출하는 형태로 구현 (§4 규칙).
- `complete`는 §5의 outbox 흐름을 따른다 — 응답은 "완료 확정(상태 변경 + 발행 예약까지 동기 보장)"이지 "재고 반영 완료"가 아님 (재고는 비동기 반영). API 응답 메시지/문서에 이 의미를 반영할 것.

---

## 8. 기술 스택 / 컨벤션

- Language / Framework: Java 21 + Spring Boot 4.0.6
- Build: **Gradle**
- DB: **PostgreSQL** (Procurement 전용) · 스키마 변경은 마이그레이션으로 관리 (V5 = confirmed 컬럼 제거, V6 = outbox topic 컬럼, V7 = so_id → so_number)
- 동기 HTTP 클라이언트: **Spring RestClient** (Item 단가·부품명 조회 전용) — Feign 미사용. Inventory 연동은 Kafka only.
- Kafka: **`org.springframework.boot:spring-boot-starter-kafka`** — raw `org.springframework.kafka:spring-kafka`만 넣으면 Boot 4.0 모듈 분리로 `KafkaTemplate` 빈이 안 생겨 **기동 실패** (sales에서 실증)
- 직렬화: JSON 문자열 (key/value String serializer, 타입 헤더 없음) — Jackson 2/3 혼재 무관
- 아키텍처 스타일: 도메인별 패키징 → 후에 헥사고날 아키텍처 도입 예상
- 인증: ⚠️ Gateway에서 JWT 검증 후 헤더 전달? 서비스에서 재검증?

> ⚠️ 항목 확정 전까지 Claude는 해당 스택 관련 코드를 임의로 생성하지 말고 먼저 물어볼 것.

---

## 9. 구현 현황 / 체크리스트 (procurement 분)

- [x] `outbox_event` 테이블 + MessageRelay(1초 폴링) 완비
- [x] V5 마이그레이션: `confirmed_by` / `confirmed_at` 컬럼 제거 + status enum CONFIRMED 제거 (이슈 #25)
- [x] `po.confirm()` 제거, `complete()` (DRAFT→RECEIVED) 구현 (이슈 #25)
- [x] `POST .../complete` 유스케이스 + 엔드포인트 (이슈 #25)
- [x] V6 마이그레이션: outbox_event `topic` 컬럼 추가 (이슈 #25)
- [x] `spring-boot-starter-kafka` + 브로커 설정 (이슈 #28)
- [x] `MessageRelay.publish()` → `KafkaTemplate.send(topic, key, payload)` 교체 (이슈 #28)
- [x] `VendorCreated` · `DomainEventOutboxRelay` · `DomainEvent` 제거 (이슈 #27)
- [x] V7 마이그레이션: `so_id` → `so_number` 컬럼 rename (이슈 #25 후속)
- [ ] **ItemRestClient 구현 + PO 작성 시 SKU별 단가·부품명 자동 조회** (이슈 #29 진행 중)
- [ ] Gateway JWT 검증 방식 확정 및 구현 (⚠️ 팀 결정 필요)
- [ ] Sales → Procurement PO 생성 트리거 방식 확정 (⚠️ 팀 결정 필요)

### 빌드 / 실행 / 테스트 명령

```bash
./gradlew build       # 빌드
./gradlew test        # 테스트
./gradlew bootRun     # 로컬 실행
```

---

## 10. 작업 시 주의사항 (Claude에게 주는 규칙)

- 상태 머신은 **DRAFT / RECEIVED / CANCELED 3개뿐**. CONFIRMED를 enum·메서드·테스트·주석 어디에도 다시 들이지 않는다.
- 상태 전이 로직은 **반드시 도메인 객체 내부**에 둔다. 서비스 레이어에서 상태를 직접 바꾸지 않는다.
- PO 라인의 부품명·단가는 **항상 스냅샷**으로 저장한다. Item을 런타임에 join하지 않는다.
- 다른 서비스 DB에 직접 접근하지 않는다. API/이벤트만 사용.
- **Inventory 연동에 동기 HTTP 코드를 만들지 않는다** — 반드시 §5의 outbox → Kafka 흐름. RestClient는 Item 단가·부품명 조회 전용.
- 완료 처리는 "PO 상태 변경(DRAFT→RECEIVED) + outbox INSERT"가 **한 트랜잭션**(동기)으로 함께 성공/실패하도록 작성한다. Kafka 발행은 relay가 비동기로.
- `eventId`는 outbox INSERT 시점에 1회 생성·저장한다. relay가 발행할 때마다 새로 만들지 않는다.
- 이벤트 DTO는 [계약 문서](./eda-event-spec.md)의 record를 **그대로 복붙**한다. 필드 추가는 nullable만, 변경은 계약 PR로.
- `occurredAt`은 UTC `Instant` 문자열. LocalDateTime 직렬화 금지. `DomainEvent` 인터페이스는 제거됨 — DTO 직접 직렬화.
- 이벤트의 단가는 원화 정수(int) — BigDecimal은 `intValueExact()`.
- SCS(Spring Cloud Stream) 도입 금지 (4팀 합의: plain spring-kafka 통일).
- `⚠️ 확인 필요` 항목은 추측해서 코드에 박지 말고 먼저 질문할 것.
