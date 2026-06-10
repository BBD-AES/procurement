package com.bbd.procurement.purchaseorder.adapter.in.web;

import com.bbd.procurement.global.auth.HasRole;
import com.bbd.procurement.global.auth.Role;
import com.bbd.procurement.global.auth.UserContextHolder;
import com.bbd.procurement.global.auth.UserPrincipal;
import com.bbd.procurement.purchaseorder.adapter.in.web.request.RegisterPurchaseOrderRequest;
import com.bbd.procurement.purchaseorder.adapter.in.web.request.UpdatePurchaseOrderHeaderRequest;
import com.bbd.procurement.purchaseorder.adapter.in.web.request.UpdatePurchaseOrderLinesRequest;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseOrderSummaryResponse;
import com.bbd.procurement.purchaseorder.application.port.in.*;
import com.bbd.procurement.purchaseorder.application.port.in.command.CancelPurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.application.port.in.command.CompletePurchaseOrderCommand;
import com.bbd.procurement.purchaseorder.domain.PurchaseOrder;
import io.swagger.v3.oas.annotations.Operation;
import com.bbd.procurement.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="PurchaseOrder", description = "PO 관리 API")
@RestController
@RequestMapping("/api/v1/purchase-orders")
@RequiredArgsConstructor
public class PurchaseOrderController {

    private final RegisterPurchaseOrderUseCase registerPurchaseOrderUseCase;
    private final UpdatePurchaseOrderHeaderUseCase updatePurchaseOrderHeaderUseCase;
    private final UpdatePurchaseOrderLinesUseCase updatePurchaseOrderLinesUseCase;
    private final CompletePurchaseOrderUseCase completePurchaseOrderUseCase;
    private final CancelPurchaseOrderUseCase cancelPurchaseOrderUseCase;
    private final GetPurchaseOrderQuery getPurchaseOrderQuery;
    private final ListPurchaseOrderQuery listPurchaseOrderQuery;

    @Operation(
            summary = "PO 작성",
            description = "PO 신규 작성 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<PurchaseOrderResponse> register(
            @Valid @RequestBody RegisterPurchaseOrderRequest request
            ) {
        String createdBy = UserContextHolder.current().userId();
        PurchaseOrder po = registerPurchaseOrderUseCase.register(request.toCommand(createdBy));
        return ApiResponse.success("구매 주문이 작성되었습니다.",
                PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 헤더 수정",
            description = "DRAFT 상태의 PO 헤더 정보 수정 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @PatchMapping("/{poNumber}")
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<PurchaseOrderResponse> updateHeader(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber,
            @Valid @RequestBody UpdatePurchaseOrderHeaderRequest request
            ) {
        PurchaseOrder po = updatePurchaseOrderHeaderUseCase.updateHeader(request.toCommand(poNumber));
        return ApiResponse.success(PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 라인 교체" ,
            description = "DRAFT 상태의 PO 라인 전체 교체 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @PutMapping("/{poNumber}/lines")
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<PurchaseOrderResponse> updateLines(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber,
            @Valid @RequestBody UpdatePurchaseOrderLinesRequest request
            ) {
        PurchaseOrder po = updatePurchaseOrderLinesUseCase.updateLines(request.toCommand(poNumber));
        return ApiResponse.success(PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 완료",
            description = "DRAFT 상태 PO 완료 처리(DRAFT -> RECEIVED) | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @PostMapping("/{poNumber}/complete")
    @HasRole({Role.HQ_MANAGER})
    public ApiResponse<PurchaseOrderResponse> complete(
            @Parameter(description = "PO번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        String receivedBy = UserContextHolder.current().userId();
        PurchaseOrder po = completePurchaseOrderUseCase.complete(
                new CompletePurchaseOrderCommand(poNumber, receivedBy)
        );
        return ApiResponse.success("PO가 완료되었습니다.", PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 취소",
            description = "PO 취소 처리 | HQ_STAFF는 DRAFT만, HQ_MANAGER는 DRAFT/CONFIRMED 가능"
    )
    @PostMapping("/{poNumber}/cancel")
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<PurchaseOrderResponse> cancel(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        String requesterId = UserContextHolder.current().userId();
        PurchaseOrder po = cancelPurchaseOrderUseCase.cancel(
                new CancelPurchaseOrderCommand(poNumber, requesterId)
        );
        return ApiResponse.success("PO가 취소되었습니다.", PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 단건 조회",
            description = "PO 상세 정보 조회 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @GetMapping("/{poNumber}")
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<PurchaseOrderResponse> get(
            @Parameter(description = "PO 번호", example = "PO-2026-000001")
            @PathVariable String poNumber
    ) {
        PurchaseOrder po = getPurchaseOrderQuery.getByPoNumber(poNumber);
        return ApiResponse.success(PurchaseOrderResponse.from(po));
    }

    @Operation(
            summary = "PO 목록 조회",
            description = "전체 PO 요약 목록 조회 |  권한: HQ_MANAGER,HQ_STAFF"
    )
    @GetMapping
    @HasRole({Role.HQ_MANAGER, Role.HQ_STAFF})
    public ApiResponse<List<PurchaseOrderSummaryResponse>> list() {
        List<PurchaseOrderSummaryResponse> result =
                listPurchaseOrderQuery.list().stream()
                        .map(PurchaseOrderSummaryResponse::from)
                        .toList();
        return ApiResponse.success(result);
    }

}
