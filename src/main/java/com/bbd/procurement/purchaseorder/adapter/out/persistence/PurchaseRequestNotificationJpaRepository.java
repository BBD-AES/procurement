package com.bbd.procurement.purchaseorder.adapter.out.persistence;

import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PurchaseRequestNotificationJpaRepository extends JpaRepository<PurchaseRequestNotification, Long> {

    @Query("select distinct n from PurchaseRequestNotification n left join fetch n.lines " +
            "where n.status in :statuses order by n.receivedAt desc")
    List<PurchaseRequestNotification> findByStatusInWithLinesOrderByReceivedAtDesc(
            @Param("statuses") Collection<PurchaseRequestStatus> statuses);

    /** 클레임(처리중 선점)용 — eventId로 라인까지 fetch. 동시 선점 충돌은 @Version 낙관적 락으로 감지(락 없음). */
    @Query("select distinct n from PurchaseRequestNotification n left join fetch n.lines " +
            "where n.eventId = :eventId")
    Optional<PurchaseRequestNotification> findByEventId(@Param("eventId") String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from PurchaseRequestNotification n " +
            "where n.soNumber = :soNumber and n.status in :statuses order by n.receivedAt asc")
    List<PurchaseRequestNotification> findActiveBySoNumberForUpdate(@Param("soNumber") String soNumber,
                                                                    @Param("statuses") Collection<PurchaseRequestStatus> statuses);

    long countByStatusIn(Collection<PurchaseRequestStatus> statuses);
}
