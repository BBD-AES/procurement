package com.bbd.procurement.shared.outbox.adapter.out.messaging;

import com.bbd.procurement.shared.outbox.adapter.out.persistence.OutboxEventJpaRepository;
import com.bbd.procurement.shared.outbox.domain.OutboxEvent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * MessageRelay 발행/실패 분기 단위테스트 (이슈 #64).
 *
 * Outbox 폴링→Kafka 발행 루프의 분기를 검증한다.
 *  - 빈 배치: 조기 return (Kafka 미호출)
 *  - 정상 발행: markProcessed 호출
 *  - 일반 예외: 로그만 남기고 다음 이벤트로 계속
 *  - InterruptedException: 인터럽트 플래그 복원 후 즉시 break
 * 영속/카프카 의존은 모두 목으로 격리한다.
 */
@ExtendWith(MockitoExtension.class)
class MessageRelayTest {

    @Mock OutboxEventJpaRepository outboxEventJpaRepository;
    @Mock KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks MessageRelay sut;

    @AfterEach
    void clearInterrupt() {
        // 인터럽트 테스트가 남긴 플래그를 정리해 다른 테스트에 영향 없게 한다.
        Thread.interrupted();
    }

    /** 발행에 필요한 게터만 stub한 OutboxEvent 목. */
    private OutboxEvent eventMock() {
        OutboxEvent event = mock(OutboxEvent.class);
        lenient().when(event.getTopic()).thenReturn("procurement.stock-in-requested");
        lenient().when(event.getAggregateId()).thenReturn("PO-2026-000001");
        lenient().when(event.getPayload()).thenReturn("{}");
        lenient().when(event.getId()).thenReturn(1L);
        lenient().when(event.getEventId()).thenReturn(UUID.randomUUID());
        return event;
    }

    @Test
    @DisplayName("폴링 결과가 비어 있으면 Kafka를 호출하지 않고 조기 return한다")
    void 빈_배치면_조기return() {
        when(outboxEventJpaRepository.findByProcessedAtIsNullOrderByOccurredAtAsc(any(Pageable.class)))
                .thenReturn(List.of());

        sut.relay();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("정상 발행되면 해당 이벤트를 markProcessed 처리한다")
    void 정상_발행시_markProcessed() {
        OutboxEvent event = eventMock();
        when(outboxEventJpaRepository.findByProcessedAtIsNullOrderByOccurredAtAsc(any(Pageable.class)))
                .thenReturn(List.of(event));
        doReturn(CompletableFuture.completedFuture(null))
                .when(kafkaTemplate).send(anyString(), anyString(), anyString());

        sut.relay();

        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
        verify(event, times(1)).markProcessed();
    }

    @Test
    @DisplayName("한 이벤트가 일반 예외로 실패해도 다음 이벤트는 계속 발행한다")
    void 일반예외면_다음이벤트_계속() {
        OutboxEvent failing = eventMock();
        OutboxEvent ok = eventMock();
        when(outboxEventJpaRepository.findByProcessedAtIsNullOrderByOccurredAtAsc(any(Pageable.class)))
                .thenReturn(List.of(failing, ok));
        // 첫 호출은 예외, 두 번째 호출은 성공
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new RuntimeException("kafka down"))
                .thenReturn(CompletableFuture.completedFuture(null));

        sut.relay();

        verify(failing, never()).markProcessed();
        verify(ok, times(1)).markProcessed();
    }

    @Test
    @DisplayName("InterruptedException 발생 시 인터럽트 플래그를 복원하고 즉시 break한다")
    void 인터럽트면_플래그복원_후_break() throws Exception {
        OutboxEvent first = eventMock();
        OutboxEvent second = eventMock();
        when(outboxEventJpaRepository.findByProcessedAtIsNullOrderByOccurredAtAsc(any(Pageable.class)))
                .thenReturn(List.of(first, second));

        @SuppressWarnings("unchecked")
        CompletableFuture<Object> interrupting = mock(CompletableFuture.class);
        when(interrupting.get(anyLong(), any())).thenThrow(new InterruptedException());
        doReturn(interrupting).when(kafkaTemplate).send(anyString(), anyString(), anyString());

        sut.relay();

        // 인터럽트 플래그가 복원되어 있어야 한다
        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        // 첫 이벤트에서 break → 두 이벤트 모두 markProcessed 미호출, send도 1회만
        verify(first, never()).markProcessed();
        verify(second, never()).markProcessed();
        verify(kafkaTemplate, times(1)).send(anyString(), anyString(), anyString());
    }
}
