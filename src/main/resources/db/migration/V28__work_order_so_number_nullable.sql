-- 작업지시(WO) 연계 SO를 선택값으로 전환.
-- 안전재고 보충(made-to-stock) 생산은 판매주문(SO) 없이도 작업지시를 생성할 수 있어야 한다.
-- (완료 시 StockInRequested.soNumber=null 허용, 백오더 충당은 soNumber 있을 때만 동작 — 도메인이 null-safe)
ALTER TABLE work_order ALTER COLUMN so_number DROP NOT NULL;

COMMENT ON COLUMN work_order.so_number IS '원 수주번호 (nullable — SO 연계 생산일 때만. 안전재고 보충 생산은 null)';
