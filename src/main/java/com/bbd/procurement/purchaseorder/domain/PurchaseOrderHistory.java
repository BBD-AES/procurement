package com.bbd.procurement.purchaseorder.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "purchase_order_history")
@Getter
@NoArgsConstructor
public class PurchaseOrderHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Getter
    private Long id;

    @Column(name = "po_number", nullable = false, length = 20, updatable = false)
    private String poNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30, updatable = false)
    private PurchaseOrderChangeType changeType;

    @Column(name = "before_payload", columnDefinition = "TEXT", updatable = false)
    private String beforePayload;

    @Column(name = "after_payload", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String afterPayload;

    @Column(name = "changed_by", nullable = false, length = 20, updatable = false)
    private Long changedBy;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    private PurchaseOrderHistory(String poNumber,
                                 PurchaseOrderChangeType changeType,
                                 String beforePayload,
                                 String afterPayload,
                                 Long changedBy) {
        this.poNumber = poNumber;
        this.changeType = changeType;
        this.beforePayload = beforePayload;
        this.afterPayload = afterPayload;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public static PurchaseOrderHistory create(String poNumber,
                                              PurchaseOrderChangeType changeType,
                                              String beforePayload,
                                              String afterPayload,
                                              Long changedBy) {
        return new PurchaseOrderHistory(poNumber, changeType, beforePayload, afterPayload, changedBy);
    }


}
