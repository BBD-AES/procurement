package com.bbd.procurement.workorder.application.port.out;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;

import java.util.List;

public interface LoadWorkOrderRequestNotificationPort {

    List<WorkOrderRequestNotification> findAllOrderByReceivedAtDesc();

    /** 같은 soNumber 의 아직 충당 여지가 있는(PENDING/PARTIAL) 알림을 receivedAt 오름차순(FIFO)으로, 쓰기 락과 함께 조회. */
    List<WorkOrderRequestNotification> findActiveBySoNumber(String soNumber);
}
