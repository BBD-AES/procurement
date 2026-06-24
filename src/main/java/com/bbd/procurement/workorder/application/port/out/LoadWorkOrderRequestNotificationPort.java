package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;

import java.util.List;

public interface LoadWorkOrderRequestNotificationPort {

    /** 아직 작업지시(WO)로 완전히 충당되지 않은(PENDING/PARTIAL) 대기 알림만 receivedAt 내림차순으로 조회. */
    List<WorkOrderRequestNotification> findActiveOrderByReceivedAtDesc();

    /** 같은 soNumber 의 아직 충당 여지가 있는(PENDING/PARTIAL) 알림을 receivedAt 오름차순(FIFO)으로, 쓰기 락과 함께 조회. */
    List<WorkOrderRequestNotification> findActiveBySoNumber(String soNumber);

    /** 아직 작업지시(WO)로 완전히 충당되지 않은(PENDING/PARTIAL) 대기 생산요청 알림 수. */
    long countPending();
}
