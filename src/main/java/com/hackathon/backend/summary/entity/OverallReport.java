package com.hackathon.backend.summary.entity;

import com.hackathon.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * 최종 종합 레포트 — member와 1:1 (실선, 식별 관계)
 *
 * monthly_conversation_summary 전체 → (AI) → overall_report
 * 회원 1인당 레포트는 1개이며, AI 재생성 시 덮어쓴다.
 * generated_at 으로 마지막 생성 시각을 추적.
 */
@Entity
@Table(name = "overall_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class OverallReport {

    @Id
    private Long memberId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    // 사용자에게 보여줄 전체 건강 보고서 본문
    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    // 프론트엔드용 구조화 보고서 데이터
    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(nullable = false)
    @Builder.Default
    private int monthlySummaryCount = 0;

    // 마지막으로 AI 보고서를 생성한 시각 (최초 생성 전은 null)
    private LocalDateTime generatedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
