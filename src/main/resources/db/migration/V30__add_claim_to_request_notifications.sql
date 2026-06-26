-- "처리중(담당자) 클레임" — 요청 알림을 한 점원이 선점(claim)하면 다른 점원에게 "이미 처리 중"으로 보이게 해
-- 동일 요청 중복 발주/작업지시를 방지한다. 오래된(stale) 클레임은 다른 점원이 takeover 가능(서비스의 TTL 판단).
--   claimed_by      : 처리 중인 사용자 userId (NULL = 미점유)
--   claimed_by_name : 표시용 담당자명 스냅샷
--   claimed_at      : 클레임 시각(최신순/만료 판단)

ALTER TABLE po_request_notification
    ADD COLUMN claimed_by      BIGINT,
    ADD COLUMN claimed_by_name VARCHAR(100),
    ADD COLUMN claimed_at      TIMESTAMP;

ALTER TABLE work_order_request_notification
    ADD COLUMN claimed_by      BIGINT,
    ADD COLUMN claimed_by_name VARCHAR(100),
    ADD COLUMN claimed_at      TIMESTAMP;

COMMENT ON COLUMN po_request_notification.claimed_by         IS '처리 중인 담당자 userId(NULL=미점유)';
COMMENT ON COLUMN po_request_notification.claimed_by_name    IS '처리 중인 담당자 표시명 스냅샷';
COMMENT ON COLUMN po_request_notification.claimed_at         IS '클레임 시각(만료/최신 판단)';
COMMENT ON COLUMN work_order_request_notification.claimed_by      IS '처리 중인 담당자 userId(NULL=미점유)';
COMMENT ON COLUMN work_order_request_notification.claimed_by_name IS '처리 중인 담당자 표시명 스냅샷';
COMMENT ON COLUMN work_order_request_notification.claimed_at      IS '클레임 시각(만료/최신 판단)';
