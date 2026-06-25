package com.bbd.procurement.notification.application.service;

import com.bbd.procurement.notification.application.port.out.LoadPoNotificationPort;
import com.bbd.procurement.notification.application.port.out.SavePoNotificationPort;
import com.bbd.procurement.notification.domain.PoNotification;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * PoNotificationService 단위테스트 (이슈 #79).
 *  - notifyManagerOfPoCreation: HQ_MANAGER 대상 알림 1건 생성(poNumber 보존)
 *  - markRead: 존재하면 읽음 처리, 없는 id는 no-op(멱등)
 *  - listUnreadForManager: HQ_MANAGER 미읽음 조회 위임
 */
@ExtendWith(MockitoExtension.class)
class PoNotificationServiceTest {

    @Mock SavePoNotificationPort savePort;
    @Mock LoadPoNotificationPort loadPort;

    @InjectMocks PoNotificationService sut;

    private static final String PO = "PO-2026-000001";

    @Test
    @DisplayName("PO 작성 알림은 HQ_MANAGER 대상으로 생성되고 poNumber를 보존한다")
    void notifyManager_savesManagerNotification() {
        sut.notifyManagerOfPoCreation(PO, 7L);

        ArgumentCaptor<PoNotification> captor = ArgumentCaptor.forClass(PoNotification.class);
        verify(savePort).save(captor.capture());
        PoNotification saved = captor.getValue();
        assertThat(saved.getTargetRole()).isEqualTo("HQ_MANAGER");
        assertThat(saved.getPoNumber()).isEqualTo(PO);
        assertThat(saved.getCreatedBy()).isEqualTo(7L);
        assertThat(saved.isRead()).isFalse();
        assertThat(saved.getMessage()).contains(PO);
    }

    @Test
    @DisplayName("markRead: 존재하는 알림은 읽음 처리된다")
    void markRead_existing_marksRead() {
        PoNotification n = PoNotification.forManager(PO, "msg", 7L);
        when(loadPort.findById(1L)).thenReturn(Optional.of(n));

        sut.markRead(1L);

        assertThat(n.isRead()).isTrue();
    }

    @Test
    @DisplayName("markRead: 없는 id는 no-op(멱등) — 예외 없이 통과")
    void markRead_missing_isNoOp() {
        when(loadPort.findById(999L)).thenReturn(Optional.empty());

        sut.markRead(999L);   // 예외 없이 통과해야 함

        verify(savePort, never()).save(any());
    }

    @Test
    @DisplayName("listUnreadForManager: HQ_MANAGER 미읽음 최신순 조회를 위임한다")
    void listUnread_delegatesToManagerBucket() {
        PoNotification n = PoNotification.forManager(PO, "msg", 7L);
        when(loadPort.findTop100ByTargetRoleAndReadFalseOrderByIdDesc("HQ_MANAGER"))
                .thenReturn(List.of(n));

        List<PoNotification> result = sut.listUnreadForManager();

        assertThat(result).containsExactly(n);
    }
}
