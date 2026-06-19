-- 숫자로 변환 안 되는 기존 값 먼저 정리 (NOT NULL 컬럼은 0, nullable 컬럼은 NULL)
UPDATE purchase_order         SET created_by   = '0'  WHERE created_by   !~ '^\d+$';
UPDATE purchase_order         SET received_by  = NULL WHERE received_by  !~ '^\d+$';
UPDATE purchase_order_history SET changed_by   = '0'  WHERE changed_by   !~ '^\d+$';
UPDATE work_order             SET created_by   = '0'  WHERE created_by   !~ '^\d+$';
UPDATE work_order             SET completed_by = NULL WHERE completed_by !~ '^\d+$';

ALTER TABLE purchase_order         ALTER COLUMN created_by   TYPE BIGINT USING created_by::bigint;
ALTER TABLE purchase_order         ALTER COLUMN received_by  TYPE BIGINT USING received_by::bigint;
ALTER TABLE purchase_order_history ALTER COLUMN changed_by   TYPE BIGINT USING changed_by::bigint;
ALTER TABLE work_order             ALTER COLUMN created_by   TYPE BIGINT USING created_by::bigint;
ALTER TABLE work_order             ALTER COLUMN completed_by TYPE BIGINT USING completed_by::bigint;