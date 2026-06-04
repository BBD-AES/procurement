-- 공급사 코드 채번용 시퀀스 (Vendor.create 시 nextval 호출 → V%06d 포맷)
CREATE SEQUENCE vendor_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MAXVALUE
    NO CYCLE;

-- 공급사 테이블
CREATE TABLE vendor (
                        id          BIGSERIAL    PRIMARY KEY,
                        code        VARCHAR(10)  NOT NULL,
                        name        VARCHAR(100) NOT NULL,
                        contact     VARCHAR(100),
                        terms       TEXT,
                        active      BOOLEAN      NOT NULL DEFAULT TRUE,
                        created_at  TIMESTAMP    NOT NULL,
                        updated_at  TIMESTAMP    NOT NULL,
                        CONSTRAINT uk_vendor_code UNIQUE (code)
);

-- 활성 공급사 빠른 조회 (목록 API 필터)
CREATE INDEX idx_vendor_active ON vendor (active);

-- 이름 검색용 (목록 API 검색 조건 들어올 가능성)
CREATE INDEX idx_vendor_name ON vendor (name);

COMMENT ON TABLE  vendor              IS '공급사 마스터';
  COMMENT ON COLUMN vendor.code         IS '공급사 코드 (V000001 형식, vendor_code_seq 기반)';
  COMMENT ON COLUMN vendor.name         IS '공급사명';
  COMMENT ON COLUMN vendor.contact      IS '연락처';
  COMMENT ON COLUMN vendor.terms        IS '거래 조건 (지불 기한 등)';
  COMMENT ON COLUMN vendor.active       IS '활성 여부 (false면 PO 작성 시 사용 불가)';

