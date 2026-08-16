package com.hackathon.backend.summary.entity;

import com.hackathon.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 하루 대화 원문을 압축한 일일 요약
 *
 * conversation_message → (Compaction AI) → daily_conversation_summary
 * 날짜별 요약은 1개만 존재해야 하므로 UNIQUE (member_id, summary_date)
 */
@Entity
@Table(
        name = "daily_conversation_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_daily_member_date",
                columnNames = {"member_id", "summary_date"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class DailyConversationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDate summaryDate;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    @Builder.Default
    private int conversationCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private int tokenCount = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
