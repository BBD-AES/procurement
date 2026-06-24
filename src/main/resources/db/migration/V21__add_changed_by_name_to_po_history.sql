-- 변경 이력 변경자 이름 스냅샷 (이슈: 변경자 컬럼이 #userId로 표시되던 문제)
-- 이력 기록 시점의 변경자 displayName을 함께 보존해, 조회 시 사번(userId) 대신 이름을 표시한다.
-- 기존 행은 NULL → 프론트는 changedByName이 없으면 changedBy(#id)로 폴백한다.
ALTER TABLE purchase_order_history ADD COLUMN changed_by_name VARCHAR(100);
