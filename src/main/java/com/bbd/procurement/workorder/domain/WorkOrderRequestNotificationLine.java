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

    // 작업지시(WO) 생성됐으나 아직 완료(COMPLETED) 안 된 누적 수량 = "생산중".
    @Column(name = "ordered_qty", nullable = false)
    private int orderedQty;

    @Column(name = "fulfilled_qty", nullable = false)
    private int fulfilledQty;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderRequestStatus status;

    private WorkOrderRequestNotificationLine(String sku, int requestedQty) {
        this.sku = sku;
        this.requestedQty = requestedQty;
        this.orderedQty = 0;
        this.fulfilledQty = 0;
        this.status = WorkOrderRequestStatus.PENDING;
    }

    public static WorkOrderRequestNotificationLine create(String sku, int requestedQty) {
        return new WorkOrderRequestNotificationLine(sku, requestedQty);
    }

    void assignTo(WorkOrderRequestNotification notification) {
        this.notification = notification;
    }

    /** 아직 생산완료되지 않은 수량(생산중 + 미지시). 완료(applyFulfillment) 상한으로 쓴다. */
    public int remaining() {
        return Math.max(0, requestedQty - fulfilledQty);
    }

    /** 아직 작업지시조차 하지 않은 수량(= 요청 - 생산중 - 완료). 작업지시(applyOrder) 상한이자 화면의 "지시해야 할 잔여". */
    public int orderableRemaining() {
        return Math.max(0, requestedQty - orderedQty - fulfilledQty);
    }

    /** 이 라인에 qty 만큼 작업지시(생성) 반영. 실제 소진한 수량을 반환. */
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

    /** 이 라인의 생산중 수량을 qty 만큼 해제(작업지시 취소). 실제 해제한 수량을 반환. */
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

    /** 이 라인에 qty 만큼 생산완료 충당. 생산중 수량을 완료로 이동시킨다. 실제 소진한 수량을 반환. */
    int applyFulfillment(int qty) {
        if (qty <= 0) {
            return 0;
        }
        int consumed = Math.min(qty, remaining());
        if (consumed <= 0) {
            return 0;
        }
        this.fulfilledQty += consumed;
        this.orderedQty = Math.max(0, this.orderedQty - consumed); // 생산중 → 완료 이동
        recomputeStatus();
        return consumed;
    }

    private void recomputeStatus() {
        if (fulfilledQty >= requestedQty) {
            this.status = WorkOrderRequestStatus.DONE;
        } else if (fulfilledQty > 0 || orderedQty > 0) {
            this.status = WorkOrderRequestStatus.PARTIAL;
        } else {
            this.status = WorkOrderRequestStatus.PENDING;
        }
    }
}
