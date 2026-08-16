package com.hackathon.backend.healthrecord.entity;

import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.conversation.entity.Conversation;
import com.hackathon.backend.member.entity.Member;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "health_record")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HealthRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    // 대화 삭제 시 NULL 처리 (출처 추적용이므로 삭제돼도 기록은 보존)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id")
    private Conversation conversation;

    // 이 기록을 생성한 AI Analysis 추적
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "analysis_id")
    private AiAnalysis analysis;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private HealthType type;

    // AI 추출 한 줄 요약 (예: "어젯밤 7시간 수면")
    @Column(nullable = false, length = 500)
    private String summary;

    // 타입별 구조화 정보
    // SLEEP   → {"hours": 7.5, "quality": "good", "bedtime": "23:00"}
    // MEAL    → {"menu": "삼각김밥, 커피", "timing": "아침"}
    // EXERCISE → {"activity": "걷기", "duration": 30, "unit": "min"}
    // SKIN    → {"condition": "건조함", "area": "볼"}
    @Column(columnDefinition = "JSON")
    private String detail;

    @Column(nullable = false)
    private LocalDate recordedDate;

    // 정확한 발생 시간이 있는 경우에만 기록
    private LocalDateTime recordedAt;

    // AI 추출 신뢰도 (0.0000 ~ 1.0000)
    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    // 추출 근거가 된 대화 원문 스니펫
    @Column(columnDefinition = "TEXT")
    private String evidence;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    @Builder.Default
    private HealthStatus status = HealthStatus.EXTRACTED;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum HealthType {
        SLEEP, MEAL, EXERCISE, SKIN, MOOD, WATER, OTHER
    }

    public enum HealthStatus {
        EXTRACTED,   // AI 추출 직후 (미검증)
        CONFIRMED,   // 사용자 확인 완료
        CORRECTED    // 사용자가 수정
    }
}
