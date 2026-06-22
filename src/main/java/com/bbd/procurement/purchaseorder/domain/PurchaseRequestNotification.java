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
     * 같은 sku 라인에 qty 만큼 충당(FIFO는 라인 추가 순서). 실제 소진한 수량 반환.
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
        boolean anyProgress = lines.stream().anyMatch(l -> l.getFulfilledQty() > 0);
        if (allDone && !lines.isEmpty()) {
            this.status = PurchaseRequestStatus.DONE;
        } else if (anyProgress) {
            this.status = PurchaseRequestStatus.PARTIAL;
        } else {
            this.status = PurchaseRequestStatus.PENDING;
        }
    }
}
