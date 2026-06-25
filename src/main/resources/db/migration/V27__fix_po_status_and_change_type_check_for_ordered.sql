-- PO 상태머신 2단계 분리(DRAFT→ORDERED→RECEIVED)에 따른 CHECK 제약 갱신
-- 증상: 본사 매니저가 발주 상세에서 '주문' 클릭 시 500 C999.
--   (POST /purchase-orders/{po}/order 의 status=ORDERED UPDATE + change_type=ORDERED 이력 INSERT 가 CHECK 위반)
--
-- 원인: 과거 ddl-auto:update 시절 Hibernate 가 enum 컬럼에 CHECK 제약을 자동 생성했다.
--   - purchase_order.status            → CHECK (status IN ('DRAFT','CANCELED','RECEIVED'))
--   - purchase_order_history.change_type → CHECK (... 'CREATED','HEADER_UPDATED','LINES_REPLACED','COMPLETED','CANCELED')
--   ORDERED 값이 enum 에 추가됐지만 옛 CHECK 제약엔 없어 주문 전이가 제약 위반으로 실패한다.
--
-- 조치: 현재 enum 전체 값을 허용하도록 CHECK 제약을 교체한다(WO 이력 CHECK 수정 V25 와 동일 방식).
ALTER TABLE purchase_order DROP CONSTRAINT IF EXISTS purchase_order_status_check;
ALTER TABLE purchase_order ADD CONSTRAINT purchase_order_status_check
    CHECK (status IN ('DRAFT', 'ORDERED', 'RECEIVED', 'CANCELED'));

ALTER TABLE purchase_order_history DROP CONSTRAINT IF EXISTS purchase_order_history_change_type_check;
ALTER TABLE purchase_order_history ADD CONSTRAINT purchase_order_history_change_type_check
    CHECK (change_type IN ('CREATED', 'HEADER_UPDATED', 'LINES_REPLACED', 'ORDERED', 'COMPLETED', 'CANCELED'));
