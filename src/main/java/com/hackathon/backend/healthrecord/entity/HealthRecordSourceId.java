package com.hackathon.backend.healthrecord.entity;

import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;

/**
 * health_record_source 복합 PK
 *
 * N:M 관계 (건강기록 ↔ 메시지) 의 복합 키
 * Serializable 구현 필수 — Hibernate 2차 캐시 직렬화 요건
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class HealthRecordSourceId implements Serializable {

    private Long healthRecordId;
    private Long conversationMessageId;
}
