package com.bbd.procurement.purchaseorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.SalesOrderRelayResponse;
import com.bbd.procurement.purchaseorder.application.port.in.GetSalesOrderQuery;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "SalesOrder", description = "SO 조회 중계 API")
@RestController
@RequestMapping("/api/v1/sales-orders")
@RequiredArgsConstructor
public class SalesOrderController {

    private final GetSalesOrderQuery getSalesOrderQuery;

    @Operation(
            summary = "SO 상세 조회",
            description = "발주 요청 기반 PO 작성을 위해 Sales 서비스의 SO 상세를 조회 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping("/{soNumber}")
    public ApiResponse<SalesOrderRelayResponse> get(
            @Parameter(description = "SO 번호", example = "SO-2026-0001")
            @PathVariable String soNumber
    ) {
        SalesOrderRelayResponse response =
                SalesOrderRelayResponse.from(getSalesOrderQuery.getBySoNumber(soNumber));
        return ApiResponse.success(response);
    }
}
