package com.bbd.procurement.notification.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * PO 작성 알림 인박스 read-model (이슈 #79).
 * HQ_STAFF가 PO를 작성하면 HQ_MANAGER 전용으로 1건 생성된다.
 * Kafka 미사용 — PO 작성과 같은 서비스·DB라 로컬 INSERT로 완결(단일 트랜잭션 멱등).
 */
@Entity
@Table(name = "po_notification")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PoNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "target_role", nullable = false, length = 20)
    private String targetRole;

    @Column(name = "po_number", nullable = false, length = 20)
    private String poNumber;

    @Column(name = "message", nullable = false, length = 255)
    private String message;

    // "read"는 일부 DB에서 예약어 — 컬럼명은 명시하고 필드 접근으로 매핑.
    @Column(name = "read", nullable = false)
    private boolean read;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    private PoNotification(String targetRole, String poNumber, String message, Long createdBy) {
        this.targetRole = targetRole;
        this.poNumber = poNumber;
        this.message = message;
        this.createdBy = createdBy;
        this.read = false;
        this.createdAt = Instant.now();
    }

    /** HQ_MANAGER 전용 알림 생성. */
    public static PoNotification forManager(String poNumber, String message, Long createdBy) {
        return new PoNotification("HQ_MANAGER", poNumber, message, createdBy);
    }

    /** 읽음 처리 — 이미 read여도 no-op 효과(멱등). */
    public void markRead() {
        this.read = true;
    }
}
