package com.bbd.procurement.workorder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "work_order_history")
@Getter
@NoArgsConstructor
public class WorkOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "work_order_number", nullable = false, length = 20, updatable = false)
    private String workOrderNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30, updatable = false)
    private WorkOrderChangeType changeType;

    @Column(name = "before_payload", columnDefinition = "TEXT", updatable = false)
    private String beforePayload;

    @Column(name = "after_payload", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String afterPayload;

    @Column(name = "changed_by", nullable = false, updatable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    private WorkOrderHistory(String workOrderNumber,
                            WorkOrderChangeType changeType,
                            String beforePayload,
                            String afterPayload,
                            Long changedBy) {
        this.workOrderNumber = workOrderNumber;
        this.changeType = changeType;
        this.beforePayload = beforePayload;
        this.afterPayload = afterPayload;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public static WorkOrderHistory create(String workOrderNumber,
                                          WorkOrderChangeType changeType,
                                          String beforePayload,
                                          String afterPayload,
                                          Long changedBy) {
        return new WorkOrderHistory(workOrderNumber, changeType, beforePayload, afterPayload, changedBy);
    }
}
