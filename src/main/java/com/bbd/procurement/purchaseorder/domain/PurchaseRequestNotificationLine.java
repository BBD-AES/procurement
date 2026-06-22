package com.bbd.procurement.purchaseorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "po_request_notification_line")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestNotificationLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false, updatable = false)
    private PurchaseRequestNotification notification;

    @Column(name = "sku", nullable = false, length = 100, updatable = false)
    private String sku;

    @Column(name = "requested_qty", nullable = false, updatable = false)
    private int requestedQty;

    @Column(name = "fulfilled_qty", nullable = false)
    private int fulfilledQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseRequestStatus status;

    private PurchaseRequestNotificationLine(String sku, int requestedQty) {
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.fulfilledQty = 0;
        this.status = PurchaseRequestStatus.PENDING;
    }

    public static PurchaseRequestNotificationLine create(String sku, int requestedQty) {
        return new PurchaseRequestNotificationLine(sku, requestedQty);
    }

    void assignTo(PurchaseRequestNotification notification) {
        this.notification = notification;
    }

    public int remaining() {
        return Math.max(0, requestedQty - fulfilledQty);
    }

    /** 이 라인에 qty 만큼 충당 시도. 실제 소진한 수량을 반환. */
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
                ? PurchaseRequestStatus.DONE
                : PurchaseRequestStatus.PARTIAL;
        return consumed;
    }
}
