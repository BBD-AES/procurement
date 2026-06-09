package com.bbd.procurement.purchaseorder.domain;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "purchase_order_line")
@NoArgsConstructor
public class PurchaseOrderLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_order_id", nullable = false)
    private PurchaseOrder purchaseOrder;

    @Column(name = "line_order", nullable = false)
    private int lineOrder;

    @Column(name = "sku", nullable = false, length = 50)
    private String sku;

    @Column(name = "part_name", nullable = false, length = 200)
    private String partName;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    private PurchaseOrderLine(int lineOrder, String sku, String partName, BigDecimal unitPrice, int quantity) {
        this.lineOrder = lineOrder;
        this.sku = sku;
        this.partName = partName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public static PurchaseOrderLine create(int lineOrder, String sku, String partName, BigDecimal unitPrice, int quantity) {
        validate(sku, partName, unitPrice, quantity);
        return new PurchaseOrderLine(lineOrder, sku, partName, unitPrice, quantity);
    }

    void assignTo(PurchaseOrder purchaseOrder) {
        this.purchaseOrder = purchaseOrder;
    }

    private static void validate(String sku, String partName, BigDecimal unitPrice, int quantity) {
        if (!StringUtils.hasText(sku) || !StringUtils.hasText(partName)) {
            throw new ApiException(ErrorCode.PO_LINE_INVALID);
        }
        if (unitPrice == null || unitPrice.signum() < 0) {
            throw new ApiException(ErrorCode.PO_LINE_INVALID);
        }
        if (quantity <= 0) {
            throw new ApiException(ErrorCode.PO_LINE_INVALID);
        }
    }

}
