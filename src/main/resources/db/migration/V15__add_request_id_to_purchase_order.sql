-- PO 생성 멱등성(중복 방지)을 위한 클라이언트 요청 식별자
-- 프론트가 "발주 생성" 클릭당 1개의 UUID(request_id)를 생성/전송하며, 재시도 시 동일 값을 유지한다.
-- 동일 request_id로 들어온 요청은 기존 PO를 재사용(replay)하고, 동시 경합은 아래 UNIQUE 제약으로 차단한다.
-- Phase 1: 선택(nullable). 레거시/미전송 클라이언트는 NULL로 들어와 기존대로 생성된다.
ALTER TABLE purchase_order ADD COLUMN request_id VARCHAR(64);

-- NULL은 UNIQUE 제약에서 중복으로 보지 않으므로(Postgres), 미전송 요청은 영향받지 않는다.
ALTER TABLE purchase_order ADD CONSTRAINT uq_purchase_order_request UNIQUE (request_id);

COMMENT ON COLUMN purchase_order.request_id IS 'PO 생성 멱등키 (클라이언트가 클릭당 생성하는 UUID, 중복 생성 방지용)';
