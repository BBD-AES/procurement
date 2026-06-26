package com.bbd.procurement.purchaseorder.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "po_request_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseRequestNotification {

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
    private PurchaseRequestStatus status;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PurchaseRequestNotificationLine> lines = new ArrayList<>();

    private PurchaseRequestNotification(String eventId, String soNumber, String warehouseCode, String payload, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.soNumber = soNumber;
        this.warehouseCode = warehouseCode;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.status = PurchaseRequestStatus.PENDING;
    }

    public static PurchaseRequestNotification create(String eventId, String soNumber, String warehouseCode,
                                                     String payload, LocalDateTime receivedAt,
                                                     List<PurchaseRequestNotificationLine> lines) {
        PurchaseRequestNotification notification =
                new PurchaseRequestNotification(eventId, soNumber, warehouseCode, payload, receivedAt);
        if (lines != null) {
            lines.forEach(notification::attachLine);
        }
        return notification;
    }

    private void attachLine(PurchaseRequestNotificationLine line) {
        line.assignTo(this);
        this.lines.add(line);
    }

    /**
     * 같은 sku 라인에 qty 만큼 발주(ORDERED) 반영(FIFO는 라인 추가 순서). 실제 소진한 수량 반환.
     */
    public int applyOrder(String sku, int qty) {
        int remaining = qty;
        for (PurchaseRequestNotificationLine line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (line.getSku().equals(sku)) {
                remaining -= line.applyOrder(remaining);
            }
        }
        recomputeStatus();
        return qty - remaining;
    }

    /**
     * 같은 sku 라인의 발주중 수량을 qty 만큼 해제(발주 취소, FIFO). 실제 해제한 수량 반환.
     */
    public int releaseOrder(String sku, int qty) {
        int remaining = qty;
        for (PurchaseRequestNotificationLine line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (line.getSku().equals(sku)) {
                remaining -= line.releaseOrder(remaining);
            }
        }
        recomputeStatus();
        return qty - remaining;
    }

    /**
     * 같은 sku 라인에 qty 만큼 입고완료 충당(FIFO는 라인 추가 순서). 실제 소진한 수량 반환.
     * 충당 후 헤더 status 재계산.
     */
    public int applyFulfillment(String sku, int qty) {
        int remaining = qty;
        for (PurchaseRequestNotificationLine line : lines) {
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
        boolean allDone = lines.stream().allMatch(l -> l.getStatus() == PurchaseRequestStatus.DONE);
        // 발주중(ordered)·입고완료(fulfilled) 어느 쪽이든 진행이 있으면 PARTIAL — 전량 발주됐지만 입고 전인 주문도
        // 목록(PENDING/PARTIAL)에 남아 "이미 발주함, 입고 대기 중"으로 보이게 한다(중복 발주 방지).
        boolean anyProgress = lines.stream().anyMatch(l -> l.getFulfilledQty() > 0 || l.getOrderedQty() > 0);
        if (allDone && !lines.isEmpty()) {
            this.status = PurchaseRequestStatus.DONE;
        } else if (anyProgress) {
            this.status = PurchaseRequestStatus.PARTIAL;
        } else {
            this.status = PurchaseRequestStatus.PENDING;
        }
    }
}
