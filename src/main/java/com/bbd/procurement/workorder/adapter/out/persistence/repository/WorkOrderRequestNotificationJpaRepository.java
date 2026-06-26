package com.bbd.procurement.workorder.adapter.out.persistence.repository;

import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import com.bbd.procurement.workorder.domain.WorkOrderRequestStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface WorkOrderRequestNotificationJpaRepository extends JpaRepository<WorkOrderRequestNotification, Long> {

    @Query("select distinct n from WorkOrderRequestNotification n left join fetch n.lines " +
            "where n.status in :statuses order by n.receivedAt desc")
    List<WorkOrderRequestNotification> findByStatusInWithLinesOrderByReceivedAtDesc(
            @Param("statuses") Collection<WorkOrderRequestStatus> statuses);

    /** 클레임(처리중 선점)용 — eventId로 라인까지 fetch + 쓰기 락(동시 클레임 직렬화). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct n from WorkOrderRequestNotification n left join fetch n.lines " +
            "where n.eventId = :eventId")
    Optional<WorkOrderRequestNotification> findByEventIdForUpdate(@Param("eventId") String eventId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from WorkOrderRequestNotification n " +
            "where n.soNumber = :soNumber and n.status in :statuses order by n.receivedAt asc")
    List<WorkOrderRequestNotification> findActiveBySoNumberForUpdate(@Param("soNumber") String soNumber,
                                                                     @Param("statuses") Collection<WorkOrderRequestStatus> statuses);

    long countByStatusIn(Collection<WorkOrderRequestStatus> statuses);
}
