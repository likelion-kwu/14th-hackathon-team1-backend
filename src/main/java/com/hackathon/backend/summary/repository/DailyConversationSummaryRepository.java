package com.hackathon.backend.summary.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.hackathon.backend.summary.entity.DailyConversationSummary;

/**
 * 일일 대화 요약 조회입니다.
 *
 * 쓰기는 AI 파이프라인이 담당하므로 여기서는 조회만 둡니다. (memberId, summaryDate)
 * 는 엔티티의 UNIQUE 제약이라 결과가 최대 1건이라 Optional 로 받습니다.
 */
public interface DailyConversationSummaryRepository extends JpaRepository<DailyConversationSummary, Long> {

	Optional<DailyConversationSummary> findByMemberIdAndSummaryDate(Long memberId, LocalDate summaryDate);

	/** 스케줄러의 주간 압축 입력을 날짜순으로 읽습니다. */
	List<DailyConversationSummary> findByMemberIdAndSummaryDateBetweenOrderBySummaryDateAsc(
			Long memberId, LocalDate start, LocalDate end);
}
