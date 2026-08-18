package com.hackathon.backend.member.dto;

import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * 알림 설정 변경 요청입니다.
 *
 * 두 값을 함께 받습니다. 부분 수정으로 만들면 "시각만 바꾸기" 를 표현하기 위해
 * null 을 "변경 안 함" 으로 해석해야 하는데, 그러면 프론트가 값을 비우는 것과
 * 건드리지 않는 것을 구분할 수 없게 됩니다. 알림 설정 화면은 두 값이 한 화면에
 * 같이 있으므로 함께 보내는 것이 자연스럽습니다.
 *
 * notifyEnabled 를 boolean 이 아니라 Boolean 으로 받습니다. 원시 타입이면 필드를
 * 아예 빠뜨린 요청이 false 로 조용히 해석되어 알림이 꺼집니다. Boolean 이어야
 * null 이 되고 @NotNull 이 400 으로 잡아냅니다.
 *
 * @param notifyTime    매일 먼저 연락할 시각입니다 (KST)
 * @param notifyEnabled 알림을 받을지 여부입니다
 */
@Schema(description = "알림 설정 변경 요청입니다.")
public record NotificationSettingRequest(

		@Schema(description = "매일 먼저 연락할 시각입니다 (KST). 초는 생략할 수 있습니다.", example = "21:00",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "알림 시각은 필수입니다")
		LocalTime notifyTime,

		@Schema(description = "알림을 받을지 여부입니다.", example = "true",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotNull(message = "알림 사용 여부는 필수입니다")
		Boolean notifyEnabled) {
}
