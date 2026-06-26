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

    /** 클레임(처리중 선점)용 — eventId로 라인까지 fetch + 쓰기 락(동시 클레임 직렬화). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct n from PurchaseRequestNotification n left join fetch n.lines " +
            "where n.eventId = :eventId")
    Optional<PurchaseRequestNotification> findByEventIdForUpdate(@Param("eventId") String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from PurchaseRequestNotification n " +
            "where n.soNumber = :soNumber and n.status in :statuses order by n.receivedAt asc")
    List<PurchaseRequestNotification> findActiveBySoNumberForUpdate(@Param("soNumber") String soNumber,
                                                                    @Param("statuses") Collection<PurchaseRequestStatus> statuses);

    long countByStatusIn(Collection<PurchaseRequestStatus> statuses);
}
