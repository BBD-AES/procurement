package com.bbd.procurement.purchaseorder.domain;

import com.bbd.procurement.global.entity.BaseTimeEntity;
import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "purchase_order")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PurchaseOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "po_number", nullable = false, unique = true, length = 20, updatable = false)
    private String poNumber;

    @Column(name = "vendor_code", nullable = false, length = 10)
    private String vendorCode;

    @Column(name = "warehouse_code", nullable = false, length = 20)
    private String warehouseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private PurchaseOrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "expected_arrival")
    private LocalDate expectedArrival;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @Column(name = "created_by", nullable = false, length = 20, updatable = false)
    private String createdBy;

    @Column(name = "confirmed_by", length = 20)
    private String confirmedBy;

    @Column(name = "received_by", length = 20)
    private String receivedBy;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<PurchaseOrderLine> lines = new ArrayList<>();

    private PurchaseOrder(String poNumber, String vendorCode,
                          String warehouseCode, LocalDate expectedArrival, String note, String createdBy) {
        this.poNumber = poNumber;
        this.vendorCode = vendorCode;
        this.warehouseCode = warehouseCode;
        this.expectedArrival = expectedArrival;
        this.note = note;
        this.createdBy = createdBy;
        this.status = PurchaseOrderStatus.DRAFT;
        this.totalAmount = BigDecimal.ZERO;
    }

    public static PurchaseOrder create(String poNumber, String vendorCode,
                                       String warehouseCode,LocalDate expectedArrival, String note,
                                       List<PurchaseOrderLine> initialLines, String createdBy) {
        validateRequired(poNumber, vendorCode, warehouseCode, createdBy);
        PurchaseOrder po = new PurchaseOrder(poNumber, vendorCode, warehouseCode, expectedArrival, note, createdBy);

        if (initialLines != null) {
            initialLines.forEach(po::attachLine);
        }
        po.recalculateTotal();
        return po;
    }

    public void updateHeader(String vendorCode, String warehouseCode, LocalDate
            expectedArrival, String note) {
        ensureDraft();
        if (StringUtils.hasText(vendorCode)) this.vendorCode = vendorCode;
        if (StringUtils.hasText(warehouseCode)) this.warehouseCode = warehouseCode;
        this.expectedArrival = expectedArrival;
        this.note = note;
    }

    public void confirm(String confirmedBy) {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
        if (lines.isEmpty()) {
            throw new ApiException(ErrorCode.PO_LINE_REQUIRED);
        }
        if (!StringUtils.hasText(confirmedBy)) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
        this.status = PurchaseOrderStatus.CONFIRMED;
        this.confirmedBy = confirmedBy;
        this.confirmedAt = LocalDateTime.now();
    }


    public void markReceived(String receivedBy) {
        if (this.status == PurchaseOrderStatus.RECEIVED) {
            throw new ApiException(ErrorCode.PO_ALREADY_RECEIVED);
        }
        if (this.status != PurchaseOrderStatus.CONFIRMED) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
        if (!StringUtils.hasText(receivedBy)) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
        this.status = PurchaseOrderStatus.RECEIVED;
        this.receivedBy = receivedBy;
        this.receivedAt = LocalDateTime.now();
    }

    public void cancel() {
        if (this.status == PurchaseOrderStatus.RECEIVED) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
        if (this.status == PurchaseOrderStatus.CANCELED) {
            return;
        }
        this.status = PurchaseOrderStatus.CANCELED;
    }

    public boolean isEditable() {
        return this.status == PurchaseOrderStatus.DRAFT;
    }

    private void attachLine(PurchaseOrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        line.assignTo(this);
        this.lines.add(line);
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
                .map(PurchaseOrderLine::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void ensureDraft() {
        if (this.status != PurchaseOrderStatus.DRAFT) {
            throw new ApiException(ErrorCode.PO_NOT_EDITABLE);
        }
    }

    private static void validateRequired(String poNumber, String vendorCode, String warehouseCode, String createdBy) {
        if (!StringUtils.hasText(poNumber) ||
                !poNumber.matches("^PO-\\d{4}-\\d{6}$")) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
        if (!StringUtils.hasText(vendorCode)
                || !StringUtils.hasText(warehouseCode)
                || !StringUtils.hasText(createdBy)) {
            throw new ApiException(ErrorCode.PO_INVALID_STATE_TRANSITION);
        }
    }
 }
