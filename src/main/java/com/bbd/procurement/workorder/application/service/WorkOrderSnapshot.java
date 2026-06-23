package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderLine;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderSnapshot(
        String workOrderNumber,
        String soNumber,
        String warehouseCode,
        String status,
        BigDecimal totalAmount,
        Long createdBy,
        Long completedBy,
        LocalDateTime completedAt,
        List<LineSnapshot> lines
) {
    public record LineSnapshot(
            int lineOrder,
            String sku,
            String partName,
            BigDecimal unitPrice,
            int quantity,
            BigDecimal subTotal
    ) {
        static LineSnapshot from(WorkOrderLine line) {
            return new LineSnapshot(
                    line.getLineOrder(),
                    line.getSku(),
                    line.getPartName(),
                    line.getUnitPrice(),
                    line.getQuantity(),
                    line.getSubTotal()
            );
        }
    }

    public static WorkOrderSnapshot from(WorkOrder wo) {
        return new WorkOrderSnapshot(
                wo.getWorkOrderNumber(),
                wo.getSoNumber(),
                wo.getWarehouseCode(),
                wo.getStatus().name(),
                wo.getTotalAmount(),
                wo.getCreatedBy(),
                wo.getCompletedBy(),
                wo.getCompletedAt(),
                wo.getLines().stream()
                        .map(LineSnapshot::from)
                        .toList()
        );
    }
}
