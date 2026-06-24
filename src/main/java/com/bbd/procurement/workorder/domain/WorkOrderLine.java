package com.bbd.procurement.workorder.domain;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "work_order_line")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "part_name", nullable = false, length = 200)
    private String partName;

    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "unit", length = 30)
    private String unit;

    @Column(name = "safety_stock", nullable = false)
    private int safetyStock;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "sourcing_type", length = 20)
    private String sourcingType;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subTotal;

    private WorkOrderLine(int lineOrder, String sku, String partName, BigDecimal unitPrice, int quantity,
                          String category, String unit, int safetyStock, boolean active, String sourcingType) {
        this.lineOrder = lineOrder;
        this.sku = sku;
        this.partName = partName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.category = category;
        this.unit = unit;
        this.safetyStock = safetyStock;
        this.active = active;
        this.sourcingType = sourcingType;
        this.subTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static WorkOrderLine create(int lineOrder, String sku, String partName, BigDecimal unitPrice, int quantity,
                                       String category, String unit, int safetyStock, boolean active, String sourcingType) {
        validate(sku, partName, unitPrice, quantity);
        return new WorkOrderLine(lineOrder, sku, partName, unitPrice, quantity,
                category, unit, safetyStock, active, sourcingType);
    }

    void assignTo(WorkOrder workOrder) {
        this.workOrder = workOrder;
    }

    private static void validate(String sku, String partName, BigDecimal unitPrice, int quantity) {
        if (!StringUtils.hasText(sku) || !StringUtils.hasText(partName)) {
            throw new ApiException(ErrorCode.WORK_ORDER_LINE_INVALID);
        }
        if (unitPrice == null || unitPrice.signum() <0) {
            throw new ApiException(ErrorCode.WORK_ORDER_LINE_INVALID);
        }
        if (quantity <= 0) {
            throw new ApiException(ErrorCode.WORK_ORDER_LINE_INVALID);
        }
    }

}
