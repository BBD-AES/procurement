-- [FEAT] StockInRequested에 Item 서비스 전체 정보 포함 (B안: 라인에 스냅샷 저장)
-- purchase_order_line / work_order_line 에 item 스냅샷 컬럼 추가.
-- 기존 행 보존: 문자열 컬럼은 NULL 허용, 원시타입(safety_stock/active)은 NOT NULL DEFAULT.

ALTER TABLE purchase_order_line ADD COLUMN category      VARCHAR(100);
ALTER TABLE purchase_order_line ADD COLUMN unit          VARCHAR(30);
ALTER TABLE purchase_order_line ADD COLUMN safety_stock  INT     NOT NULL DEFAULT 0;
ALTER TABLE purchase_order_line ADD COLUMN active        BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE purchase_order_line ADD COLUMN sourcing_type VARCHAR(20);

ALTER TABLE work_order_line ADD COLUMN category      VARCHAR(100);
ALTER TABLE work_order_line ADD COLUMN unit          VARCHAR(30);
ALTER TABLE work_order_line ADD COLUMN safety_stock  INT     NOT NULL DEFAULT 0;
ALTER TABLE work_order_line ADD COLUMN active        BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE work_order_line ADD COLUMN sourcing_type VARCHAR(20);

COMMENT ON COLUMN purchase_order_line.category      IS 'item 분류 스냅샷';
COMMENT ON COLUMN purchase_order_line.unit          IS 'item 단위 스냅샷';
COMMENT ON COLUMN purchase_order_line.safety_stock  IS 'item 안전재고 스냅샷';
COMMENT ON COLUMN purchase_order_line.active        IS 'item 활성여부 스냅샷';
COMMENT ON COLUMN purchase_order_line.sourcing_type IS 'item 소싱타입 스냅샷 (BUY/MAKE)';

COMMENT ON COLUMN work_order_line.category      IS 'item 분류 스냅샷';
COMMENT ON COLUMN work_order_line.unit          IS 'item 단위 스냅샷';
COMMENT ON COLUMN work_order_line.safety_stock  IS 'item 안전재고 스냅샷';
COMMENT ON COLUMN work_order_line.active        IS 'item 활성여부 스냅샷';
COMMENT ON COLUMN work_order_line.sourcing_type IS 'item 소싱타입 스냅샷 (BUY/MAKE)';
