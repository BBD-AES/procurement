package com.bbd.procurement.purchaseorder.adapter.out.persistence;

/**
 * {@code group by status} 집계 결과를 담는 Spring Data 인터페이스 프로젝션.
 * 상태 enum 타입은 호출하는 리포지토리마다 다르므로 제네릭으로 둔다.
 */
public interface StatusCount<S extends Enum<S>> {
    S getStatus();

    long getCount();
}
