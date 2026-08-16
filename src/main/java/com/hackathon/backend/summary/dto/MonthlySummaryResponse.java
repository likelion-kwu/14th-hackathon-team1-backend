package com.hackathon.backend.summary.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hackathon.backend.summary.entity.MonthlyConversationSummary;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 월간 대화 요약 응답입니다.
 *
 * @param memberId           회원 식별자입니다
 * @param periodStart        기간 시작일입니다
 * @param periodEnd          기간 종료일입니다
 * @param summary            요약 본문입니다
 * @param weeklySummaryCount 이 요약을 구성한 주간 요약 건수입니다
 * @param tokenCount         요약 생성에 사용된 토큰 수입니다
 * @param createdAt          생성 시각입니다
 * @param updatedAt          마지막 수정 시각입니다
 */
@Schema(description = "월간 대화 요약입니다.")
public record MonthlySummaryResponse(
		Long memberId,
		LocalDate periodStart,
		LocalDate periodEnd,
		String summary,
		int weeklySummaryCount,
		int tokenCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static MonthlySummaryResponse from(MonthlyConversationSummary entity) {
		return new MonthlySummaryResponse(
				entity.getMember().getId(),
				entity.getPeriodStart(),
				entity.getPeriodEnd(),
				entity.getSummary(),
				entity.getWeeklySummaryCount(),
				entity.getTokenCount(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
