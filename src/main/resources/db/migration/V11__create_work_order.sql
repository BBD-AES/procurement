-- 이슈 #60: 작업지시(WorkOrder) — MAKE(생산) 처리
-- PO와 동형. 완료(COMPLETED) 시 procurement.stock-in-requested 발행으로 재고 증가 + 백오더 충족

-- 작업지시 번호 채번 시퀀스 (WO-YYYY-NNNNNN의 NNNNNN, 연도 리셋 없음)
CREATE SEQUENCE work_order_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE;

-- 작업지시 헤더 테이블
CREATE TABLE work_order (
                            id                 BIGSERIAL      PRIMARY KEY,
                            work_order_number  VARCHAR(20)    NOT NULL,
                            so_number          VARCHAR(30)    NOT NULL,
                            warehouse_code     VARCHAR(20)    NOT NULL,
                            status             VARCHAR(20)    NOT NULL,
                            total_amount       NUMERIC(15, 2) NOT NULL DEFAULT 0,
                            created_by         VARCHAR(20)    NOT NULL,
                            completed_by       VARCHAR(20),
                            created_at         TIMESTAMP      NOT NULL,
                            updated_at         TIMESTAMP      NOT NULL,
                            completed_at       TIMESTAMP,
                            CONSTRAINT uk_work_order_number UNIQUE (work_order_number)
);

CREATE INDEX idx_work_order_so_number ON work_order (so_number);
CREATE INDEX idx_work_order_status    ON work_order (status);

-- 작업지시 상세 테이블
CREATE TABLE work_order_line (
                                 id            BIGSERIAL      PRIMARY KEY,
                                 work_order_id BIGINT         NOT NULL,
                                 line_order    INTEGER        NOT NULL,
                                 sku           VARCHAR(50)    NOT NULL,
                                 part_name     VARCHAR(200)   NOT NULL,
                                 unit_price    NUMERIC(15, 2) NOT NULL,
                                 quantity      INTEGER        NOT NULL,
                                 subtotal      NUMERIC(15, 2) NOT NULL,
                                 CONSTRAINT fk_work_order_line_wo FOREIGN KEY (work_order_id) REFERENCES work_order(id) ON DELETE CASCADE
);

CREATE INDEX idx_work_order_line_wo_id ON work_order_line (work_order_id);

-- 코멘트 등록
COMMENT ON TABLE  work_order                   IS '작업지시(WorkOrder) 헤더 — MAKE(생산) 처리, vendor 없음';
COMMENT ON COLUMN work_order.work_order_number  IS '작업지시 번호 (WO-YYYY-NNNNNN, work_order_number_seq 기반)';
COMMENT ON COLUMN work_order.so_number          IS '원 수주번호 (StockInRequested.soNumber로 전달 → 백오더 충족)';
COMMENT ON COLUMN work_order.status             IS 'PLANNED / IN_PRODUCTION / COMPLETED / CANCELED';
COMMENT ON COLUMN work_order_line.unit_price    IS '생성 시 item 기준단가 스냅샷 (movement 이력용)';