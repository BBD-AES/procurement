package com.bbd.procurement.purchaseorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseRequestNotificationResponse;
import com.bbd.procurement.purchaseorder.application.port.in.GetPurchaseRequestNotificationQuery;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Tag(name = "PurchaseRequest", description = "Sales 발주 요청 알림 API")
@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final GetPurchaseRequestNotificationQuery getPurchaseRequestNotificationQuery;
    private final ObjectMapper objectMapper;

    @Operation(
            summary = "발주 요청 알림 목록 조회",
            description = "Sales가 발행한 발주 요청 알림 최신순 조회"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping
    public ApiResponse<List<PurchaseRequestNotificationResponse>> list() {
        List<PurchaseRequestNotificationResponse> result = getPurchaseRequestNotificationQuery.list().stream()
                .map(n ->
                PurchaseRequestNotificationResponse.from(n, objectMapper))
                .toList();
        return ApiResponse.success(result);
    }
}
