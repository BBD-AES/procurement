package com.bbd.procurement.workorder.adapter.in.web.response;

import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record WorkOrderResponse(
        String workOrderNumber,
        String soNumber,
        String warehouseCode,
        WorkOrderStatus status,
        BigDecimal totalAmount,
        String createdBy,
        String completedBy,
        LocalDateTime createdAt,
        LocalDateTime completedAt,
        List<WorkOrderLineResponse> lines
) {
    public static WorkOrderResponse from(WorkOrder wo) {
        return new WorkOrderResponse(
                wo.getWorkOrderNumber(), wo.getSoNumber(), wo.getWarehouseCode(),
                wo.getStatus(), wo.getTotalAmount(), wo.getCreatedBy(),
                wo.getCompletedBy(), wo.getCreatedAt(), wo.getCompletedAt(),
                wo.getLines().stream()
                        .map(WorkOrderLineResponse::from).toList()
        );
    }
}
