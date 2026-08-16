package com.hackathon.backend.ai.entity;

import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // conversation_id nullable — 대화 없이 주기적으로 생성되는 분석 작업도 존재
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TaskType taskType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.PENDING;

    @Column(length = 100)
    private String modelName;

    @Column(length = 20)
    private String schemaVersion;

    // AI API 원본 응답 전체를 JSON 문자열로 보관 (디버깅 / 재처리용)
    @Column(columnDefinition = "JSON")
    private String rawResponse;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum TaskType {
        HEALTH_EXTRACTION,   // 대화에서 건강 기록 추출
        DAILY_SUMMARY,       // 하루 대화 → 일일 요약
        WEEKLY_SUMMARY,      // 일일 요약 7개 → 주간 요약
        MONTHLY_SUMMARY,     // 주간 요약 4개 → 월간 요약
        OVERALL_REPORT       // 월간 요약 전체 → 종합 레포트
    }

    public enum AnalysisStatus {
        PENDING, PROCESSING, SUCCESS, FAILED
    }
}
