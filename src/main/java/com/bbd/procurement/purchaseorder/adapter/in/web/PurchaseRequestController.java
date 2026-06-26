package com.bbd.procurement.purchaseorder.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.purchaseorder.adapter.in.web.response.PurchaseRequestNotificationResponse;
import com.bbd.procurement.purchaseorder.application.port.in.ClaimPurchaseRequestNotificationUseCase;
import com.bbd.procurement.purchaseorder.application.port.in.GetPurchaseRequestNotificationQuery;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.application.model.CurrentUserSnapshotResult;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import com.bbd.securitycore.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "PurchaseRequest", description = "Sales 발주 요청 알림 API")
@RestController
@RequestMapping("/api/v1/purchase-requests")
@RequiredArgsConstructor
public class PurchaseRequestController {

    private final GetPurchaseRequestNotificationQuery getPurchaseRequestNotificationQuery;
    private final ClaimPurchaseRequestNotificationUseCase claimPurchaseRequestNotificationUseCase;
    private final GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;

    @Operation(
            summary = "발주 요청 알림 목록 조회",
            description = "Sales가 발행한 발주 요청 알림 최신순 조회"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @GetMapping
    public ApiResponse<List<PurchaseRequestNotificationResponse>> list() {
        List<PurchaseRequestNotificationResponse> result = getPurchaseRequestNotificationQuery.list().stream()
                .map(PurchaseRequestNotificationResponse::from)
                .toList();
        return ApiResponse.success(result);
    }

    @Operation(
            summary = "발주 요청 처리중 선점(claim)",
            description = "요청을 담당자가 처리중으로 선점해 다른 담당자의 중복 발주를 막는다. 이미 다른 담당자가 처리중이면 409 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PostMapping("/{eventId}/claim")
    public ApiResponse<PurchaseRequestNotificationResponse> claim(
            @Parameter(description = "요청 알림 eventId") @PathVariable String eventId
    ) {
        CurrentUserSnapshotResult snapshot = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot();
        PurchaseRequestNotification notification =
                claimPurchaseRequestNotificationUseCase.claim(eventId, snapshot.userId(), snapshot.displayName());
        return ApiResponse.success("요청을 처리중으로 선점했습니다.", PurchaseRequestNotificationResponse.from(notification));
    }

    @Operation(
            summary = "발주 요청 처리중 해제(release)",
            description = "본인이 선점한 요청의 처리중 상태를 해제한다 | 권한: HQ_MANAGER, HQ_STAFF"
    )
    @RequireRole({UserRole.HQ_MANAGER, UserRole.HQ_STAFF})
    @PostMapping("/{eventId}/release")
    public ApiResponse<PurchaseRequestNotificationResponse> release(
            @Parameter(description = "요청 알림 eventId") @PathVariable String eventId
    ) {
        Long userId = getCurrentUserSnapshotUseCase.getCurrentUserSnapshot().userId();
        PurchaseRequestNotification notification =
                claimPurchaseRequestNotificationUseCase.release(eventId, userId);
        return ApiResponse.success("요청 처리중 상태를 해제했습니다.", PurchaseRequestNotificationResponse.from(notification));
    }
}
