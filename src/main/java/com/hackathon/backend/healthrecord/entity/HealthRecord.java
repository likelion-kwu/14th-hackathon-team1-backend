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
    //
    // 컬럼 타입을 JSON 이 아니라 TEXT 로 둡니다. 이유는 두 가지입니다.
    //
    // 첫째, 테스트(H2)에서 저장 원문과 조회 값이 달라집니다. H2 는 문자열을 JSON 컬럼에
    // 넣을 때 "JSON 문서 원문"이 아니라 "JSON 으로 인코딩할 스칼라 값"으로 취급해
    // {"bedtime":"03:00"} 이 "{\"bedtime\":\"03:00\"}" 으로 한 겹 더 감싸집니다
    // (HealthRecordRepositoryTest 로 재현). 그대로 응답에 실리면 프론트가 JSON.parse 를
    // 두 번 해야 합니다.
    //   주의: 이건 H2 동작입니다. MySQL 8 은 넣은 문자열을 JSON 텍스트로 파싱해 저장하고
    //   정규화된 텍스트로 돌려주므로 이 현상이 없습니다. "JSON 컬럼은 어디서나 깨진다" 로
    //   읽지 마십시오.
    //
    // 둘째, 그럼에도 TEXT 로 가는 이유는 이 값을 저장하고 그대로 꺼내 쓰기만 할 뿐
    // JSON_EXTRACT 같은 DB 내부 질의를 하지 않기 때문입니다. JSON 타입으로 얻는 것이
    // 없는데 테스트와 운영의 저장 형태만 갈립니다. TEXT 면 양쪽이 같습니다.
    //
    // 저장 시점에 잘못된 JSON 을 DB 가 걸러주길 원하게 되면 그때는 TEXT 가 아니라
    // @JdbcTypeCode(SqlTypes.JSON) 이 정공법입니다.
    @Column(columnDefinition = "TEXT")
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

    public void confirm() {
        this.status = HealthStatus.CONFIRMED;
    }

    public enum HealthType {
        SLEEP, MEAL, EXERCISE, SKIN, MOOD, WATER, OTHER
    }

    public enum HealthStatus {
        EXTRACTED,   // AI 추출 직후 (미검증)
        CONFIRMED,   // 사용자 확인 완료
        CORRECTED    // 사용자가 수정
    }
}
