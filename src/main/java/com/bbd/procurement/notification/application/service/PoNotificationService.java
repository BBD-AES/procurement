package com.bbd.procurement.notification.application.service;

import com.bbd.procurement.notification.application.port.in.CreatePoNotificationUseCase;
import com.bbd.procurement.notification.application.port.in.GetPoNotificationQuery;
import com.bbd.procurement.notification.application.port.in.MarkPoNotificationReadUseCase;
import com.bbd.procurement.notification.application.port.out.LoadPoNotificationPort;
import com.bbd.procurement.notification.application.port.out.SavePoNotificationPort;
import com.bbd.procurement.notification.domain.PoNotification;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PO 작성 알림 인박스 서비스 (이슈 #79).
 * 조회/읽음/생성 유스케이스를 한 서비스로 구현 — 알림은 비핵심 read-model.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PoNotificationService implements
        GetPoNotificationQuery, MarkPoNotificationReadUseCase, CreatePoNotificationUseCase {

    private static final String TARGET_MANAGER = "HQ_MANAGER";

    private final SavePoNotificationPort savePort;
    private final LoadPoNotificationPort loadPort;

    @Override
    public List<PoNotification> listUnreadForManager() {
        return loadPort.findTop100ByTargetRoleAndReadFalseOrderByIdDesc(TARGET_MANAGER);
    }

    @Override
    @Transactional
    public void markRead(Long id) {
        // 더티체킹으로 read=true 반영. 없는 id면 no-op(멱등).
        loadPort.findById(id).ifPresent(PoNotification::markRead);
    }

    @Override
    @Transactional
    public void notifyManagerOfPoCreation(String poNumber, Long createdBy) {
        savePort.save(PoNotification.forManager(
                poNumber,
                "스태프가 발주 " + poNumber + " 를 작성했습니다 — 검토하세요",
                createdBy));
    }
}
