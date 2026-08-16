package com.hackathon.backend.summary.entity;

import com.hackathon.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 주간 요약 4개를 압축한 월간 요약
 *
 * weekly_conversation_summary × 4 → (Compaction AI) → monthly_conversation_summary
 * period_start = 해당 월 1일, period_end = 해당 월 말일
 * UNIQUE (member_id, period_start)
 */
@Entity
@Table(
        name = "monthly_conversation_summary",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_monthly_member_period",
                columnNames = {"member_id", "period_start"}
        )
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MonthlyConversationSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(nullable = false)
    private LocalDate periodStart;

    @Column(nullable = false)
    private LocalDate periodEnd;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    @Builder.Default
    private int weeklySummaryCount = 4;

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
