package com.bbd.procurement.workorder.domain;

import com.bbd.procurement.global.entity.BaseTimeEntity;
import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Getter
@Entity
@Table(name = "work_order")
@NoArgsConstructor
public class WorkOrder extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 낙관적 락 버전. 동시 상태전이(start/complete) 충돌을 막는다.
     * 두 트랜잭션이 같은 WO를 동시에 전이하면 둘째 flush가 version 불일치로 실패 → 전체 롤백.
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "work_order_number", nullable = false, unique = true, length = 20, updatable = false)
    private String workOrderNumber;

    // 연계 SO. 안전재고 보충(made-to-stock) 생산은 SO 없이도 생성 가능 → nullable.
    @Column(name = "so_number", length = 30)
    private String soNumber;

    @Column(name = "warehouse_code", nullable = false, length = 20)
    private String warehouseCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private WorkOrderStatus status;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "created_by", nullable = false, length = 20, updatable = false)
    private Long createdBy;

    @Column(name = "completed_by", length = 20)
    private Long completedBy;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "request_id", length = 64, updatable = false)
    private String requestId;

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<WorkOrderLine> lines = new ArrayList<>();

    private WorkOrder(String workOrderNumber, String soNumber, String warehouseCode, Long createdBy, String requestId) {
        this.workOrderNumber = workOrderNumber;
        this.soNumber = soNumber;
        this.warehouseCode = warehouseCode;
        this.createdBy = createdBy;
        this.requestId = requestId;
        this.status = WorkOrderStatus.PLANNED;
        this.totalAmount = BigDecimal.ZERO;
    }

    public static WorkOrder create(String workOrderNumber, String soNumber, String warehouseCode, List<WorkOrderLine> initialLines, Long createdBy, String requestId) {
        validateRequired(workOrderNumber, soNumber, warehouseCode, createdBy);

        WorkOrder wo = new WorkOrder(workOrderNumber, soNumber, warehouseCode, createdBy, requestId);

        if (initialLines != null) {
            initialLines.forEach(wo::attachLine);
        }
        wo.recalculateTotal();
        return wo;
    }

    public void start() {
        if (this.status != WorkOrderStatus.PLANNED) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
        this.status = WorkOrderStatus.IN_PRODUCTION;
    }

    public void markCompleted(Long completedBy) {
        if (this.status != WorkOrderStatus.IN_PRODUCTION) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
        if (this.lines.isEmpty()) {
            throw new ApiException(ErrorCode.WORK_ORDER_LINE_REQUIRED);
        }
        if (completedBy == null) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
        this.status = WorkOrderStatus.COMPLETED;
        this.completedBy = completedBy;
        this.completedAt = LocalDateTime.now();
    }

    public boolean cancel() {
        if (this.status == WorkOrderStatus.COMPLETED) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
        if (this.status == WorkOrderStatus.CANCELED) {
            return false;
        }
        this.status = WorkOrderStatus.CANCELED;
        return true;
    }

    public void updateHeader(String warehouseCode, String soNumber) {
        ensurePlanned();
        if (StringUtils.hasText(warehouseCode)) {
            this.warehouseCode = warehouseCode;
        }
        if (StringUtils.hasText(soNumber)) {
            this.soNumber = soNumber;
        }
    }

    public void replaceLines(List<WorkOrderLine> newLines) {
        ensurePlanned();
        this.lines.clear();
        if (newLines != null) {
            newLines.forEach(this::attachLine);
        }
        recalculateTotal();
    }

    private void ensurePlanned() {
        if (this.status != WorkOrderStatus.PLANNED) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
    }

    private void attachLine(WorkOrderLine line) {
        Objects.requireNonNull(line, "line must not be null");
        line.assignTo(this);
        this.lines.add(line);
    }

    private void recalculateTotal() {
        this.totalAmount = lines.stream()
                .map(WorkOrderLine::getSubTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static void validateRequired(String workOrderNumber, String soNumber, String warehouseCode, Long createdBy) {
        if (!StringUtils.hasText(workOrderNumber) || !workOrderNumber.matches("^WO-\\d{4}-\\d{6}$")) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
        // soNumber는 선택(안전재고 보충 생산은 SO 없이 생성 가능).
        if (!StringUtils.hasText(warehouseCode)
        || createdBy == null) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
    }
}
