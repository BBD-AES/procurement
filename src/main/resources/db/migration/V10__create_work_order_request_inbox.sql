-- 이슈 #58: 생산(MAKE) 요청 알림 인박스
-- sales.purchase-requested의 MAKE 라인 → 별도 인박스 저장 (B안: 담당자가 보고 작업지시 생성)
-- 멱등으로 공유

CREATE TABLE work_order_request_notification (
                                                 id             BIGSERIAL    PRIMARY KEY,
                                                 event_id       VARCHAR(36)  NOT NULL,
                                                 so_number      VARCHAR(30)  NOT NULL,
                                                 warehouse_code VARCHAR(20)  NOT NULL,
                                                 payload        TEXT         NOT NULL,
                                                 status         VARCHAR(20)  NOT NULL,
                                                 received_at    TIMESTAMP    NOT NULL,
                                                 created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                                 CONSTRAINT uk_work_order_request_notification_event_id UNIQUE (event_id)
);

-- 상태값 조회를 위한 인덱스 생성
CREATE INDEX idx_work_order_request_notification_status
    ON work_order_request_notification (status);

-- 테이블 및 컬럼 코멘트 등록
COMMENT ON TABLE  work_order_request_notification                 IS 'MAKE(생산) 요청 알림 (B안: 담당자가 보고 작업지시 생성)';
COMMENT ON COLUMN work_order_request_notification.so_number      IS '연관 수주번호 (작업지시 생성 시 WorkOrder.soNumber로 보관)';
COMMENT ON COLUMN work_order_request_notification.warehouse_code IS '입고 목적지 창고';
COMMENT ON COLUMN work_order_request_notification.payload        IS '수신 이벤트 중 MAKE 라인만 필터한 JSON (PurchaseRequested 형식 유지)';
COMMENT ON COLUMN work_order_request_notification.status         IS 'PENDING / DONE (그 soNumber로 작업지시 생성되면 DONE)';