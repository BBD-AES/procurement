# 프론트엔드 요구서 — 요청 대기(구매/생산) 탭 개선

> 대상: 프론트 담당
> 백엔드(Procurement): 본 문서의 데이터/엔드포인트는 **이미 구현 완료**. 프론트는 이 계약대로 화면만 맞추면 된다.
> 관련 백엔드 변경: 알림 라인 4단계 수량(`ordered_qty`) 추가, 요청 "처리중 클레임" 기능, SO별 PO/WO 역조회 필터.

---

## 0. 한 줄 요약

요청 대기 탭(발주/생산)에서 **부분 발주 후 "뭘 했고 뭘 더 해야 하는지"가 안 보여서** 헷갈리고, **상태가 '미처리'로만 떠서** 다른 점원이 중복 발주하는 문제를 고친다.
→ ① 주문 단위로 표시 통일 ② 라인별 4단계 수량 표시 ③ "처리중(담당자)" 표시로 중복 발주 방지.

---

## 1. 작업 범위 (프론트가 할 일)

| # | 요구 | 핵심 |
|---|---|---|
| ① | 요청 대기 탭을 **주문(soNumber) 단위 1행**으로 통일 | 특히 **생산 탭이 지금 아이템별로 펼쳐져 있음 → 주문 1행으로** |
| ② | 행을 펼치면 **라인별 4단계 수량** 표시 + 주문서는 **미발주 잔여로 prefill** | `remainingToOrderQty` 사용, 끝난 라인 숨김 |
| ③ | **상태 라벨 정확히** + **"처리중(담당자)" 표시 + claim/release 연동** | 중복 발주 방지 |

> 백엔드는 데이터·상태·엔드포인트를 모두 제공한다. 화면 렌더링·호출 타이밍만 프론트 몫.

---

## 2. 공통 API 규약

- Base path: `/api/v1`
- 인증: 기존과 동일(JWT). 권한: 모든 요청 알림 API = `HQ_MANAGER`, `HQ_STAFF`.
- **성공 응답 래퍼**
  ```json
  { "code": "SUCCESS", "message": "OK", "data": { ... } }
  ```
  (실데이터는 항상 `data` 안. null 필드는 응답에서 생략될 수 있음 — `NON_NULL`)
- **에러 응답** (ProblemDetail / RFC7807 형태)
  ```json
  { "title": "R002", "detail": "이미 다른 담당자가 처리 중인 요청입니다.", "status": 409, "timestamp": "..." }
  ```
  → 프론트는 `status`(HTTP 코드)와 `title`(에러코드)로 분기, `detail`을 토스트 메시지로 사용.

---

## 3. 요청 알림 데이터 모델

발주 요청(BUY)·생산 요청(MAKE) **응답 구조 동일**. 알림 1건 = **주문 1개(soNumber)**, 그 안에 라인 배열.

```jsonc
{
  "eventId": "evt-uuid",          // 알림 고유 id (claim/release 호출 시 사용)
  "soNumber": "SO-2026-000123",   // 주문(요청 대기 행의 단위)
  "warehouseCode": "WH-HQ-001",
  "status": "PARTIAL",            // PENDING | PARTIAL | DONE  (주문 전체 상태)
  "receivedAt": "2026-06-26T10:00:00",
  "claimedBy": 1024,              // 처리중 담당자 userId (null=아무도 처리 안 함)
  "claimedByName": "김담당",       // 처리중 담당자 표시명 (null 가능)
  "claimedAt": "2026-06-26T10:05:00", // 클레임 시각 (null 가능)
  "lines": [
    {
      "sku": "A-001",
      "requestedQty": 3,          // ① 요청 원수량
      "orderedQty": 2,            // ② 발주중(발주/작업지시 했으나 미입고·미완료)
      "fulfilledQty": 0,          // ③ 입고완료(BUY) / 생산완료(MAKE)
      "remainingQty": 3,          // 미입고분 = requested - fulfilled (발주중 포함)
      "remainingToOrderQty": 1,   // ★ 아직 발주 안 한 양 = requested - ordered - fulfilled
      "status": "PARTIAL"         // 라인 단위 상태
    }
  ]
}
```

**4단계 수량 관계 (반드시 이 식으로 이해)**
```
요청(requestedQty) = 발주중(orderedQty) + 입고완료(fulfilledQty) + 미발주잔여(remainingToOrderQty)
```
- **주문서/발주 폼 prefill = `remainingToOrderQty`** (요청수량·remainingQty 아님!)
- 라인 `status == "DONE"` 또는 `remainingToOrderQty == 0` → **그 라인은 더 발주할 것 없음** (주문서에서 숨기거나 비활성)

---

## 4. 엔드포인트

| 메서드 | 엔드포인트 | 용도 |
|---|---|---|
| GET | `/api/v1/purchase-requests` | 발주 요청 알림 목록(활성 PENDING/PARTIAL) |
| POST | `/api/v1/purchase-requests/{eventId}/claim` | 발주 요청 처리중 선점 |
| POST | `/api/v1/purchase-requests/{eventId}/release` | 발주 요청 처리중 해제(본인만) |
| GET | `/api/v1/work-order-requests` | 생산 요청 알림 목록(활성 PENDING/PARTIAL) |
| POST | `/api/v1/work-order-requests/{eventId}/claim` | 생산 요청 처리중 선점 |
| POST | `/api/v1/work-order-requests/{eventId}/release` | 생산 요청 처리중 해제(본인만) |
| GET | `/api/v1/purchase-orders?soNumber=SO-...` | 그 주문으로 만든 PO 목록(역조회) |
| GET | `/api/v1/work-orders?soNumber=SO-...` | 그 주문으로 만든 작업지시 목록(역조회) |
| GET | `/api/v1/purchase-orders/stats` | 배지 숫자(대기 요청 수) 포함 대시보드 집계 |

- claim/release 응답 = **갱신된 알림 객체**(§3 구조) → 받아서 그 행만 갱신하면 됨.
- 목록은 **활성(PENDING/PARTIAL)만** 내려옴. DONE은 목록에서 자동 제외.

---

## 5. 화면별 요구사항

### 5.1 요청 대기 탭 목록 (① 표시 통일)

- **행 = 주문(soNumber) 1개.** 발주 탭은 이미 그렇고, **생산 탭은 현재 `lines`를 아이템별로 펼쳐 N행 → 주문 1행으로 변경.**
- 행에 표시 권장: soNumber, 상태 배지(§6), 진행 요약(예: "3개 품목 중 1개 발주 대기"), 처리중 표시(§5.4), receivedAt.
- 정렬: 응답이 최신순(receivedAt desc).

### 5.2 배지 숫자 (요청 대기 옆 수)

- `GET /api/v1/purchase-orders/stats` 응답의 `data.pendingPurchaseRequests`(발주), `data.pendingWorkOrderRequests`(생산) 사용.
- 의미 = **활성(PENDING+PARTIAL) 주문 수** → 목록 행 수와 일치(주문 단위). 라인 수로 세지 말 것.

### 5.3 상세 / 주문서 (② 4단계 수량)

행을 펼치거나 상세로 들어가면 **라인 테이블**:

| SKU | 부품명 | 요청 | 발주중 | 입고완료 | 미발주 잔여 | 상태 |
|---|---|---|---|---|---|---|
| A-001 | … | requestedQty | orderedQty | fulfilledQty | **remainingToOrderQty** | line.status |

- **주문서(발주 폼) 자동 채움 = `remainingToOrderQty`.** (요청수량으로 채우면 중복 발주됨)
- `remainingToOrderQty == 0` 또는 `line.status == "DONE"` 라인은 **주문서에서 숨김/비활성** → "A 1개만" 뜨고 "B 2개"는 안 뜨게.
- (선택) 발주중 수량은 노란색, 입고완료는 초록, 미발주 잔여는 회색 등으로 시각 구분 권장.

### 5.4 처리중(claim) 연동 (③ 중복 발주 방지) — 핵심

**표시**
- `claimedBy != null` → 행/상세에 **"처리중 · {claimedByName}"** 표시.
- 처리중인 요청이 **다른 사람(내 userId ≠ claimedBy)** 것이면 → 발주/작업지시 버튼 **비활성** + "OO 담당자가 처리 중" 안내.

**호출 타이밍 (권장)**
- 담당자가 요청을 **열거나 "발주 시작" 누를 때 → `claim` 호출.**
- 처리 끝/취소/화면 이탈 시 → `release` 호출(본인 것만 가능).

**claim 응답 분기**
- 200 → 내가 선점. 응답의 claim 필드로 화면 갱신, 발주 진행 허용.
- **409 (`title: "R002"`)** → 이미 다른 담당자 처리중. 진행 막고 "이미 처리 중인 요청" 안내 + 목록 새로고침.
- 404 (`R001`) → 알림 없음(이미 완료되어 사라졌을 수 있음). 목록 새로고침.

**release 응답 분기**
- 200 → 해제됨.
- 403 (`title: "R003"`) → 본인이 선점한 게 아님. (보통 버튼을 본인에게만 노출하므로 방어용)

**takeover 규칙(백엔드 자동)**
- 클레임은 **30분 지나면 만료** → 다른 담당자가 claim하면 자동으로 가져옴(409 안 남). 프론트는 별도 처리 불필요, claim 결과만 따르면 됨.

### 5.5 역조회 — "이 주문으로 뭘 발주했나"

- 상세에서 "발주 내역/작업지시 내역" 보여줄 때:
  - 발주: `GET /api/v1/purchase-orders?soNumber={soNumber}`
  - 생산: `GET /api/v1/work-orders?soNumber={soNumber}`
- 각 PO/WO의 상태(DRAFT/ORDERED/RECEIVED 등)와 함께 표시하면 "발주됨·입고대기 / 입고완료" 추적 가능.

---

## 6. 상태 → 라벨 매핑 (② "미처리" 대신 "부분 발주중")

| status | 발주(BUY) 라벨 | 생산(MAKE) 라벨 | 의미 |
|---|---|---|---|
| `PENDING` | 미처리 | 미처리 | 아무도 발주/작업지시 시작 안 함 |
| `PARTIAL` | **부분 발주중** | **부분 작업중** | 일부 발주/작업지시·입고됨 (진행 중) |
| `DONE` | 완료 | 완료 | 전량 입고/생산완료 (목록에서 사라짐) |

> 지금 PARTIAL을 "미처리"로 뭉뚱그려 표시 중 → **PARTIAL은 "부분 발주중"으로 구분**할 것. 그래야 다른 점원이 "이미 진행 중"임을 알고 중복 발주 안 함.
> 추가로 `claimedBy != null`이면 status와 별개로 "처리중(담당자)" 뱃지를 함께.

---

## 7. 에러 코드 (claim/release)

| HTTP | title(코드) | 의미 | 프론트 처리 |
|---|---|---|---|
| 409 | `R002` | 이미 다른 담당자가 처리 중 | 진행 차단 + 안내 + 목록 새로고침 |
| 404 | `R001` | 요청 알림 없음(완료/삭제됨) | 목록 새로고침 |
| 403 | `R003` | 본인 것만 해제 가능 | (방어용) 무시 또는 안내 |

---

## 8. 예시 시나리오 (생산 요청, A=3 / B=2)

1. 요청 수신 → 목록에 **주문 1행**(A3·B2), status `PENDING`(미처리).
2. 김담당이 요청 열기 → `claim` 호출 → status 표시 옆 "처리중·김담당", 다른 점원에겐 버튼 비활성.
3. 김담당이 A 2개·B 2개 작업지시 후 완료 → 라인: A(req3, fulfilled2, **미발주잔여1**, PARTIAL), B(req2, fulfilled2, **미발주잔여0**, DONE). 주문 status `PARTIAL`.
4. 다시 그 주문 상세 진입 → 주문서엔 **A 1개만** 뜸(B는 미발주잔여 0 → 숨김). ← 기존 "A1·B2" 버그 해소.
5. 작업 끝/이탈 → `release`. (남은 A 1개는 다른 담당자가 이어서 처리 가능)

---

## 9. 주의

- "쌓이는 단위"는 백엔드가 이미 주문(soNumber) 단위. 생산 탭만 `lines`를 펼치지 않도록 바꾸면 됨.
- 주문서 prefill에 `requestedQty`/`remainingQty` 쓰지 말 것 → **`remainingToOrderQty`** 만.
- claim은 "소프트 락(표시용)"이다. 물리적 발주 자체를 막진 않으니, **반드시 처리중 표시 + 버튼 비활성으로 UX 차단**해야 중복 발주가 방지된다.
