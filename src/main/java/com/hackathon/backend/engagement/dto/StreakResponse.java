package com.hackathon.backend.engagement.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 연속 대화 기록(스트릭) 응답입니다.
 *
 * @param memberId      회원 식별자입니다
 * @param currentStreak 현재 연속 일수입니다
 * @param longestStreak 최고 기록입니다
 * @param lastActiveDate 마지막으로 대화한 날짜입니다. 한 번도 없으면 null 입니다
 * @param updatedAt     마지막 갱신 시각입니다. 스트릭 기록이 없으면 null 입니다
 */
@Schema(description = "연속 대화 기록입니다.")
public record StreakResponse(

		@Schema(description = "회원 식별자입니다.", example = "1")
		Long memberId,

		@Schema(description = "현재 연속 일수입니다.", example = "3")
		int currentStreak,

		@Schema(description = "최고 기록입니다.", example = "12")
		int longestStreak,

		@Schema(description = "마지막으로 대화한 날짜입니다. 한 번도 없으면 null 입니다.", example = "2026-08-19",
				nullable = true)
		LocalDate lastActiveDate,

		@Schema(description = "마지막 갱신 시각입니다. 스트릭 기록이 아직 없으면 null 입니다.",
				example = "2026-08-19T21:10:00", nullable = true)
		LocalDateTime updatedAt) {
}
