-- 이슈 #25: Outbox 발행 인프라 확장
-- outbox_event에 topic 컬럼 추가 (Kafka 발행 시 토픽 식별 — MessageRelay가 행만 보고 발행)
-- (confirmed_by/confirmed_at 제거는 V5에서 이미 수행됨)

-- outbox_event — topic 컬럼 추가 (기존 row 호환 위해 nullable)
ALTER TABLE outbox_event
    ADD COLUMN topic VARCHAR(100);

COMMENT ON COLUMN outbox_event.topic IS 'Kafka 토픽명 (예: procurement.stock-in-requested). 후속 Kafka 이슈에서 MessageRelay가 KafkaTemplate.send(topic, key, payload)에 사용. 기존 Vendor 이벤트 등 외부 발행 의도 없는 row는 NULL';