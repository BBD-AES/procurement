-- PO에 발주 요청 출처 Sales Order 식별자 추가
-- 수동 작성 PO는 NULL 허용
ALTER TABLE purchase_order
    ADD COLUMN so_id VARCHAR(30);

CREATE INDEX idx_po_so_id ON purchase_order (so_id);

COMMENT ON COLUMN purchase_order.so_id IS '발주 요청 출처 Sales Order 식별자 (수동 작성 PO는 NULL)';