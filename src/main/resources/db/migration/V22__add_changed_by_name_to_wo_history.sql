-- 작업지시(WO) 변경 이력 변경자 이름 스냅샷 (PO V21과 대칭).
-- 이력 기록 시점의 변경자 displayName을 함께 보존해, 조회 시 사번(userId) 대신 이름을 표시한다.
-- 기존 행은 NULL → 조회측은 changedByName이 없으면 changedBy(#id)로 폴백한다.
ALTER TABLE work_order_history ADD COLUMN changed_by_name VARCHAR(100);
