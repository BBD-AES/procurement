-- 공급사 등록 멱등성(중복 방지)을 위한 클라이언트 요청 식별자 (PO #79 / WO 패턴 대칭 적용)
-- 프론트가 "공급사 등록" 클릭당 1개의 UUID(request_id)를 생성/전송하며, 재시도 시 동일 값을 유지한다.
-- (헤더 Idempotency-Key가 org 표준, 본문 request_id는 레거시 폴백 — 둘 중 하나가 이 컬럼에 저장된다.)
-- 동일 request_id로 들어온 요청은 기존 공급사를 재사용(replay)하고, 동시 경합은 아래 UNIQUE 제약으로 차단한다.
-- Phase 1: 선택(nullable). 레거시/미전송 클라이언트는 NULL로 들어와 기존대로 생성된다.
ALTER TABLE vendor ADD COLUMN request_id VARCHAR(64);

-- NULL은 UNIQUE 제약에서 중복으로 보지 않으므로(Postgres), 미전송 요청은 영향받지 않는다.
ALTER TABLE vendor ADD CONSTRAINT uq_vendor_request UNIQUE (request_id);

COMMENT ON COLUMN vendor.request_id IS '공급사 등록 멱등키 (클라이언트가 클릭당 생성하는 UUID, 중복 생성 방지용)';
