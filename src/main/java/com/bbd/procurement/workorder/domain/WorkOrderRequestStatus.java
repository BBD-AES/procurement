package com.bbd.procurement.workorder.domain;

public enum WorkOrderRequestStatus {
    PENDING, // 수신됨, 아직 충당된 수량 없음
    PARTIAL, // 일부 라인/수량만 작업지시 완료(COMPLETED)로 충당됨
    DONE     // 모든 라인이 요청 수량만큼 충당 완료
}
