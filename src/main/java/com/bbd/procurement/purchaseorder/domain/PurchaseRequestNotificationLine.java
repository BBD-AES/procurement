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

    // 발주(PO ORDERED)됐으나 아직 입고(RECEIVED) 안 된 누적 수량 = "발주중".
    @Column(name = "ordered_qty", nullable = false)
    private int orderedQty;

    @Column(name = "fulfilled_qty", nullable = false)
    private int fulfilledQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseRequestStatus status;

    private PurchaseRequestNotificationLine(String sku, int requestedQty) {
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.orderedQty = 0;
        this.fulfilledQty = 0;
        this.status = PurchaseRequestStatus.PENDING;
    }

    public static PurchaseRequestNotificationLine create(String sku, int requestedQty) {
        return new PurchaseRequestNotificationLine(sku, requestedQty);
    }

    void assignTo(PurchaseRequestNotification notification) {
        this.notification = notification;
    }

    /** 아직 입고되지 않은 수량(발주중 + 미발주). 입고완료(applyFulfillment) 상한으로 쓴다. */
    public int remaining() {
        return Math.max(0, requestedQty - fulfilledQty);
    }

    /** 아직 발주조차 하지 않은 수량(= 요청 - 발주중 - 입고완료). 발주(applyOrder) 상한이자 화면의 "발주해야 할 잔여". */
    public int orderableRemaining() {
        return Math.max(0, requestedQty - orderedQty - fulfilledQty);
    }

    /** 이 라인에 qty 만큼 발주(ORDERED) 반영. 실제 소진한 수량을 반환. */
    int applyOrder(int qty) {
        if (qty <= 0) {
            return 0;
        }
        int consumed = Math.min(qty, orderableRemaining());
        if (consumed <= 0) {
            return 0;
        }
        this.orderedQty += consumed;
        recomputeStatus();
        return consumed;
    }

    /** 이 라인의 발주중 수량을 qty 만큼 해제(발주 취소). 실제 해제한 수량을 반환. */
    int releaseOrder(int qty) {
        if (qty <= 0) {
            return 0;
        }
        int released = Math.min(qty, orderedQty);
        if (released <= 0) {
            return 0;
        }
        this.orderedQty -= released;
        recomputeStatus();
        return released;
    }

    /** 이 라인에 qty 만큼 입고완료(RECEIVED) 충당. 발주중 수량을 입고완료로 이동시킨다. 실제 소진한 수량을 반환. */
    int applyFulfillment(int qty) {
        if (qty <= 0) {
            return 0;
        }
        int consumed = Math.min(qty, remaining());
        if (consumed <= 0) {
            return 0;
        }
        this.fulfilledQty += consumed;
        this.orderedQty = Math.max(0, this.orderedQty - consumed); // 발주중 → 입고완료 이동
        recomputeStatus();
        return consumed;
    }

    private void recomputeStatus() {
        if (fulfilledQty >= requestedQty) {
            this.status = PurchaseRequestStatus.DONE;
        } else if (fulfilledQty > 0 || orderedQty > 0) {
            this.status = PurchaseRequestStatus.PARTIAL;
        } else {
            this.status = PurchaseRequestStatus.PENDING;
        }
    }
}
