package com.bbd.procurement.workorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.workorder.adapter.in.web.request.CreateWorkOrderRequest;
import com.bbd.procurement.workorder.adapter.in.web.response.WorkOrderResponse;
import com.bbd.procurement.workorder.application.port.in.CompleteWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.CreateWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderQuery;
import com.bbd.procurement.workorder.application.port.in.StartWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.command.CompleteWorkOrderCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "WorkOrder", description = "작업지시(생산) 관리 API")
@RestController
@RequestMapping("/api/v1/work-orders")
@RequiredArgsConstructor
public class WorkOrderController {

    private final CreateWorkOrderUseCase createWorkOrderUseCase;
    private final StartWorkOrderUseCase startWorkOrderUseCase;
    private final CompleteWorkOrderUseCase completeWorkOrderUseCase;
    private final GetWorkOrderQuery getWorkOrderQuery;

    @Operation(
            summary = "작업지시 생성",
            description = "생산 요청 알림 기반 작업 지시 생성 (PlANNED)"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkOrderResponse> create(
            @Parameter(description = "작업자 사번")
            @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId,
            @Valid @RequestBody CreateWorkOrderRequest request
            ) {
        WorkOrder wo = createWorkOrderUseCase.create(request.toCommand(userId));
        return ApiResponse.success("작업 지시가 생성되었습니다.", WorkOrderResponse.from(wo));
    }

    @Operation(
            summary = "작업 지시 착수",
            description = "PLANNED -> IN_PRODUCTION"
    )
    @PostMapping("/{workOrderNumber}/start")
    public ApiResponse<WorkOrderResponse> start(
            @PathVariable String workOrderNumber
    ) {
        WorkOrder workOrder = startWorkOrderUseCase.start(workOrderNumber);
        return ApiResponse.success("작업 지시가 착수되었습니다.", WorkOrderResponse.from(workOrder));
    }

    @Operation(
            summary = "작업 지시 완료",
            description = "IN_PRODUCTION -> COMPLETED + StockInRequested 발행"
    )
    @PostMapping("/{workOrderNumber}/complete")
    public ApiResponse<WorkOrderResponse> complete(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber,
            @Parameter(description = "작업자 사번") @RequestHeader(value = "X-User-Id", defaultValue = "SYSTEM") String userId
    ) {
        WorkOrder workOrder = completeWorkOrderUseCase.complete(new CompleteWorkOrderCommand(workOrderNumber, userId));
        return ApiResponse.success("작업 지시가 완료되었습니다.", WorkOrderResponse.from(workOrder));
    }

    @Operation(summary = "작업 지시 단건 조회")
    @GetMapping("/{workOrderNumber}")
    public ApiResponse<WorkOrderResponse> get(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber
    ) {
        WorkOrder workOrder = getWorkOrderQuery.getByWorkOrderNumber(workOrderNumber);
        return ApiResponse.success(WorkOrderResponse.from(workOrder));
    }

    @Operation(summary = "작업 지시 목록 조회")
    @GetMapping
    public ApiResponse<List<WorkOrderResponse>> list() {
        List<WorkOrderResponse> result = getWorkOrderQuery.list().stream()
                .map(WorkOrderResponse::from)
                .toList();
        return ApiResponse.success(result);
    }

}
