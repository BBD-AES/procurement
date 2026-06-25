-- WO 변경이력 change_type CHECK 제약 잔재 수정
-- 증상: WO 헤더/라인 수정 시 500 C999 (이력 INSERT가 CHECK 위반).
--
-- 원인: 과거 ddl-auto:update 시절 WorkOrderChangeType enum이 'CANCELED' 단일값일 때
--   Hibernate가 work_order_history.change_type 에 CHECK(change_type='CANCELED')를 자동 생성했다.
--   이후 enum이 HEADER_UPDATED / LINES_REPLACED 로 확장됐지만 CHECK 제약은 그대로 남아
--   헤더/라인 수정 이력(HEADER_UPDATED·LINES_REPLACED) INSERT가 제약 위반으로 실패했다.
--   (cancel='CANCELED'·create=이력없음 만 통과해 부분 동작처럼 보였다.)
--
-- 조치: 현재 enum 전체 값을 허용하도록 CHECK 제약을 교체한다(PO 이력 CHECK와 대칭).
ALTER TABLE work_order_history DROP CONSTRAINT IF EXISTS work_order_history_change_type_check;
ALTER TABLE work_order_history ADD CONSTRAINT work_order_history_change_type_check
    CHECK (change_type IN ('HEADER_UPDATED', 'LINES_REPLACED', 'CANCELED'));
