package com.hackathon.backend.conversation.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.hackathon.backend.conversation.entity.Conversation;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대화 조회 응답입니다.
 *
 * 메시지는 담지 않습니다. 목록 조회에서 대화마다 메시지를 함께 끌어오면
 * 대화 수만큼 쿼리가 나갑니다. 메시지가 필요하면 메시지 목록 API 를 씁니다.
 *
 * 같은 이유로 messageCount 도 넣지 않았습니다. 목록의 각 행마다 count 쿼리가
 * 나가기 때문입니다. 화면에 필요해지면 목록 쿼리를 join 으로 바꾸면서 함께
 * 넣어야 합니다.
 *
 * @param id          대화 식별자입니다
 * @param memberId    회원 식별자입니다
 * @param type        대화 방식입니다 (CALL, CHAT)
 * @param status      대화 상태입니다
 * @param sessionDate 대화가 속한 날짜입니다 (KST)
 * @param startedAt   시작 시각입니다. 시작 전이면 null 입니다
 * @param endedAt     종료 시각입니다. 종료 전이면 null 입니다
 * @param createdAt   생성 시각입니다
 * @param updatedAt   마지막 수정 시각입니다
 */
@Schema(description = "대화 정보입니다.")
public record ConversationResponse(

		@Schema(description = "대화 식별자입니다.", example = "1")
		Long id,

		@Schema(description = "회원 식별자입니다.", example = "1")
		Long memberId,

		@Schema(description = "대화 방식입니다.", example = "CHAT")
		Conversation.ConversationType type,

		@Schema(description = "대화 상태입니다. 사용자 요청으로 도달하는 값은 IN_PROGRESS 와 COMPLETED 이고, "
				+ "나머지는 스케줄러와 통화 연동이 정합니다.", example = "IN_PROGRESS")
		Conversation.ConversationStatus status,

		@Schema(description = "대화가 속한 날짜입니다 (KST).", example = "2026-08-19")
		LocalDate sessionDate,

		@Schema(description = "시작 시각입니다. 시작 전이면 null 입니다.", example = "2026-08-19T21:00:00",
				nullable = true)
		LocalDateTime startedAt,

		@Schema(description = "종료 시각입니다. 종료 전이면 null 입니다.", example = "2026-08-19T21:12:00",
				nullable = true)
		LocalDateTime endedAt,

		@Schema(description = "생성 시각입니다.", example = "2026-08-19T21:00:00")
		LocalDateTime createdAt,

		@Schema(description = "마지막 수정 시각입니다.", example = "2026-08-19T21:12:00")
		LocalDateTime updatedAt) {
}
