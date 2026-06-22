package com.bbd.procurement.workorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.workorder.adapter.in.web.response.WorkOrderRequestNotificationResponse;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderRequestNotificationQuery;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "WorkOrderRequest", description = "생산 요청 알림 API")
@RestController
@RequestMapping("/api/v1/work-order-requests")
@RequiredArgsConstructor
public class WorkOrderRequestController {

    private final GetWorkOrderRequestNotificationQuery getWorkOrderRequestNotificationQuery;

    @Operation(
            summary = "생산 요청 알림 목록 조회",
            description = "sales 발주 요청 중 생산 라인 알림 최신순 조회"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping
    public ApiResponse<List<WorkOrderRequestNotificationResponse>> list() {
        List<WorkOrderRequestNotificationResponse> result = getWorkOrderRequestNotificationQuery.list().stream()
                .map(WorkOrderRequestNotificationResponse::from)
                .toList();
        return ApiResponse.success(result);
    }
}
