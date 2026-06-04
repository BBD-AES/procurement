package com.bbd.procurement.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@MappedSuperclass // JPA: 해당 클래스를 상속받는 자식 엔티티들에게 아래 필드들을 데이터베이스 컬럼으로 내려줍 (자체 테이블은 안 만들어짐)
@EntityListeners(AuditingEntityListener.class) // 엔티티의 저장/수정 이벤트를 감시하여 시간을 자동으로 입력
public class BaseTimeEntity {

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
