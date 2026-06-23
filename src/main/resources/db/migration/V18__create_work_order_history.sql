-- 이슈 #94: WO 변경이력 테이블 (PO 변경이력 #36 패턴 대칭)
-- WorkOrder 취소 등 상태 변경을 변경 전/후 스냅샷과 함께 기록

CREATE TABLE work_order_history (
                                    id                 BIGSERIAL PRIMARY KEY,
                                    work_order_number  VARCHAR(20)  NOT NULL,
                                    change_type        VARCHAR(30)  NOT NULL,
                                    before_payload     TEXT,
                                    after_payload      TEXT         NOT NULL,
                                    changed_by         BIGINT       NOT NULL,
                                    changed_at         TIMESTAMP    NOT NULL
);

CREATE INDEX idx_wo_history_work_order_number ON work_order_history (work_order_number);

COMMENT ON COLUMN work_order_history.change_type     IS 'CANCELED (이후 생명주기 이벤트 확장 가능)';
COMMENT ON COLUMN work_order_history.before_payload  IS '변경 전 WO 전체 스냅샷 JSON';
COMMENT ON COLUMN work_order_history.after_payload   IS '변경 후 WO 전체 스냅샷 JSON';
