package com.hackathon.backend.summary.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hackathon.backend.summary.entity.DailyConversationSummary;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 일일 대화 요약 응답입니다.
 *
 * 엔티티를 직접 직렬화하지 않습니다. member 연관관계가 지연 로딩이라 그대로
 * 내보내면 open-in-view 가 꺼진 상태에서 직렬화 시점에 터집니다.
 *
 * @param memberId          회원 식별자입니다
 * @param summaryDate       요약 대상 날짜입니다
 * @param summary           요약 본문입니다
 * @param conversationCount 요약에 반영된 대화 수입니다
 * @param tokenCount        요약 생성에 사용된 토큰 수입니다
 * @param createdAt         생성 시각입니다
 * @param updatedAt         마지막 수정 시각입니다
 */
@Schema(description = "일일 대화 요약입니다.")
public record DailySummaryResponse(
		Long memberId,
		LocalDate summaryDate,
		String summary,
		int conversationCount,
		int tokenCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static DailySummaryResponse from(DailyConversationSummary entity) {
		return new DailySummaryResponse(
				entity.getMember().getId(),
				entity.getSummaryDate(),
				entity.getSummary(),
				entity.getConversationCount(),
				entity.getTokenCount(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}
}
