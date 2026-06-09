-- 구매 주문서(PO) 번호 채번 시퀀스
-- PO-YYYY-NNNNNN 포맷에서 NNNNNN 부분
-- 연도 리셋 없음 -> 무한 증가
CREATE SEQUENCE po_number_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE;

-- 구매 주문서 헤더
CREATE TABLE purchase_order (
                                id                BIGSERIAL      PRIMARY KEY,
                                po_number         VARCHAR(20)    NOT NULL,
                                vendor_code       VARCHAR(10)    NOT NULL,
                                warehouse_code    VARCHAR(20)    NOT NULL,
                                status            VARCHAR(20)    NOT NULL,
                                total_amount      NUMERIC(15, 2) NOT NULL DEFAULT 0,
                                expected_arrival  DATE,
                                note              TEXT,
                                created_by        VARCHAR(20)    NOT NULL,
                                confirmed_by      VARCHAR(20),
                                received_by       VARCHAR(20),
                                created_at        TIMESTAMP      NOT NULL,
                                updated_at        TIMESTAMP      NOT NULL,
                                confirmed_at      TIMESTAMP,
                                received_at       TIMESTAMP,
                                CONSTRAINT uk_po_number UNIQUE (po_number)
);

CREATE INDEX idx_po_vendor_code     ON purchase_order (vendor_code);
CREATE INDEX idx_po_status          ON purchase_order (status);
CREATE INDEX idx_po_created_at      ON purchase_order (created_at);
CREATE INDEX idx_po_expected_arrival ON purchase_order (expected_arrival);

COMMENT ON TABLE  purchase_order                  IS '구매 주문서(PO) 헤더';
  COMMENT ON COLUMN purchase_order.po_number        IS 'PO 번호 (PO-YYYY-NNNNNN 포맷, po_number_seq 기반)';
  COMMENT ON COLUMN purchase_order.vendor_code      IS '공급사 코드 (FK 아님, 코드 참조)';
  COMMENT ON COLUMN purchase_order.warehouse_code   IS '입고 대상 창고 코드 (예: WH-HQ-001)';
  COMMENT ON COLUMN purchase_order.status           IS '상태: DRAFT / CONFIRMED / RECEIVED / CANCELED';
  COMMENT ON COLUMN purchase_order.total_amount     IS '전체 금액 (라인 subtotal 합계, 도메인이 재계산하여 저장)';
  COMMENT ON COLUMN purchase_order.expected_arrival IS '예상 도착일 (선택)';
  COMMENT ON COLUMN purchase_order.note             IS '비고/메모 (자유 텍스트, 선택)';
  COMMENT ON COLUMN purchase_order.created_by       IS '작성자 사번';
  COMMENT ON COLUMN purchase_order.confirmed_by     IS '확정자 사번 (CONFIRMED 진입 시 박힘)';
  COMMENT ON COLUMN purchase_order.received_by      IS '입고 처리자 사번 (RECEIVED 진입 시 박힘)';
  COMMENT ON COLUMN purchase_order.confirmed_at     IS '확정 시각';
  COMMENT ON COLUMN purchase_order.received_at      IS '입고 시각';

  -- 구매 주문 라인 (주문 항목)
CREATE TABLE purchase_order_line (
                                     id                 BIGSERIAL      PRIMARY KEY,
                                     purchase_order_id  BIGINT         NOT NULL,
                                     line_order         INT            NOT NULL,
                                     sku                VARCHAR(50)    NOT NULL,
                                     part_name          VARCHAR(200)   NOT NULL,
                                     unit_price         NUMERIC(15, 2) NOT NULL,
                                     quantity           INT            NOT NULL,
                                     subtotal           NUMERIC(15, 2) NOT NULL,
                                     CONSTRAINT fk_po_line_po
                                         FOREIGN KEY (purchase_order_id) REFERENCES purchase_order (id) ON DELETE CASCADE
);

CREATE INDEX idx_po_line_po_id ON purchase_order_line (purchase_order_id);

COMMENT ON TABLE  purchase_order_line             IS '구매 주문 라인 (작성 시점 부품명/단가 스냅샷 보존)';
  COMMENT ON COLUMN purchase_order_line.line_order  IS '라인 표시 순서';
  COMMENT ON COLUMN purchase_order_line.part_name   IS '부품명 (스냅샷, 마스터 변경 무관)';
  COMMENT ON COLUMN purchase_order_line.unit_price  IS '단가 (스냅샷)';
  COMMENT ON COLUMN purchase_order_line.subtotal    IS '단가 × 수량';
