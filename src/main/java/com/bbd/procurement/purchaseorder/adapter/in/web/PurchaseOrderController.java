package com.bbd.procurement.purchaseorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.request.RegisterPurchaseOrderRequest;
import com.bbd.procurement.purchaseorder.adapter.in.web.request.UpdatePurchaseOrderHeaderRequest;
import com.bbd.procurement.purchaseorder.adapter.in.web.request.UpdatePurchaseOrderLinesRequest;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderHistoryResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderResponseAssembler;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderStatsResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderSummaryResponse;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.result.ProcurementStats;
import com.bbd.procurement.purchaseorder.application.port.in.command.CancelPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.application.port.in.command.CompletePurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.application.port.in.command.OrderPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import com.bbd.procurement.notification.application.port.in.CreatePoNotificationUseCase;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.application.model.CurrentUserSnapshotResult;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import com.bbd.securitycore.domain.UserRole;
import com.bbd.securitycore.idempotency.Idempotent;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="PurchaseOrder", description = "PO 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final RegisterPurchaseOrderUseCase registerPurchaseOrderUseCase;
    private final UpdatePurchaseOrderHeaderUseCase updatePurchaseOrderHeaderUseCase;
    private final UpdatePurchaseOrderLinesUseCase updatePurchaseOrderLinesUseCase;
    private final OrderPurchaseOrderUseCase orderPurchaseOrderUseCase;
    private final CompletePurchaseOrderUseCase completePurchaseOrderUseCase;
    private final CancelPurchaseOrderUseCase cancelPurchaseOrderUseCase;
    private final GetPurchaseOrderQuery getPurchaseOrderQuery;
    private final ListPurchaseOrderQuery listPurchaseOrderQuery;
    private final GetPurchaseOrderHistoryQuery getPurchaseOrderHistoryQuery;
    private final GetPurchaseOrderStatsQuery getPurchaseOrderStatsQuery;
    private final GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;
    private final CreatePoNotificationUseCase createPoNotificationUseCase;
    private final PurchaseOrderResponseAssembler responseAssembler;

    @Operation(
            summary = "PO 작성",
            description = "PO 신규 작성 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @Idempotent // 멱등 표준: Idempotency-Key 재요청 빠른길(중복 생성 차단).
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PurchaseOrderResponse> register(
            @Valid @RequestBody RegisterPurchaseOrderRequest request
            ) {
        CurrentUserSnapshotResult snapshot = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot();
        Long userId = snapshot.userId();
        PurchaseOrder po = registerPurchaseOrderUseCase.register(request.toCommand(userId));

        // 스태프가 작성한 경우에만 매니저에게 알림
        if (snapshot.role() == UserRole.HQ_STAFF) {
            try {
                createPoNotificationUseCase.notifyManagerOfPoCreation(po.getPoNumber(), userId);
            } catch (Exception e) {
                log.warn("PO 작성 알림 생성 실패 (PO는 정상 작성됨) poNumber={}", po.getPoNumber(), e);
            }
        }

        return ApiResponse.success("구매 주문이 작성되었습니다.",
                PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 헤더 수정",
            description = "DRAFT 상태의 PO 헤더 정보 수정 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PatchMapping("/{poNumber}")
    public ApiResponse<PurchaseOrderResponse> updateHeader(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber,
            @Valid @RequestBody UpdatePurchaseOrderHeaderRequest request
            ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        PurchaseOrder po = updatePurchaseOrderHeaderUseCase.updateHeader(request.toCommand(poNumber, userId));
        return ApiResponse.success(PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 라인 교체" ,
            description = "DRAFT 상태의 PO 라인 전체 교체 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PutMapping("/{poNumber}/lines")
    public ApiResponse<PurchaseOrderResponse> updateLines(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber,
            @Valid @RequestBody UpdatePurchaseOrderLinesRequest request
            ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        PurchaseOrder po = updatePurchaseOrderLinesUseCase.updateLines(request.toCommand(poNumber, userId));
        return ApiResponse.success(PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 주문",
            description = "DRAFT 상태 PO 주문 처리(DRAFT -> ORDERED). 주문 후에는 수정·취소 불가, 재고 반영은 아직 일어나지 않음 | 권한: HQ_MANAGER"
    )
    @RequireRole(UserRole.HQ_MANAGER)
    @Idempotent // 멱등 표준: Idempotency-Key 재요청 빠른길(중복 주문 차단). 상태전이(DRAFT->ORDERED) 보호
    @PostMapping("/{poNumber}/order")
    public ApiResponse<PurchaseOrderResponse> order(
            @Parameter(description = "PO번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        PurchaseOrder po = orderPurchaseOrderUseCase.order(
                new OrderPurchaseOrderCommand(poNumber, userId)
        );
        return ApiResponse.success("PO가 주문되었습니다.", PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 입고완료",
            description = "ORDERED 상태 PO 입고완료 처리(ORDERED -> RECEIVED). 이 시점에 재고 증가 요청(StockInRequested) 발행 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @Idempotent // 멱등 표준: Idempotency-Key 재요청 빠른길(중복 입고 차단). 상태전이(ORDERED->RECEIVED) 보호
    @PostMapping("/{poNumber}/complete")
    public ApiResponse<PurchaseOrderResponse> complete(
            @Parameter(description = "PO번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        PurchaseOrder po = completePurchaseOrderUseCase.complete(
                new CompletePurchaseOrderCommand(poNumber, userId)
        );
        return ApiResponse.success("PO가 입고완료되었습니다.", PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 취소",
            description = "DRAFT 상태 PO 취소 처리 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PostMapping("/{poNumber}/cancel")
    public ApiResponse<PurchaseOrderResponse> cancel(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        PurchaseOrder po = cancelPurchaseOrderUseCase.cancel(
                new CancelPurchaseOrderCommand(poNumber, userId)
        );
        return ApiResponse.success("PO가 취소되었습니다.", PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "발주 대시보드 집계 조회",
            description = "PO/WO 상태별 카운트(0 포함)와 대기 발주요청/생산요청 수를 1회로 집계 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping("/stats")
    public ApiResponse<PurchaseOrderStatsResponse> stats() {
        ProcurementStats stats = getPurchaseOrderStatsQuery.getStats();
        return ApiResponse.success(new PurchaseOrderStatsResponse(
                stats.poByStatus(),
                stats.woByStatus(),
                stats.pendingPurchaseRequests(),
                stats.pendingWorkOrderRequests()
        ));
    }

    @Operation(
            summary = "PO 단건 조회",
            description = "PO 상세 정보 조회 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping("/{poNumber}")
    public ApiResponse<PurchaseOrderResponse> get(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        PurchaseOrder po = getPurchaseOrderQuery.getByPoNumber(poNumber);
        return ApiResponse.success(PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 목록 조회",
            description = "전체 PO 요약 목록 조회. soNumber 지정 시 해당 SO 연계 PO만(요청 알림 상세 역조회용) |  권한: HQ_MANAGER,HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping
    public ApiResponse<List<PurchaseOrderSummaryResponse>> list(
            @Parameter(description = "SO 번호로 필터(선택). 지정 시 해당 SO 연계 PO만 반환", example = "SO-2026-000001")
            @RequestParam(required = false) String soNumber
    ) {
        List<PurchaseOrder> purchaseOrders = (soNumber == null || soNumber.isBlank())
                ? listPurchaseOrderQuery.list()
                : listPurchaseOrderQuery.listBySoNumber(soNumber);
        List<PurchaseOrderSummaryResponse> result = purchaseOrders.stream()
                .map(PurchaseOrderSummaryResponse::from)
                .toList();
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "PO 변경 이력 조회",
            description = "PO의 생성·수정·완료·취소 이력을 시간순으로 조회 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping("/{poNumber}/history")
    public ApiResponse<List<PurchaseOrderHistoryResponse>> getHistory(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        List<PurchaseOrderHistoryResponse> result =
                getPurchaseOrderHistoryQuery.getHistory(poNumber).stream()
                        .map(responseAssembler::toHistoryResponse)
                        .toList();
        return ApiResponse.success(result);
    }

}
