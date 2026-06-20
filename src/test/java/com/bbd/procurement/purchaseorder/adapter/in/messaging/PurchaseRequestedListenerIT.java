package com.bbd.procurement.purchaseorder.adapter.in.messaging;

import com.bbd.procurement.global.config.KafkaConsumerConfig;
import com.bbd.procurement.purchaseorder.application.port.out.LoadPurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.port.out.SavePurchaseRequestNotificationPort;
import com.bbd.procurement.purchaseorder.application.service.PurchaseRequestNotificationService;
import com.bbd.procurement.purchaseorder.application.service.SourcingResolver;
import com.bbd.procurement.purchaseorder.domain.PurchaseRequestNotification;
import com.bbd.procurement.purchaseorder.domain.SourcingType;
import com.bbd.procurement.shared.inbox.application.port.out.ProcessedEventPort;
import com.bbd.procurement.workorder.application.port.out.SaveWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.domain.WorkOrderRequestNotification;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.kafka.autoconfigure.KafkaAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.context.EmbeddedKafka;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * sales.purchase-requested 토픽 → PurchaseRequestedListener → PurchaseRequestNotificationService
 * 까지의 컨슈머 경로를 EmbeddedKafka(인메모리 브로커)로 통합 검증한다.
 *
 * 영속/외부 의존(포트, SourcingResolver)은 목으로 대체하므로 DB/Redis/Keycloak 없이 동작한다.
 */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = PurchaseRequestedListenerIT.TestApp.class,
        properties = {
                "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
                "spring.kafka.consumer.group-id=procurement-purchase-it",
                "spring.kafka.consumer.auto-offset-reset=earliest",
                "spring.kafka.consumer.key-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.consumer.value-deserializer=org.apache.kafka.common.serialization.StringDeserializer",
                "spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.StringSerializer",
                "spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer"
        }
)
@EmbeddedKafka(partitions = 1, topics = {PurchaseRequestedListenerIT.TOPIC})
class PurchaseRequestedListenerIT {

    static final String TOPIC = "sales.purchase-requested";

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    SavePurchaseRequestNotificationPort savePort;
    @Autowired
    SaveWorkOrderRequestNotificationPort saveWorkOrderPort;
    @Autowired
    ProcessedEventPort processedEventPort;
    @Autowired
    SourcingResolver sourcingResolver;

    @BeforeEach
    void setUp() {
        reset(savePort, saveWorkOrderPort, processedEventPort, sourcingResolver);
        when(processedEventPort.existsByEventId(anyString())).thenReturn(false);
    }

    @Test
    void BUY_라인은_구매요청으로_라우팅된다() {
        when(sourcingResolver.resolve(eq("SKU-001"), any())).thenReturn(SourcingType.BUY);

        kafkaTemplate.send(TOPIC, "SO-1", message("evt-buy", "SO-1", "SKU-001", "BUY"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(savePort, times(1)).save(any(PurchaseRequestNotification.class));
            verify(saveWorkOrderPort, never()).save(any());
            verify(processedEventPort, times(1)).save("evt-buy");
        });
    }

    @Test
    void MAKE_라인은_작업지시로_라우팅된다() {
        when(sourcingResolver.resolve(eq("SKU-002"), any())).thenReturn(SourcingType.MAKE);

        kafkaTemplate.send(TOPIC, "SO-2", message("evt-make", "SO-2", "SKU-002", "MAKE"));

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            verify(saveWorkOrderPort, times(1)).save(any(WorkOrderRequestNotification.class));
            verify(savePort, never()).save(any());
            verify(processedEventPort, times(1)).save("evt-make");
        });
    }

    @Test
    void 이미_처리된_이벤트는_중복제거되어_저장되지_않는다() {
        when(processedEventPort.existsByEventId("evt-dup")).thenReturn(true);

        kafkaTemplate.send(TOPIC, "SO-9", message("evt-dup", "SO-9", "SKU-001", "BUY"));

        // 컨슈머가 메시지를 소비해 멱등성 검사를 수행할 때까지 대기
        await().atMost(Duration.ofSeconds(15))
                .untilAsserted(() -> verify(processedEventPort).existsByEventId("evt-dup"));

        verify(savePort, never()).save(any());
        verify(saveWorkOrderPort, never()).save(any());
        verify(processedEventPort, never()).save(anyString());
    }

    private static String message(String eventId, String soNumber, String sku, String sourcingType) {
        return """
                {
                  "eventId": "%s",
                  "source": "sales",
                  "eventType": "PURCHASE_REQUESTED",
                  "occurredAt": "2026-06-19T00:00:00Z",
                  "soNumber": "%s",
                  "warehouseCode": "WH-01",
                  "lines": [
                    { "sku": "%s", "quantity": 10, "sourcingType": "%s" }
                  ]
                }
                """.formatted(eventId, soNumber, sku, sourcingType);
    }

    /**
     * 전체 애플리케이션 컨텍스트(DB/보안/웹) 대신 Kafka 경로만 띄우기 위한 슬림 설정.
     */
    @Configuration
    @EnableKafka
    @Import(KafkaConsumerConfig.class)
    @ImportAutoConfiguration(KafkaAutoConfiguration.class)
    static class TestApp {

        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }

        @Bean
        SavePurchaseRequestNotificationPort savePurchaseRequestNotificationPort() {
            return mock(SavePurchaseRequestNotificationPort.class);
        }

        @Bean
        LoadPurchaseRequestNotificationPort loadPurchaseRequestNotificationPort() {
            return mock(LoadPurchaseRequestNotificationPort.class);
        }

        @Bean
        SaveWorkOrderRequestNotificationPort saveWorkOrderRequestNotificationPort() {
            return mock(SaveWorkOrderRequestNotificationPort.class);
        }

        @Bean
        ProcessedEventPort processedEventPort() {
            return mock(ProcessedEventPort.class);
        }

        @Bean
        SourcingResolver sourcingResolver() {
            return mock(SourcingResolver.class);
        }

        @Bean
        PurchaseRequestNotificationService purchaseRequestNotificationService(
                ProcessedEventPort processedEventPort,
                SavePurchaseRequestNotificationPort savePort,
                LoadPurchaseRequestNotificationPort loadPort,
                SaveWorkOrderRequestNotificationPort saveWorkOrderPort,
                SourcingResolver sourcingResolver,
                ObjectMapper objectMapper
        ) {
            return new PurchaseRequestNotificationService(
                    processedEventPort, savePort, loadPort, saveWorkOrderPort, sourcingResolver, objectMapper);
        }

        @Bean
        PurchaseRequestedListener purchaseRequestedListener(PurchaseRequestNotificationService service) {
            return new PurchaseRequestedListener(service);
        }
    }
}
