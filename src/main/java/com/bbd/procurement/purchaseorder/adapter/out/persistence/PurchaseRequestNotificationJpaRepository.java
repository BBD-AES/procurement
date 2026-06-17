package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestNotificationJpaRepository extends JpaRepository<PurchaseRequestNotification, Long> {

    List<PurchaseRequestNotification> findAllByOrderByReceivedAtDesc();

    List<PurchaseRequestNotification> findBySoNumberAndStatus(String soNumber, PurchaseRequestStatus status);

}
