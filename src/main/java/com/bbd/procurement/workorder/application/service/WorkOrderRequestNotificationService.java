package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import com.bbd.procurement.workorder.application.port.in.ClaimWorkOrderRequestNotificationUseCase;
import com.bbd.procurement.workorder.application.port.in.GetWorkOrderRequestNotificationQuery;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkOrderRequestNotificationService
        implements GetWorkOrderRequestNotificationQuery, ClaimWorkOrderRequestNotificationUseCase {

    /** 이 시간보다 오래된 클레임은 만료로 보고 다른 담당자가 takeover 할 수 있다. */
    private static final Duration CLAIM_TTL = Duration.ofMinutes(30);

    private final LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;
    private final SaveWorkOrderRequestNotificationPort saveWorkOrderRequestNotificationPort;

    @Override
    @Transactional(readOnly = true)
    public List<WorkOrderRequestNotification> list() {
        return loadWorkOrderRequestNotificationPort.findActiveOrderByReceivedAtDesc();
    }

    @Override
    @Transactional
    public WorkOrderRequestNotification claim(String eventId, Long userId, String userName) {
        WorkOrderRequestNotification notification = loadWorkOrderRequestNotificationPort.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOTIFICATION_NOT_FOUND));
        LocalDateTime now = LocalDateTime.now();
        notification.claim(userId, userName, now, now.minus(CLAIM_TTL));
        saveWorkOrderRequestNotificationPort.save(notification);
        return notification;
    }

    @Override
    @Transactional
    public WorkOrderRequestNotification release(String eventId, Long userId) {
        WorkOrderRequestNotification notification = loadWorkOrderRequestNotificationPort.findByEventIdForUpdate(eventId)
                .orElseThrow(() -> new ApiException(ErrorCode.REQUEST_NOTIFICATION_NOT_FOUND));
        notification.releaseClaim(userId);
        saveWorkOrderRequestNotificationPort.save(notification);
        return notification;
    }
}
