package com.hackathon.backend.conversation.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_message",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_conversation_sequence",
				columnNames = {"conversation_id", "sequence_no"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int sequenceNo;

    @Column(nullable = false)
    @Builder.Default
    private int tokenCount = 0;

    /**
     * 클라이언트가 재전송 중복 방지를 위해 제공하는 UUID입니다.
     * 프론트가 Idempotency-Key 헤더로 넘기면 이 값에 저장됩니다.
     * null 허용(헤더를 보내지 않는 요청), DB 레벨 unique 제약으로 중복 저장을 방지합니다.
     */
    @Column(unique = true)
    private String clientMessageId;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum MessageRole {
        USER, ASSISTANT, SYSTEM
    }
}
