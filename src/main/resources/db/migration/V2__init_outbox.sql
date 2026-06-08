-- 트랜잭셔널 Outbox 테이블 (도메인 이벤트 발행 대기 큐)
CREATE TABLE outbox_event (
                              id              BIGSERIAL    PRIMARY KEY,
                              event_id        UUID         NOT NULL,
                              aggregate_type  VARCHAR(50)  NOT NULL,
                              aggregate_id    VARCHAR(100) NOT NULL,
                              event_type      VARCHAR(100) NOT NULL,
                              payload         TEXT         NOT NULL,
                              occurred_at     TIMESTAMP    NOT NULL,
                              processed_at    TIMESTAMP,
                              created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT uk_outbox_event_id UNIQUE (event_id)
);

-- Relay 스케줄러의 미처리 row 빠른 탐색
CREATE INDEX idx_outbox_processed_at ON outbox_event (processed_at);

-- 특정 Aggregate 이벤트 추적 (디버깅·운영용)
CREATE INDEX idx_outbox_aggregate ON outbox_event (aggregate_type, aggregate_id);

COMMENT ON TABLE  outbox_event                IS '트랜잭셔널 Outbox - 도메인
  이벤트 발행 대기 큐';
  COMMENT ON COLUMN outbox_event.event_id       IS '글로벌 유니크 이벤트 식별자
  (consumer 멱등성 키)';
  COMMENT ON COLUMN outbox_event.aggregate_type IS '도메인 Aggregate 타입 (Vendor,
  PurchaseOrder 등)';
  COMMENT ON COLUMN outbox_event.aggregate_id   IS '도메인 Aggregate 식별자
  (V000001 등)';
  COMMENT ON COLUMN outbox_event.event_type     IS '이벤트 타입 (VendorCreated
  등)';
  COMMENT ON COLUMN outbox_event.payload        IS '이벤트 JSON 직렬화 데이터';
  COMMENT ON COLUMN outbox_event.occurred_at    IS '도메인 이벤트 발생 시각';
  COMMENT ON COLUMN outbox_event.processed_at   IS 'Relay 발행 완료 시각 (NULL이면
  미처리)';
  COMMENT ON COLUMN outbox_event.created_at     IS 'row INSERT 시각';