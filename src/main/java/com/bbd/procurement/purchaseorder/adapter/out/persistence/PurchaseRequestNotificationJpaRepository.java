package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PurchaseRequestNotificationJpaRepository extends JpaRepository<PurchaseRequestNotification, Long> {

    List<PurchaseRequestNotification> findAllByOrderByReceivedAtDesc();

}
