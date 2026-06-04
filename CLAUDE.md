# CLAUDE.md — Procurement Service

> Hyundai AutoEver 3기 4차 프로젝트 / **Procurement(구매) 도메인** 백엔드
> 이 문서는 Claude Code가 이 서비스를 작업할 때 반드시 따라야 할 컨텍스트와 규칙이다.
> `⚠️ 확인 필요` 표시는 아직 팀 결정이 안 된/내가 확정 못 한 항목이니 임의로 코드에 박지 말 것.

---

## 1. 이 서비스가 하는 일 (한 줄 요약)

**"어떤 공급사에게 + 어떤 부품을 + 몇 개 + 얼마에 주문했고 + 입고했는지"** 를 관리하는 서비스.

- 사용자: 발주처(본사) 직원
- Procurement가 직접 책임지는 것:
    - 공급사(Vendor) 정보 관리 — 코드, 이름, 연락처, 거래 조건, 활성 여부
    - 구매 주문(PO) 작성 · 확정 · 취소
    - 입고 처리 및 재고 반영 (Inventory 호출)
    - 구매 단가의 **거래 시점 스냅샷** 보존
    - 안전재고 미달 시 PO 자동 생성
    - Sales 도메인으로부터 발주 요청 수신

---

## 2. 아키텍처 컨텍스트

- 6개 마이크로서비스, **서비스마다 별도 DB** (공유 DB 금지, 직접 테이블 접근 금지)
- Client(React) → **Gateway**(Spring Cloud Gateway, On-Premise) → 각 서비스(ECS Fargate)
- 메시징: **Kafka** / CI/CD: **GitLab**
- 다른 서비스 데이터가 필요하면 **반드시 그 서비스의 API/이벤트를 통해서**만 접근

### 인터페이스 (다른 서비스와의 관계)

| 방향 | 대상 | 목적 | 통신 방식 |
|---|---|---|---|
| Procurement → Inventory | 재고 증가 요청 | 입고 처리(RECEIVED) 시 | **동기 REST (Feign)** |
| Procurement → Item | 발주 가능 부품 · 현재 단가 조회 | PO 작성 시 | 동기 REST (Feign) 가정 |
| Procurement → User/Auth | 권한 · 소속 검증 | 요청 처리 시 | ⚠️ 확인 필요 |
| Sales → Procurement | 발주할 물품 리스트 요청 | PO 생성 트리거 | ⚠️ 확인 필요 |

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
├ number      // 유일
├ vendorCode  // Vendor 참조 (FK 아님, 코드 참조)
├ status      // DRAFT / CONFIRMED / RECEIVED / CANCELED
├ createdBy / confirmedBy / receivedBy   // 사번
├ createdAt / confirmedAt / receivedAt
└ lines (1:N)
```

### PurchaseOrderLine (주문 항목 한 줄)
```
PurchaseOrderLine
├ sku / partName   // 작성 시점 스냅샷
├ quantity
├ unitPrice        // 작성 시점 스냅샷
└ subtotal
```
> PO status = **PO 전체의 단계**, line = **개별 품목 줄**. 둘을 헷갈리지 말 것.
> line의 부품명/단가는 마스터를 참조하지 않고 **작성 시점 값을 자체 저장**한다.

---

## 4. 상태 머신 (State Machine)

```
DRAFT ──확정──> CONFIRMED ──입고──> RECEIVED
  │                 │
  └──── 취소 ───────┴────> CANCELED
(RECEIVED는 종료 상태, 취소 불가)
```

| 상태 | 설명 | 가능 권한 |
|---|---|---|
| DRAFT | 임시 저장 (작성·수정·삭제 가능) | HQ_STAFF, HQ_MANAGER |
| CONFIRMED | 입고 요청 대기 (공급사 전달됨) | HQ_MANAGER |
| RECEIVED | Inventory에 재고 최종 반영 완료 | — |
| CANCELED | 도중 취소 종료 | HQ_STAFF, HQ_MANAGER |

**규칙**
- 상태 전이는 **도메인 객체가 스스로 검증**한다. 서비스/컨트롤러에서 `if (status == ...)` 식으로 우회하는 구조 금지.
- 잘못된 전이는 예외를 던진다 (예: `RECEIVED → CONFIRMED` 불가).
- 가능 상태에서만 취소 가능.

---

## 5. 핵심 비즈니스 규칙 (절대 깨지면 안 됨)

1. **단가 스냅샷** — PO 라인 단가는 작성 시점 마스터 단가를 *복사*. 이후 마스터가 바뀌어도 PO 단가는 불변.
2. **멱등성** — 같은 PO를 **중복 입고 처리할 수 없다**. 재고가 두 번 늘어나면 안 됨.
3. **롤백 / 정합성** — Inventory 호출 실패 시 PO 상태 변경도 **롤백**. 입고 처리는 "PO 상태 변경 + 재고 증가"가 한 묶음으로 성립해야 한다.

### 입고 처리 흐름 (동기 REST 기준 — 순서 중요)
```
① 사전 검증: PO가 CONFIRMED인지 확인. 이미 RECEIVED면 즉시 거부 (멱등 1차 방어)
② Inventory 동기 호출: 멱등성 키(= PO 번호)와 함께 재고 증가 요청
③ 호출 성공 → 같은 트랜잭션에서 PO 상태를 RECEIVED로 커밋
   호출 실패 → 예외 던지고 PO 상태 변경 안 함 (= 롤백)
```

**주의해야 할 함정**
- 멱등성은 **양쪽 다** 건다: Procurement는 PO 상태로(이미 RECEIVED면 거부), Inventory는 멱등성 키로(같은 키 재요청 시 재고 증가 무시).
- **부분 성공** 케이스 — Inventory는 성공했는데 ③의 로컬 커밋이 실패하면? 재시도 시 Inventory가 같은 키로 중복 방어해주므로 재고는 안전. 그래서 Inventory 쪽 멱등성 키 처리가 필수다.
- 단일 DB 트랜잭션이 두 서비스에 걸칠 수 없으므로(서비스별 DB 분리), "완벽한 원자성"이 아니라 "멱등성 + 재시도로 최종 일관성 확보"가 목표임을 기억할 것.

---

## 6. 권한

| 역할 | 권한 |
|---|---|
| HQ_MANAGER | 모든 PO 작성·확정·입고·취소 + 공급사 관리 |
| HQ_STAFF | PO 작성·확정·입고 (취소는 매니저 승인 필요) |
| BRANCH | 구매 권한 없음 (구매는 본사 영역) |

---

## 7. 기술 스택 / 컨벤션

- Language / Framework: Java + Spring Boot (버전 ⚠️ 미확정)
- Build: **Gradle**
- DB: **PostgreSQL** (Procurement 전용)
- Inventory/Item 호출: **Spring Cloud OpenFeign (동기)**
- 아키텍처 스타일: ⚠️ 레이어드 / 헥사고날 등 팀 컨벤션 확정 필요
- 인증: ⚠️ Gateway에서 JWT 검증 후 헤더 전달? 서비스에서 재검증?

> 위 항목 확정 전까지 Claude는 스택 관련 코드를 임의로 생성하지 말고 먼저 물어볼 것.

---

## 8. 빌드 / 실행 / 테스트 명령

```bash
./gradlew build       # 빌드
./gradlew test        # 테스트
./gradlew bootRun     # 로컬 실행
```

---

## 9. 작업 시 주의사항 (Claude에게 주는 규칙)

- 상태 전이 로직은 **반드시 도메인 객체 내부**에 둔다. 서비스 레이어에서 상태를 직접 바꾸지 않는다.
- PO 라인의 부품명·단가는 **항상 스냅샷**으로 저장한다. Item을 런타임에 join하지 않는다.
- 다른 서비스 DB에 직접 접근하지 않는다. API/이벤트만 사용.
- Inventory 입고 호출에는 **멱등성**을 보장한다 (재시도해도 재고 한 번만 증가).
- 입고 처리는 PO 상태 변경과 재고 증가가 **함께 성공/실패**하도록 작성한다.
- `⚠️ 확인 필요` 항목은 추측해서 코드에 박지 말고 먼저 질문할 것.