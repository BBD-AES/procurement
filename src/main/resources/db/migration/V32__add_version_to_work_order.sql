-- 낙관적 락(@Version)용 버전 컬럼.
-- 동시 상태전이(start/complete 더블클릭 등) 충돌 시 둘째 트랜잭션을 롤백시킨다.
-- PurchaseOrder의 V31과 동일한 목적. 기존 행은 0으로 초기화한다.
ALTER TABLE work_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
