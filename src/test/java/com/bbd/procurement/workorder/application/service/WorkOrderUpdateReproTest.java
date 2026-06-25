package com.bbd.procurement.workorder.application.service;

import com.bbd.procurement.purchaseorder.application.port.out.LoadItemPort;
import com.bbd.procurement.purchaseorder.application.port.out.result.ItemResult;
import com.bbd.procurement.shared.outbox.application.port.SaveOutboxEventPort;
import com.bbd.procurement.workorder.adapter.out.persistence.WorkOrderHistoryPersistenceAdapter;
import com.bbd.procurement.workorder.adapter.out.persistence.WorkOrderPersistenceAdapter;
import com.bbd.procurement.workorder.adapter.out.persistence.repository.WorkOrderJpaRepository;
import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderHeaderCommand;
import com.bbd.procurement.workorder.application.port.in.command.UpdateWorkOrderLinesCommand;
import com.bbd.procurement.workorder.application.port.in.command.WorkOrderLineItem;
import com.bbd.procurement.workorder.application.port.out.LoadWorkOrderRequestNotificationPort;
import com.bbd.procurement.workorder.application.port.out.WorkOrderNumberGeneratorPort;
import com.bbd.procurement.workorder.domain.WorkOrder;
import com.bbd.procurement.workorder.domain.WorkOrderLine;
import com.bbd.securitycore.application.model.CurrentUserSnapshotResult;
import com.bbd.securitycore.application.port.in.GetCurrentUserSnapshotUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * WO 수정(헤더/라인) 500 C999 재현 — 실제 JPA flush(이력 INSERT 포함)까지 통과시킨다.
 * 엔티티 기준(ddl-auto) 스키마 + 실 objectMapper + 실 repository, 외부 포트만 mock.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Import({WorkOrderService.class, WorkOrderPersistenceAdapter.class, WorkOrderHistoryPersistenceAdapter.class, WorkOrderUpdateReproTest.MapperConfig.class})
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.jpa.show-sql=true",
})
class WorkOrderUpdateReproTest {

    @TestConfiguration
    static class MapperConfig {
        @Bean
        ObjectMapper objectMapper() {
            return JsonMapper.builder().build();
        }
    }

    @Autowired WorkOrderService sut;
    @Autowired WorkOrderJpaRepository woRepo;

    @MockitoBean WorkOrderNumberGeneratorPort workOrderNumberGeneratorPort;
    @MockitoBean LoadItemPort loadItemPort;
    @MockitoBean SaveOutboxEventPort saveOutboxEventPort;
    @MockitoBean LoadWorkOrderRequestNotificationPort loadWorkOrderRequestNotificationPort;
    @MockitoBean GetCurrentUserSnapshotUseCase getCurrentUserSnapshotUseCase;

    private static final String WO = "WO-2026-000010";

    @BeforeEach
    void setUp() {
        CurrentUserSnapshotResult snap = mock(CurrentUserSnapshotResult.class);
        lenient().when(snap.displayName()).thenReturn("테스터");
        lenient().when(getCurrentUserSnapshotUseCase.getCurrentUserSnapshot()).thenReturn(snap);
        lenient().when(loadItemPort.findBySku("SKU-2"))
                .thenReturn(new ItemResult("SKU-2", "부품2", 100, "MAKE", "C", "EA", 0, true));

        // PLANNED 상태 WO 1건 적재(라인 1줄)
        WorkOrder wo = WorkOrder.create(WO, "SO-1", "WH-HQ-001",
                List.of(WorkOrderLine.create(1, "SKU-1", "부품1", new BigDecimal("100"), 2, "C", "EA", 0, true, "MAKE")),
                1L, null);
        woRepo.saveAndFlush(wo);
    }

    @Test
    @DisplayName("라인 교체 — flush까지 (이력 INSERT 포함)")
    void updateLines_flush() {
        sut.updateLines(new UpdateWorkOrderLinesCommand(WO,
                List.of(new WorkOrderLineItem(1, "SKU-2", 3)), 1L));
        woRepo.flush(); // 더티체킹 강제 flush — 여기서 터지면 스택 노출
    }

    @Test
    @DisplayName("헤더 수정 — flush까지 (이력 INSERT 포함)")
    void updateHeader_flush() {
        sut.updateHeader(new UpdateWorkOrderHeaderCommand(WO, "WH-HQ-002", "SO-9", 1L));
        woRepo.flush();
    }
}
