package com.hackathon.backend.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 메시지 전송 요청입니다.
 *
 * role 을 받지 않습니다. 이 API 로 들어오는 발화는 항상 사용자 것입니다.
 * 클라이언트가 role 을 정할 수 있으면 ASSISTANT 발화를 위조해 넣을 수 있고,
 * 그 내용이 그대로 요약과 건강 기록의 근거가 됩니다.
 *
 * @param content 사용자 발화 본문입니다
 */
@Schema(description = "메시지 전송 요청입니다.")
public record MessageSendRequest(

		@Schema(description = "사용자 발화 본문입니다.", example = "어제 잠을 잘 못 잤어요",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "메시지 내용은 필수입니다")
		@Size(max = 2000, message = "메시지는 2000자를 넘을 수 없습니다")
		String content) {
}
