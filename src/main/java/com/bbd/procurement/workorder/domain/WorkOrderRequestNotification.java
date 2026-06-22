package com.bbd.procurement.workorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "work_order_request_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkOrderRequestNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36, updatable = false)
    private String eventId;

    @Column(name = "so_number", nullable = false, length = 30, updatable = false)
    private String soNumber;

    @Column(name = "warehouse_code", nullable = false, length = 20, updatable = false)
    private String warehouseCode;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderRequestStatus status;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderRequestNotificationLine> lines = new ArrayList<>();

    private WorkOrderRequestNotification(String eventId, String soNumber, String warehouseCode,
                                         String payload, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.soNumber = soNumber;
        this.warehouseCode = warehouseCode;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.status = WorkOrderRequestStatus.PENDING;
    }

    public static WorkOrderRequestNotification create(String eventId, String soNumber, String warehouseCode,
                                                      String payload, LocalDateTime receivedAt,
                                                      List<WorkOrderRequestNotificationLine> lines) {
        WorkOrderRequestNotification notification =
                new WorkOrderRequestNotification(eventId, soNumber, warehouseCode, payload, receivedAt);
        if (lines != null) {
            lines.forEach(notification::attachLine);
        }
        return notification;
    }

    private void attachLine(WorkOrderRequestNotificationLine line) {
        line.assignTo(this);
        this.lines.add(line);
    }

    public int applyFulfillment(String sku, int qty) {
        int remaining = qty;
        for (WorkOrderRequestNotificationLine line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (line.getSku().equals(sku)) {
                remaining -= line.applyFulfillment(remaining);
            }
        }
        recomputeStatus();
        return qty - remaining;
    }

    public void recomputeStatus() {
        boolean allDone = lines.stream().allMatch(l -> l.getStatus() == WorkOrderRequestStatus.DONE);
        boolean anyProgress = lines.stream().anyMatch(l -> l.getFulfilledQty() > 0);
        if (allDone && !lines.isEmpty()) {
            this.status = WorkOrderRequestStatus.DONE;
        } else if (anyProgress) {
            this.status = WorkOrderRequestStatus.PARTIAL;
        } else {
            this.status = WorkOrderRequestStatus.PENDING;
        }
    }
}
