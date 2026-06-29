package com.bbd.procurement.workorder.domain;

import com.bbd.procurement.global.error.ApiException;
import com.bbd.procurement.global.error.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "work_order_request_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WorkOrderRequestNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 낙관적 락 — 두 담당자가 같은 요청을 동시에 선점(claim)하면 둘째 flush가 version 불일치로 실패.
    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "event_id", nullable = false, unique = true, length = 36, updatable = false)
    private String eventId;

    @Column(name = "so_number", nullable = false, length = 30, updatable = false)
    private String soNumber;

    @Column(name = "warehouse_code", nullable = false, length = 20, updatable = false)
    private String warehouseCode;

    @Column(name = "payload", nullable = false, columnDefinition = "TEXT", updatable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private WorkOrderRequestStatus status;

    @Column(name = "received_at", nullable = false, updatable = false)
    private LocalDateTime receivedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // "처리중" 클레임 — 한 담당자가 선점하면 다른 담당자에게 처리중으로 보임(중복 작업지시 방지).
    @Column(name = "claimed_by")
    private Long claimedBy;

    @Column(name = "claimed_by_name", length = 100)
    private String claimedByName;

    @Column(name = "claimed_at")
    private LocalDateTime claimedAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<WorkOrderRequestNotificationLine> lines = new ArrayList<>();

    private WorkOrderRequestNotification(String eventId, String soNumber, String warehouseCode,
                                         String payload, LocalDateTime receivedAt) {
        this.eventId = eventId;
        this.soNumber = soNumber;
        this.warehouseCode = warehouseCode;
        this.payload = payload;
        this.receivedAt = receivedAt;
        this.status = WorkOrderRequestStatus.PENDING;
    }

    public static WorkOrderRequestNotification create(String eventId, String soNumber, String warehouseCode,
                                                      String payload, LocalDateTime receivedAt,
                                                      List<WorkOrderRequestNotificationLine> lines) {
        WorkOrderRequestNotification notification =
                new WorkOrderRequestNotification(eventId, soNumber, warehouseCode, payload, receivedAt);
        if (lines != null) {
            lines.forEach(notification::attachLine);
        }
        return notification;
    }

    private void attachLine(WorkOrderRequestNotificationLine line) {
        line.assignTo(this);
        this.lines.add(line);
    }

    /** 같은 sku 라인에 qty 만큼 작업지시(생성) 반영(FIFO). 실제 소진한 수량 반환. */
    public int applyOrder(String sku, int qty) {
        int remaining = qty;
        for (WorkOrderRequestNotificationLine line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (line.getSku().equals(sku)) {
                remaining -= line.applyOrder(remaining);
            }
        }
        recomputeStatus();
        return qty - remaining;
    }

    /** 같은 sku 라인의 생산중 수량을 qty 만큼 해제(작업지시 취소, FIFO). 실제 해제한 수량 반환. */
    public int releaseOrder(String sku, int qty) {
        int remaining = qty;
        for (WorkOrderRequestNotificationLine line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (line.getSku().equals(sku)) {
                remaining -= line.releaseOrder(remaining);
            }
        }
        recomputeStatus();
        return qty - remaining;
    }

    public int applyFulfillment(String sku, int qty) {
        int remaining = qty;
        for (WorkOrderRequestNotificationLine line : lines) {
            if (remaining <= 0) {
                break;
            }
            if (line.getSku().equals(sku)) {
                remaining -= line.applyFulfillment(remaining);
            }
        }
        recomputeStatus();
        return qty - remaining;
    }

    public void recomputeStatus() {
        boolean allDone = lines.stream().allMatch(l -> l.getStatus() == WorkOrderRequestStatus.DONE);
        // 생산중(ordered)·완료(fulfilled) 어느 쪽이든 진행이 있으면 PARTIAL — 전량 지시됐지만 완료 전인 주문도
        // 목록(PENDING/PARTIAL)에 남아 "이미 작업지시함, 생산 대기 중"으로 보이게 한다(중복 지시 방지).
        boolean anyProgress = lines.stream().anyMatch(l -> l.getFulfilledQty() > 0 || l.getOrderedQty() > 0);
        if (allDone && !lines.isEmpty()) {
            this.status = WorkOrderRequestStatus.DONE;
        } else if (anyProgress) {
            this.status = WorkOrderRequestStatus.PARTIAL;
        } else {
            this.status = WorkOrderRequestStatus.PENDING;
        }
    }

    /**
     * 이 요청을 담당자(userId)가 처리중으로 선점한다.
     * 다른 담당자가 이미 선점했고 그 클레임이 아직 유효(staleThreshold 이후)하면 거부한다.
     * (같은 담당자의 재선점, 또는 만료된 클레임의 takeover 는 허용)
     */
    public void claim(Long userId, String userName, LocalDateTime now, LocalDateTime staleThreshold) {
        boolean heldByOther = claimedBy != null
                && !claimedBy.equals(userId)
                && claimedAt != null
                && claimedAt.isAfter(staleThreshold);
        if (heldByOther) {
            throw new ApiException(ErrorCode.REQUEST_ALREADY_CLAIMED);
        }
        this.claimedBy = userId;
        this.claimedByName = userName;
        this.claimedAt = now;
    }

    /** 클레임 해제. 본인(claimedBy)만 해제할 수 있다. 미점유면 no-op. */
    public void releaseClaim(Long userId) {
        if (claimedBy == null) {
            return;
        }
        if (!Objects.equals(claimedBy, userId)) {
            throw new ApiException(ErrorCode.REQUEST_CLAIM_FORBIDDEN);
        }
        this.claimedBy = null;
        this.claimedByName = null;
        this.claimedAt = null;
    }
}
