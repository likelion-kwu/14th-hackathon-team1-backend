package com.hackathon.backend.summary.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.backend.summary.entity.MonthlyConversationSummary;

/**
 * 월간 대화 요약 조회입니다.
 *
 * 쓰기는 AI 파이프라인이 담당하므로 여기서는 조회만 둡니다. (memberId, periodStart)
 * 는 엔티티의 UNIQUE 제약이라 결과가 최대 1건이라 Optional 로 받습니다.
 */
public interface MonthlyConversationSummaryRepository extends JpaRepository<MonthlyConversationSummary, Long> {

	Optional<MonthlyConversationSummary> findByMemberIdAndPeriodStart(Long memberId, LocalDate periodStart);
}
