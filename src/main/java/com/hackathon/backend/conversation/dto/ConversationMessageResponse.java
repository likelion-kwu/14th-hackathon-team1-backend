package com.hackathon.backend.conversation.dto;

import java.time.LocalDateTime;

import com.hackathon.backend.conversation.entity.ConversationMessage;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 대화 메시지 응답입니다.
 *
 * @param id             메시지 식별자입니다
 * @param conversationId 소속 대화 식별자입니다
 * @param role           발화 주체입니다 (USER, ASSISTANT, SYSTEM)
 * @param content        발화 본문입니다
 * @param sequenceNo     대화 안에서의 순서입니다. 1 부터 시작합니다
 * @param tokenCount     이 메시지의 토큰 수입니다
 * @param createdAt      생성 시각입니다
 */
@Schema(description = "대화 메시지입니다.")
public record ConversationMessageResponse(

		@Schema(description = "메시지 식별자입니다.", example = "10")
		Long id,

		@Schema(description = "소속 대화 식별자입니다.", example = "1")
		Long conversationId,

		@Schema(description = "발화 주체입니다. SYSTEM 은 프롬프트용이라 화면에 그리지 않습니다.", example = "USER")
		ConversationMessage.MessageRole role,

		@Schema(description = "발화 본문입니다.", example = "어제 잠을 잘 못 잤어요")
		String content,

		@Schema(description = "대화 안에서의 순서입니다. 1 부터 시작합니다.", example = "1")
		int sequenceNo,

		/*
		 * AI 연동 전에는 0 입니다. 목 구현이 토큰을 세지 않기 때문입니다.
		 * 프론트가 이 값으로 무언가를 계산하면 연동 시점에 동작이 바뀝니다.
		 */
		@Schema(description = "이 메시지의 토큰 수입니다. AI 연동 전에는 항상 0 입니다.", example = "0")
		int tokenCount,

		@Schema(description = "생성 시각입니다.", example = "2026-08-19T21:00:05")
		LocalDateTime createdAt) {
}
