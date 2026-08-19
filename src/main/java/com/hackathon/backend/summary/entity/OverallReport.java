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
    //
    // HealthRecord.detail 과 같은 이유로 JSON 이 아니라 TEXT 입니다. 판단 근거는
    // 그쪽 주석에 정리해 두었습니다. 요약하면 (1) H2 에서 저장 원문과 조회 값이
    // 달라지고(SummaryRepositoryTest 로 재현), (2) 이 값은 저장·조회만 할 뿐
    // DB 내부 질의를 하지 않아 JSON 타입으로 얻는 것이 없기 때문입니다.
    @Column(columnDefinition = "TEXT")
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

    /** 회원당 하나인 리포트를 온디맨드 재생성 결과로 갱신합니다. */
    public void refresh(String summary, String detail, int monthlySummaryCount, LocalDateTime generatedAt) {
        this.summary = summary;
        this.detail = detail;
        this.monthlySummaryCount = monthlySummaryCount;
        this.generatedAt = generatedAt;
    }
}
