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

public interface WorkOrderRequestNotificationJpaRepository extends JpaRepository<WorkOrderRequestNotification, Long> {

    @Query("select distinct n from WorkOrderRequestNotification n left join fetch n.lines order by n.receivedAt desc")
    List<WorkOrderRequestNotification> findAllWithLinesOrderByReceivedAtDesc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select n from WorkOrderRequestNotification n " +
            "where n.soNumber = :soNumber and n.status in :statuses order by n.receivedAt asc")
    List<WorkOrderRequestNotification> findActiveBySoNumberForUpdate(@Param("soNumber") String soNumber,
                                                                     @Param("statuses") Collection<WorkOrderRequestStatus> statuses);

    long countByStatusIn(Collection<WorkOrderRequestStatus> statuses);
}
