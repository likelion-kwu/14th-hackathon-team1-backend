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

    // AI API 원본 응답 전체를 보관합니다. 원문 재처리가 목적이므로 DB가 JSON을
    // 정규화하지 않게 TEXT를 씁니다. H2의 JSON 컬럼은 문자열을 한 번 더 인코딩해
    // 테스트와 MySQL의 조회 형태가 달라집니다.
    @Column(columnDefinition = "TEXT")
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

    /**
     * 외부 모델 호출 직전에 작업을 처리 중으로 전환합니다.
     *
     * 모델과 스키마 버전은 결과를 해석하거나 재처리할 때 필요하므로 호출을 시작한
     * 시점에 함께 고정합니다. 이미 종료된 작업을 다시 실행하려면 새 작업을 만들어
     * 추적해야 하므로 여기서 상태를 되돌리지는 않습니다.
     */
    public void startProcessing(String modelName, String schemaVersion) {
        requireStatus(AnalysisStatus.PENDING);
        this.status = AnalysisStatus.PROCESSING;
        this.modelName = modelName;
        this.schemaVersion = schemaVersion;
        this.errorMessage = null;
    }

    /** 모델 응답 원문을 보관하고 작업을 성공으로 종료합니다. */
    public void succeed(String rawResponse) {
        requireStatus(AnalysisStatus.PROCESSING);
        this.status = AnalysisStatus.SUCCESS;
        this.rawResponse = rawResponse;
        this.errorMessage = null;
    }

    /** 호출 또는 결과 검증 실패를 기록합니다. */
    public void fail(String rawResponse, String errorMessage) {
        if (status != AnalysisStatus.PENDING && status != AnalysisStatus.PROCESSING) {
            throw new IllegalStateException("종료된 분석 작업은 실패 상태로 변경할 수 없습니다.");
        }
        this.status = AnalysisStatus.FAILED;
        this.rawResponse = rawResponse;
        this.errorMessage = errorMessage;
    }

    /**
     * 모델 원문이 없는 인프라 단계 실패를 기록할 때 사용합니다.
     */
    public void fail(String errorMessage) {
        fail(null, errorMessage);
    }

    private void requireStatus(AnalysisStatus expected) {
        if (status != expected) {
            throw new IllegalStateException("분석 작업 상태가 " + expected + " 이어야 합니다.");
        }
    }
}
