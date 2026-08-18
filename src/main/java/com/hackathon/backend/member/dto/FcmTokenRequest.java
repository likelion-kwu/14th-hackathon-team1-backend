package com.hackathon.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * FCM 푸시 토큰 등록 요청입니다.
 *
 * 앱 재설치나 캐시 삭제로 토큰이 바뀌므로 프론트는 앱을 열 때마다 이 API 를
 * 호출해도 됩니다. 같은 값을 다시 보내도 결과가 같습니다.
 *
 * @param fcmToken 기기 푸시 토큰입니다
 */
@Schema(description = "FCM 푸시 토큰 등록 요청입니다.")
public record FcmTokenRequest(

		@Schema(description = "기기 푸시 토큰입니다.", example = "fMEr...:APA91bH...",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "FCM 토큰은 필수입니다")
		@Size(max = 255, message = "FCM 토큰은 255자를 넘을 수 없습니다")
		String fcmToken) {
}
