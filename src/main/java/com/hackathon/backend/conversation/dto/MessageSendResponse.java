package com.hackathon.backend.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 메시지 전송 응답입니다.
 *
 * 저장된 사용자 메시지와 AI 응답을 함께 돌려줍니다. AI 응답만 주면 프론트가
 * 방금 보낸 사용자 메시지의 id 와 sequenceNo 를 알 수 없어, 화면에 낙관적으로
 * 그려둔 말풍선을 서버 데이터와 맞출 수 없습니다.
 *
 * @param userMessage      저장된 사용자 메시지입니다
 * @param assistantMessage AI 응답 메시지입니다
 */
@Schema(description = "메시지 전송 결과입니다. 사용자 메시지와 AI 응답이 함께 저장됩니다.")
public record MessageSendResponse(

		@Schema(description = "저장된 사용자 메시지입니다.")
		ConversationMessageResponse userMessage,

		@Schema(description = "AI 응답 메시지입니다. AI 연동 전에는 목 응답입니다.")
		ConversationMessageResponse assistantMessage) {
}
