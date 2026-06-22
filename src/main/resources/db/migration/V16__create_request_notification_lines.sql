-- 이슈 #1(B안): 요청 알림 "라인 단위 부분 충족" 추적
-- 기존에는 알림 헤더(status PENDING/DONE)만 있어, 같은 soNumber로 PO/WorkOrder를 만들면
-- 충족 여부와 무관하게 그 soNumber의 PENDING 알림이 통째로 DONE 처리되는 문제가 있었다.
-- 라인별 requested_qty / fulfilled_qty 를 추적해, 완료(RECEIVED/COMPLETED) 시점에 충당된 수량만 반영한다.

-- ─────────────────────────────────────────────
-- (1) 구매(BUY) 요청 알림 라인
-- ─────────────────────────────────────────────
CREATE TABLE po_request_notification_line (
    id              BIGSERIAL    PRIMARY KEY,
    notification_id BIGINT       NOT NULL REFERENCES po_request_notification(id),
    sku             VARCHAR(100) NOT NULL,
    requested_qty   INT          NOT NULL,
    fulfilled_qty   INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'  -- PENDING / PARTIAL / DONE
);

CREATE INDEX idx_po_req_noti_line_notification ON po_request_notification_line (notification_id);

COMMENT ON TABLE  po_request_notification_line               IS '구매요청 알림의 라인별 충족 추적 (이슈 #1 B안)';
COMMENT ON COLUMN po_request_notification_line.requested_qty IS 'Sales가 요청한 원 수량';
COMMENT ON COLUMN po_request_notification_line.fulfilled_qty IS 'PO 완료(RECEIVED)로 충당된 누적 수량';
COMMENT ON COLUMN po_request_notification_line.status        IS 'PENDING / PARTIAL / DONE (라인 단위)';

-- ─────────────────────────────────────────────
-- (2) 생산(MAKE) 요청 알림 라인
-- ─────────────────────────────────────────────
CREATE TABLE work_order_request_notification_line (
    id              BIGSERIAL    PRIMARY KEY,
    notification_id BIGINT       NOT NULL REFERENCES work_order_request_notification(id),
    sku             VARCHAR(100) NOT NULL,
    requested_qty   INT          NOT NULL,
    fulfilled_qty   INT          NOT NULL DEFAULT 0,
    status          VARCHAR(20)  NOT NULL DEFAULT 'PENDING'
);

CREATE INDEX idx_wo_req_noti_line_notification ON work_order_request_notification_line (notification_id);

COMMENT ON TABLE work_order_request_notification_line IS '생산요청 알림의 라인별 충족 추적 (이슈 #1 B안)';

-- ─────────────────────────────────────────────
-- (3) 기존 알림 백필 — payload JSON의 lines[]를 라인 테이블로 전개
--     이미 DONE인 알림은 fulfilled = requested 로 채워 종료 상태 유지,
--     PENDING 알림은 fulfilled 0 으로 시작.
-- ─────────────────────────────────────────────
INSERT INTO po_request_notification_line (notification_id, sku, requested_qty, fulfilled_qty, status)
SELECT n.id,
       l ->> 'sku',
       (l ->> 'quantity')::int,
       CASE WHEN n.status = 'DONE' THEN (l ->> 'quantity')::int ELSE 0 END,
       CASE WHEN n.status = 'DONE' THEN 'DONE' ELSE 'PENDING' END
FROM po_request_notification n,
     LATERAL json_array_elements(n.payload::json -> 'lines') AS l;

INSERT INTO work_order_request_notification_line (notification_id, sku, requested_qty, fulfilled_qty, status)
SELECT n.id,
       l ->> 'sku',
       (l ->> 'quantity')::int,
       CASE WHEN n.status = 'DONE' THEN (l ->> 'quantity')::int ELSE 0 END,
       CASE WHEN n.status = 'DONE' THEN 'DONE' ELSE 'PENDING' END
FROM work_order_request_notification n,
     LATERAL json_array_elements(n.payload::json -> 'lines') AS l;
