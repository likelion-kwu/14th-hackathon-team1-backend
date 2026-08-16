package com.hackathon.backend.healthrecord.entity;

import com.hackathon.backend.conversation.entity.ConversationMessage;
import jakarta.persistence.*;
import lombok.*;

/**
 * health_record ↔ conversation_message 의 N:M 연결 테이블
 *
 * 하나의 건강 기록이 여러 메시지에서 근거를 가질 수 있고,
 * 하나의 메시지에서 여러 건강 기록이 추출될 수 있음
 */
@Entity
@Table(name = "health_record_source")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HealthRecordSource {

    @EmbeddedId
    private HealthRecordSourceId id;

    @MapsId("healthRecordId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "health_record_id")
    private HealthRecord healthRecord;

    @MapsId("conversationMessageId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_message_id")
    private ConversationMessage conversationMessage;
}
