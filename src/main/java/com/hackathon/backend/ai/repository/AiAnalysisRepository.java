package com.hackathon.backend.ai.repository;

import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.ai.entity.AiAnalysis.AnalysisStatus;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AiAnalysisRepository extends JpaRepository<AiAnalysis, Long> {

    // 특정 대화의 분석 결과 조회 (건강기록 추출 결과 확인용)
    Optional<AiAnalysis> findByConversationIdAndTaskType(Long conversationId, TaskType taskType);

    // 실패한 작업 재처리용 조회
    List<AiAnalysis> findByMemberIdAndStatusAndTaskType(
            Long memberId, AnalysisStatus status, TaskType taskType);
}
