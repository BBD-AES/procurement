package com.bbd.procurement.workorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.workorder.adapter.in.web.request.CreateWorkOrderRequest;
import com.bbd.procurement.workorder.adapter.in.web.request.UpdateWorkOrderHeaderRequest;
import com.bbd.procurement.workorder.adapter.in.web.request.UpdateWorkOrderLinesRequest;
import com.bbd.procurement.workorder.adapter.in.web.response.WorkOrderHistoryResponse;
import com.bbd.procurement.workorder.adapter.in.web.response.WorkOrderHistoryResponseAssembler;
import com.bbd.procurement.workorder.adapter.in.web.response.WorkOrderResponse;
import com.bbd.procurement.workorder.application.port.in.CancelWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.CompleteWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.CreateWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderHistoryQuery;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderQuery;
import com.bbd.procurement.workorder.application.port.in.StartWorkOrderUseCase;
import com.bbd.procurement.workorder.application.port.in.UpdateWorkOrderHeaderUseCase;
import com.bbd.procurement.workorder.application.port.in.UpdateWorkOrderLinesUseCase;
import com.bbd.procurement.workorder.application.port.in.command.CancelWorkOrderCommand;
import com.bbd.procurement.workorder.application.port.in.command.CompleteWorkOrderCommand;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import com.bbd.securitycore.domain.UserRole;
import com.bbd.securitycore.idempotency.Idempotent;
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
    private final CancelWorkOrderUseCase cancelWorkOrderUseCase;
    private final UpdateWorkOrderHeaderUseCase updateWorkOrderHeaderUseCase;
    private final UpdateWorkOrderLinesUseCase updateWorkOrderLinesUseCase;
    private final GetWorkOrderQuery getWorkOrderQuery;
    private final GetWorkOrderHistoryQuery getWorkOrderHistoryQuery;
    private final GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;
    private final WorkOrderHistoryResponseAssembler historyResponseAssembler;

    @Operation(
            summary = "작업지시 생성",
            description = "생산 요청 알림 기반 작업 지시 생성 (PlANNED)"
    )
    @Idempotent // 멱등 표준: Idempotency-Key 재요청 빠른길(중복 생성 차단). docs/idempotency-spec.md
    @PostMapping
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WorkOrderResponse> create(
            @Valid @RequestBody CreateWorkOrderRequest request
            ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        WorkOrder wo = createWorkOrderUseCase.create(request.toCommand(userId));
        return ApiResponse.success("작업 지시가 생성되었습니다.", WorkOrderResponse.from(wo));
    }

    @Operation(
            summary = "작업 지시 착수",
            description = "PLANNED -> IN_PRODUCTION"
    )
    @RequireRole(UserRole.HQ_MANAGER)
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
    @RequireRole(UserRole.HQ_MANAGER)
    @PostMapping("/{workOrderNumber}/complete")
    public ApiResponse<WorkOrderResponse> complete(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        WorkOrder workOrder = completeWorkOrderUseCase.complete(new CompleteWorkOrderCommand(workOrderNumber, userId));
        return ApiResponse.success("작업 지시가 완료되었습니다.", WorkOrderResponse.from(workOrder));
    }

    @Operation(
            summary = "작업 지시 취소",
            description = "PLANNED/IN_PRODUCTION -> CANCELED (COMPLETED는 불가, 이미 취소면 멱등 no-op) | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PostMapping("/{workOrderNumber}/cancel")
    public ApiResponse<WorkOrderResponse> cancel(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        WorkOrder workOrder = cancelWorkOrderUseCase.cancel(new CancelWorkOrderCommand(workOrderNumber, userId));
        return ApiResponse.success("작업 지시가 취소되었습니다.", WorkOrderResponse.from(workOrder));
    }

    @Operation(
            summary = "작업지시 헤더 수정",
            description = "PLANNED 상태의 작업지시 헤더(생산창고·연계 SO) 수정 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PatchMapping("/{workOrderNumber}")
    public ApiResponse<WorkOrderResponse> updateHeader(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber,
            @Valid @RequestBody UpdateWorkOrderHeaderRequest request
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        WorkOrder workOrder = updateWorkOrderHeaderUseCase.updateHeader(request.toCommand(workOrderNumber, userId));
        return ApiResponse.success(WorkOrderResponse.from(workOrder));
    }

    @Operation(
            summary = "작업지시 라인 교체",
            description = "PLANNED 상태의 작업지시 라인 전체 교체 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PutMapping("/{workOrderNumber}/lines")
    public ApiResponse<WorkOrderResponse> updateLines(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber,
            @Valid @RequestBody UpdateWorkOrderLinesRequest request
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        WorkOrder workOrder = updateWorkOrderLinesUseCase.updateLines(request.toCommand(workOrderNumber, userId));
        return ApiResponse.success(WorkOrderResponse.from(workOrder));
    }

    @Operation(summary = "작업 지시 단건 조회")
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping("/{workOrderNumber}")
    public ApiResponse<WorkOrderResponse> get(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber
    ) {
        WorkOrder workOrder = getWorkOrderQuery.getByWorkOrderNumber(workOrderNumber);
        return ApiResponse.success(WorkOrderResponse.from(workOrder));
    }

    @Operation(
            summary = "작업지시 변경 이력 조회",
            description = "작업지시의 헤더수정·라인교체·취소 이력을 시간순으로 조회 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping("/{workOrderNumber}/history")
    public ApiResponse<List<WorkOrderHistoryResponse>> getHistory(
            @Parameter(description = "작업 지시 번호", example = "WO-2026-000001")
            @PathVariable String workOrderNumber
    ) {
        List<WorkOrderHistoryResponse> result =
                getWorkOrderHistoryQuery.getHistory(workOrderNumber).stream()
                        .map(historyResponseAssembler::toHistoryResponse)
                        .toList();
        return ApiResponse.success(result);
    }

    @Operation(summary = "작업 지시 목록 조회")
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping
    public ApiResponse<List<WorkOrderResponse>> list() {
        List<WorkOrderResponse> result = getWorkOrderQuery.list().stream()
                .map(WorkOrderResponse::from)
                .toList();
        return ApiResponse.success(result);
    }

}
