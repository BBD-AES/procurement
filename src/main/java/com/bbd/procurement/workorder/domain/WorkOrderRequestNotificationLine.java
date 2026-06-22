package com.bbd.procurement.workorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "work_order_request_notification_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkOrderRequestNotificationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false, updatable = false)
    private WorkOrderRequestNotification notification;

    @Column(name = "sku", nullable = false, length = 100, updatable = false)
    private String sku;

    @Column(name = "requested_qty", nullable = false, updatable = false)
    private int requestedQty;

    @Column(name = "fulfilled_qty", nullable = false)
    private int fulfilledQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderRequestStatus status;

    private WorkOrderRequestNotificationLine(String sku, int requestedQty) {
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.fulfilledQty = 0;
        this.status = WorkOrderRequestStatus.PENDING;
    }

    public static WorkOrderRequestNotificationLine create(String sku, int requestedQty) {
        return new WorkOrderRequestNotificationLine(sku, requestedQty);
    }

    void assignTo(WorkOrderRequestNotification notification) {
        this.notification = notification;
    }

    public int remaining() {
        return Math.max(0, requestedQty - fulfilledQty);
    }

    int applyFulfillment(int qty) {
        if (qty <= 0) {
            return 0;
        }
        int consumed = Math.min(qty, remaining());
        if (consumed <= 0) {
            return 0;
        }
        this.fulfilledQty += consumed;
        this.status = (fulfilledQty >= requestedQty)
                ? WorkOrderRequestStatus.DONE
                : WorkOrderRequestStatus.PARTIAL;
        return consumed;
    }
}
