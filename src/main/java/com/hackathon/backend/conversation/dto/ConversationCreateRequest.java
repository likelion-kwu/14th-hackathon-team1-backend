package com.hackathon.backend.conversation.dto;

import com.hackathon.backend.conversation.entity.Conversation;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 대화 시작 요청입니다.
 *
 * sessionDate 를 받지 않습니다. 대화는 "지금" 시작하는 것이므로 서버가 오늘(KST)로
 * 정합니다. 클라이언트가 날짜를 정하게 하면 기기 시계가 틀어진 만큼 요약이
 * 엉뚱한 날짜에 묶입니다.
 *
 * status 도 받지 않습니다. 시작 요청이므로 IN_PROGRESS 로 고정입니다.
 *
 * @param memberId 회원 식별자입니다
 * @param type     대화 방식입니다
 */
@Schema(description = "대화 시작 요청입니다.")
public record ConversationCreateRequest(

		@Schema(description = "회원 식별자입니다.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "회원 식별자는 필수입니다")
		@Positive(message = "회원 식별자는 0보다 커야 합니다")
		Long memberId,

		@Schema(description = "대화 방식입니다. CALL 은 통화 연동이 붙기 전까지 사실상 CHAT 과 같게 동작합니다.",
				example = "CHAT", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "대화 방식은 필수입니다")
		Conversation.ConversationType type) {
}
