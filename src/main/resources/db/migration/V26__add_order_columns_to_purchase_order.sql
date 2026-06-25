-- PO 상태머신 DRAFT → ORDERED → RECEIVED 2단계 분리.
-- 주문(ORDERED) 전이 시점의 처리자/시각 컬럼 추가. 재고 반영은 입고완료(RECEIVED) 시점에만 일어난다.
ALTER TABLE purchase_order ADD COLUMN ordered_by BIGINT;
ALTER TABLE purchase_order ADD COLUMN ordered_at TIMESTAMP;

COMMENT ON COLUMN purchase_order.ordered_by IS '주문 처리자 사번 (ORDERED 진입 시 박힘)';
COMMENT ON COLUMN purchase_order.ordered_at IS '주문 시각 (DRAFT -> ORDERED)';

COMMENT ON COLUMN purchase_order.status IS '상태: DRAFT / ORDERED / RECEIVED / CANCELED';
