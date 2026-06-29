-- 낙관적 락(@Version)용 버전 컬럼 — 요청 알림 클레임(처리중 선점)의 동시 충돌 감지용.
-- 두 담당자가 같은 요청을 동시에 선점하면 둘째 flush가 version 불일치로 실패(409)한다.
-- (수량 충당은 비관적 락 FOR UPDATE 그대로 유지 — 그쪽은 변경 없음)
-- 기존 행은 0으로 초기화한다.

ALTER TABLE po_request_notification         ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE work_order_request_notification ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

COMMENT ON COLUMN po_request_notification.version         IS '낙관적 락 버전(클레임 동시 충돌 감지)';
COMMENT ON COLUMN work_order_request_notification.version IS '낙관적 락 버전(클레임 동시 충돌 감지)';
