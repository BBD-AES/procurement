# Inventory 안전재고 미달 목록 조회 API — 요청서

> **요청 주체:** Procurement(구매) 도메인
> **수신 주체:** Inventory(재고) 도메인
> **목적:** 안전재고 미달 품목 보충(replenishment)을 위한 동기 조회 API 신설 요청
> **연동 형태:** Procurement → Inventory **동기 HTTP 호출** (Spring HTTP Interface `@HttpExchange`, 기존 Item·Sales 연동과 동일 패턴)
> **상태:** 제안(Draft) — 아래 §5 합의 항목 확정 후 계약 확정

---

## 1. 배경 / 왜 필요한가

지금까지 Procurement의 PO 작성 트리거는 **Sales 백오더 알림** 하나뿐이었다. 여기에 더해 **안전재고(safety_stock) 미달 품목을 보충하기 위한 PO** 를 작성해야 한다.

안전재고 미달 여부는 `safety_stock`·현재고를 소유한 **Inventory만 판정할 수 있다.** Procurement는 해당 데이터를 갖고 있지 않으며, 마이크로서비스 원칙상 Inventory DB에 직접 접근할 수도 없다(API/이벤트로만 접근).

또한 품목이 약 100만 개, 창고·지점이 수십 개 규모이므로 **"전체 재고를 내려받아 Procurement에서 거르는" 방식은 불가**하다. **미달 판정·필터링은 데이터 주인인 Inventory가 서버측에서 수행**하고, Procurement에는 **이미 걸러진 작은 결과(미달분)만** 전달해야 한다.

따라서 Inventory가 "안전재고 미달 목록"을 반환하는 조회 API를 신설해주기를 요청한다.

---

## 2. 책임 경계 (중요)

요청 범위를 명확히 하기 위해 책임을 먼저 못 박는다.

| 항목 | 책임 주체 | 비고 |
|---|---|---|
| 안전재고 미달 **판정·필터링** | **Inventory** | `safety_stock`·현재고 소유자. 미달 행만 추려 반환 |
| 발주중(on-order) 수량 **차감** | **Procurement** | 열린 PO는 Procurement가 소유. **Inventory는 차감하지 않음** |
| 작업카드(보충 worklist) 생성·관리 | **Procurement** | 미달 목록을 받아 인박스에 적재, 담당자가 PO 작성 |

> ⚠️ **Inventory에 요청하는 범위는 "재고 대비 안전재고 미달분"이라는 날것까지다.** "이미 발주해둔 수량 빼기"는 Inventory가 열린 PO를 모르므로 Procurement가 로컬에서 처리한다. Inventory는 on-order를 신경 쓸 필요가 없다.

---

## 3. 요청 API 스펙 (제안)

### 엔드포인트

```
GET /api/v1/stocks/below-safety-stock
```

> 경로·네이밍은 Inventory 컨벤션에 맞춰 조정 가능. 의미("안전재고 미달 목록 조회")만 유지되면 된다.

### 요청 파라미터 (Query)

| 파라미터 | 타입 | 필수 | 설명 |
|---|---|---|---|
| `warehouseCode` | String | 선택 | 특정 창고로 한정. 미지정 시 전체 창고 |
| `page` | int | 선택 | 페이지 번호 (0-base). 대량 대비 페이지네이션 권장 |
| `size` | int | 선택 | 페이지 크기 (예: 500) |
| `updatedSince` | String(ISO-8601) | 선택(권장) | 해당 시각 이후 변동분만 — 델타 조회로 부하 절감 |

### 응답 본문 (Success)

라인 배열. 각 라인은 (sku, warehouseCode) 단위의 미달 1건.

| 필드 | 타입 | 설명 |
|---|---|---|
| `sku` | String | 부품 식별자 |
| `warehouseCode` | String | 창고 코드 (예: `WH-HQ-001`) |
| `availableQty` | int | 가용재고 (정의는 §5-2 합의 필요) |
| `safetyStock` | int | 해당 (sku, 창고)의 안전재고 기준 |
| `shortageQty` | int | 미달 수량 = (목표선 − availableQty), Inventory가 계산 (목표선 정의는 §5-1 합의 필요) |

#### 응답 예시

```json
[
  {
    "sku": "SKU-1001",
    "warehouseCode": "WH-HQ-001",
    "availableQty": 30,
    "safetyStock": 100,
    "shortageQty": 70
  },
  {
    "sku": "SKU-2050",
    "warehouseCode": "WH-HQ-001",
    "availableQty": 5,
    "safetyStock": 40,
    "shortageQty": 35
  }
]
```

> Inventory 표준 응답 래퍼(`ApiResponse<T>` 등)가 있으면 그 안에 담아도 무방. Procurement는 라인 배열만 파싱한다.

---

## 4. 비기능 요구사항 (대규모 대비)

1. **서버측 필터링 필수** — `available < safety_stock` 조건을 Inventory가 직접 적용해 **미달 행만** 반환한다. 전체 재고를 반환하지 않는다.
2. **인덱스 지원 권장** — 컬럼-대-컬럼 비교(`available < safety_stock`)는 일반 인덱스로 안 타므로, 부분 인덱스(partial index) 또는 `shortage` 생성컬럼 인덱스 등으로 미달 행만 스캔되게 한다.
3. **페이지네이션 지원** — 미달 건수가 많을 때를 대비.
4. **(권장) 델타 조회** — `updatedSince` 기준 변동분만 제공하면 주기 호출 부하가 크게 준다.
5. **호출 패턴** — Procurement는 이 API를 **사용자 요청 경로가 아니라 백그라운드 스케줄러**에서 주기적으로 호출한다(핵심 API와 장애 격리). 따라서 실시간 초저지연보다 **안정적 대량 응답**이 중요하다.

---

## 5. 합의가 필요한 항목 (확정 전 계약 미완)

> 아래 3가지는 숫자 정합성에 직결된다. Inventory 팀과 합의 후 본 스펙을 확정한다.

### 5-1. 보충 목표선 (shortageQty 계산 기준)

미달이 발동했을 때 **어디까지 채울 것인가**:

- (A) 안전재고선까지만: `shortageQty = safetyStock − availableQty`
- (B) 그 위 목표재고/최대치까지: `shortageQty = targetLevel − availableQty` (reorder point는 safety_stock, order-up-to level은 그보다 높음)

일반적인 ERP는 (B)에 가깝다(안전재고에 닿으면 발동, 더 높은 목표선까지 보충). **(B)로 갈 경우 `targetLevel`/`reorderPoint` 필드를 Inventory가 보유·관리하는지 확인 필요.**

### 5-2. "가용재고(availableQty)"의 정의

- 물리 현재고(on-hand) 그대로인가, 아니면 **예약(allocated/reserved) 물량을 뺀 가용(available-to-promise)** 인가?
- 백오더 등으로 이미 잡아둔 물량과 **이중 계산되지 않도록** 정의를 맞춘다.

### 5-3. on-order 차감 책임 재확인

- on-order(발주중 수량) 차감은 **Procurement가 로컬에서 수행**한다. Inventory는 차감하지 않는다(§2 참조). → 합의 시 명시만 하면 됨.

---

## 6. 전체 흐름 (참고)

```
[Inventory]                         [Procurement]                         [프론트]
미달 판정·필터링                     백그라운드 스케줄러
(safety_stock 소유)                  ① below-safety-stock 호출  ─────►  (해당 없음)
                  ◄───────────────  ② 미달 목록(필터링된 작은 결과) 반환
                                     ③ 로컬에서 on-order 차감
                                        (순부족분 = shortageQty − 발주중)
                                     ④ 순부족분 > 0 → 작업카드 인박스 upsert
                                                                          ⑤ 인박스 조회
                                                                          ⑥ 담당자가 PO 작성
```

- Inventory와 Procurement는 **이 한 방향 호출(①②)로만** 만난다. Inventory가 Procurement를 부르거나 DB를 보는 일은 없다.
- on-order 차감·작업카드·PO 작성은 전부 Procurement 내부 일이다.

---

## 7. 에러 / 엣지 케이스 (제안)

| 상황 | 기대 동작 |
|---|---|
| 미달 품목 없음 | 빈 배열 `[]` 반환 (에러 아님) |
| 존재하지 않는 `warehouseCode` | 빈 배열 또는 400 — Inventory 정책에 따름 |
| Inventory 일시 장애/타임아웃 | Procurement는 해당 주기를 **graceful skip**, 다음 주기 재시도 (작업카드 유실 없음) |
| 동일 (sku, 창고) 중복 라인 | 발생하지 않도록 (sku, 창고) 유일 보장 권장 |

---

## 8. 연동 기술 메모 (Procurement 측)

- Procurement는 `InventoryHttpService`(`@HttpExchange`)로 이 API를 호출한다. (스텁·`bbd-inventory-service` base-url 설정 기존재)
- 헥사고날 구조에 맞춰 `LoadInventoryShortagePort` + `InventoryClientAdapter`로 감싸 **anti-corruption 계층**을 둔다 → Inventory 도메인 개념 누수 최소화.
- 호출은 `@Scheduled` 백그라운드 작업에서 수행 (사용자 동기 경로와 분리).

---

## 9. 요청 요약 (한 줄)

**Inventory가 "안전재고 미달인 (sku, 창고, 가용재고, 안전재고, 부족수량)" 목록을 서버측 필터링·페이지네이션으로 반환하는 `GET /api/v1/stocks/below-safety-stock` 조회 API를 신설**해주기를 요청합니다. on-order 차감·작업카드·PO 작성은 Procurement가 담당하며, §5의 3가지(목표선 정의 / 가용재고 정의 / 차감 책임)만 합의되면 계약을 확정합니다.
