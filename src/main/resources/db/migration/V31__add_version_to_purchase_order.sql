-- 낙관적 락(@Version)용 버전 컬럼.
-- 동시 입고완료(complete) 등 상태전이 충돌 시 둘째 트랜잭션을 롤백시켜
-- StockInRequested 이벤트가 중복 발행(=재고 2배 증가)되는 것을 막는다.
-- 기존 행은 0으로 초기화한다.
ALTER TABLE purchase_order ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
