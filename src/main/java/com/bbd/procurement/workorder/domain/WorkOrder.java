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

    @Column(name = "work_order_number", nullable = false, unique = true, length = 20, updatable = false)
    private String workOrderNumber;

    @Column(name = "so_number", nullable = false, length = 30, updatable = false)
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

    @OneToMany(mappedBy = "workOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("lineOrder ASC")
    private List<WorkOrderLine> lines = new ArrayList<>();

    private WorkOrder(String workOrderNumber, String soNumber, String warehouseCode, Long createdBy) {
        this.workOrderNumber = workOrderNumber;
        this.soNumber = soNumber;
        this.warehouseCode = warehouseCode;
        this.createdBy = createdBy;
        this.status = WorkOrderStatus.PLANNED;
        this.totalAmount = BigDecimal.ZERO;
    }

    public static WorkOrder create(String workOrderNumber, String soNumber, String warehouseCode, List<WorkOrderLine> initialLines, Long createdBy) {
        validateRequired(workOrderNumber, soNumber, warehouseCode, createdBy);

        WorkOrder wo = new WorkOrder(workOrderNumber, soNumber, warehouseCode, createdBy);

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

    public void cancel() {
        if (this.status == WorkOrderStatus.COMPLETED) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
        if (this.status == WorkOrderStatus.CANCELED) {
            return;
        }
        this.status = WorkOrderStatus.CANCELED;
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
        if (!StringUtils.hasText(soNumber)
        || !StringUtils.hasText(warehouseCode)
        || createdBy == null) {
            throw new ApiException(ErrorCode.WORK_ORDER_INVALID_STATE_TRANSITION);
        }
    }
}
