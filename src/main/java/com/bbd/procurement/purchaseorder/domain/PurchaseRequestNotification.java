package com.bbd.procurement.purchaseorder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "po_request_notification")
@Getter
@NoArgsConstructor
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

    private PurchaseRequestNotification(String eventId, String soNumber, String warehouseCode, String payload, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.soNumber = soNumber;
        this.warehouseCode = warehouseCode;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.status = PurchaseRequestStatus.PENDING;
    }

    public static PurchaseRequestNotification create(String eventId, String soNumber, String warehouseCode, String payload, LocalDateTime receivedAt) {
        return new PurchaseRequestNotification(eventId, soNumber, warehouseCode, payload, receivedAt);
    }

    public void markDone() {
        this.status = PurchaseRequestStatus.DONE;
    }
}
