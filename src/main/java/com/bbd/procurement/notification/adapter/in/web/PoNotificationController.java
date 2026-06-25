package com.bbd.procurement.notification.adapter.in.web;

import com.bbd.procurement.global.response.ApiResponse;
import com.bbd.procurement.notification.adapter.in.web.response.PoNotificationResponse;
import com.bbd.procurement.notification.application.port.in.GetPoNotificationQuery;
import com.bbd.procurement.notification.application.port.in.MarkPoNotificationReadUseCase;
import com.bbd.securitycore.adapter.in.annotation.RequireRole;
import com.bbd.securitycore.domain.UserRole;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "PoNotification", description = "PO 작성 알림 인박스 API (매니저 전용)")
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class PoNotificationController {

    private final GetPoNotificationQuery query;
    private final MarkPoNotificationReadUseCase markReadUseCase;

    @Operation(summary = "PO 알림 인박스 조회", description = "미읽음 알림 최신순 | 권한: HQ_MANAGER")
    @RequireRole(UserRole.HQ_MANAGER)
    @GetMapping
    public ApiResponse<List<PoNotificationResponse>> inbox() {
        List<PoNotificationResponse> result = query.listUnreadForManager().stream()
                .map(PoNotificationResponse::from)
                .toList();
        return ApiResponse.success(result);
    }

    @Operation(summary = "PO 알림 읽음 처리", description = "멱등 — 없는 id는 no-op | 권한: HQ_MANAGER")
    @RequireRole(UserRole.HQ_MANAGER)
    @PatchMapping("/{id}/read")
    public ApiResponse<Void> read(
            @Parameter(description = "알림 id", example = "1")
            @PathVariable Long id) {
        markReadUseCase.markRead(id);
        return ApiResponse.success("읽음 처리되었습니다.", null);
    }
}
