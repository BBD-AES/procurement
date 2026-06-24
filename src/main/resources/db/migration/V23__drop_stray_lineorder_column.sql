-- WorkOrderLine 엔티티가 컬럼을 @Column(name="lineOrder")로 잘못 매핑해(올바른 값은 line_order)
-- ddl-auto:update 환경에서 Hibernate가 만든 잉여 컬럼 "lineorder"를 정리한다.
-- 엔티티 매핑은 line_order로 교정됨. 정상 마이그레이션만 거친 환경에는 이 컬럼이 없으므로 IF EXISTS로 무해 no-op.
ALTER TABLE work_order_line DROP COLUMN IF EXISTS lineorder;
