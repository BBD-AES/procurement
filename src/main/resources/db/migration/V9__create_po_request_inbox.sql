-- 이슈 #48: sales.purchase-requested 구독 인프라
-- (1) 이벤트 멱등 가드 + (2) 발주 요청 알림 저장 (B안 — 사람이 보고 PO 작성)

-- (1) 멱등 가드: 같은 eventId 중복 배달(at-least-once) 방어 (계약서 §4)
CREATE TABLE processed_event (
                                 id           BIGSERIAL    PRIMARY KEY,
                                 event_id     VARCHAR(36)  NOT NULL,
                                 processed_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT uk_processed_event_event_id UNIQUE (event_id)
);

COMMENT ON TABLE  processed_event          IS '소비 완료 이벤트 멱등 가드 (eventId 디둡)';
COMMENT ON COLUMN processed_event.event_id  IS '이벤트 1건당 UUID (계약 멱등 키)';


-- (2) 발주 요청 알림 저장 (payload JSON으로 보존)
CREATE TABLE po_request_notification (
                                         id             BIGSERIAL    PRIMARY KEY,
                                         event_id       VARCHAR(36)  NOT NULL,
                                         so_number      VARCHAR(30)  NOT NULL,
                                         warehouse_code VARCHAR(20)  NOT NULL,
                                         payload        TEXT         NOT NULL,
                                         status         VARCHAR(20)  NOT NULL,
                                         received_at    TIMESTAMP    NOT NULL,
                                         created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                         CONSTRAINT uk_po_request_notification_event_id UNIQUE (event_id)
);

CREATE INDEX idx_po_request_notification_status ON po_request_notification (status);

COMMENT ON TABLE  po_request_notification                IS 'sales 발주 요청 알림 (B안: 담당자가 보고 수동 PO 작성)';
COMMENT ON COLUMN po_request_notification.so_number      IS '연관 수주번호 (= 이벤트 key). PO 작성 시 PO.soNumber로 보관';
COMMENT ON COLUMN po_request_notification.warehouse_code IS '입고 목적지 창고 (수주 도착창고)';
COMMENT ON COLUMN po_request_notification.payload        IS '수신 이벤트 전체 JSON (lines 포함 — 후속 A안에서 재사용)';
COMMENT ON COLUMN po_request_notification.status         IS 'PENDING / DONE (그 soNumber로 PO 작성되면 DONE)';