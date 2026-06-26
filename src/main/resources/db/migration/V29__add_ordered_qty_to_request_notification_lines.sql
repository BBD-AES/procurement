-- on-order(발주중/생산중) 수량 추적: 요청 알림 라인에 ordered_qty 추가.
-- 발주(PO DRAFT→ORDERED)·작업지시(WO 생성) 시점에 누적되고,
-- 입고완료(PO RECEIVED)/생산완료(WO COMPLETED) 시 fulfilled_qty 로 이동,
-- 작업지시 취소 시 해제(decrement)된다.
-- 이로써 라인 수량을 4단계로 추적한다:
--   requested(요청) / ordered(발주중) / fulfilled(입고완료) / 미발주잔여(=requested-ordered-fulfilled)
-- 기존 행은 0 으로 백필(과거 발주중 수량은 복원 불가, 입고완료분은 fulfilled_qty 에 이미 반영됨).

ALTER TABLE po_request_notification_line
    ADD COLUMN ordered_qty INT NOT NULL DEFAULT 0;

ALTER TABLE work_order_request_notification_line
    ADD COLUMN ordered_qty INT NOT NULL DEFAULT 0;

COMMENT ON COLUMN po_request_notification_line.ordered_qty        IS '발주(PO ORDERED)됐으나 아직 입고 안 된 누적 수량(발주중)';
COMMENT ON COLUMN work_order_request_notification_line.ordered_qty IS '작업지시(WO) 생성됐으나 아직 완료 안 된 누적 수량(생산중)';
