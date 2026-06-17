-- purchase_order_history.after_payload: PO 전체 스냅샷 JSON을 담으므로 TEXT여야 함
-- (엔티티 length=20 + ddl-auto로 varchar(20)이 되어 버린 경우 교정)
ALTER TABLE purchase_order_history ALTER COLUMN after_payload TYPE text;
