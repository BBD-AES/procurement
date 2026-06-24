package com.bbd.procurement.workorder.adapter.in.web.request;

import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderLinesCommand;
import com.bbd.procurement.workorder.application.port.in.command.WorkOrderLineItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record UpdateWorkOrderLinesRequest(

        @Valid
        @NotEmpty(message = "lines는 최소 1개 이상이여야 합니다.")
        List<WorkOrderLineItemRequest> lines
) {
    public UpdateWorkOrderLinesCommand toCommand(String workOrderNumber, Long updatedBy) {
        List<WorkOrderLineItem> items = lines.stream()
                .map(WorkOrderLineItemRequest::toCommandItem)
                .toList();
        return new UpdateWorkOrderLinesCommand(workOrderNumber, items, updatedBy);
    }
}
