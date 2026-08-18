package com.hackathon.backend.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 회원 가입 요청입니다.
 *
 * 알림 시각과 알림 여부는 받지 않습니다. 엔티티에 기본값(21:00, 활성)이 있고,
 * 가입 직후 바꾸고 싶으면 알림 설정 API 를 쓰면 됩니다. 가입 폼에 넣을 항목을
 * 늘리면 프론트가 첫 화면부터 물어봐야 할 것이 늘어납니다.
 *
 * @param nickname 표시 이름입니다
 * @param phone    전화번호입니다. 유니크 제약이 걸려 있습니다
 */
@Schema(description = "회원 가입 요청입니다.")
public record MemberCreateRequest(

		@Schema(description = "표시 이름입니다.", example = "김할머니", requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "닉네임은 필수입니다")
		@Size(max = 50, message = "닉네임은 50자를 넘을 수 없습니다")
		String nickname,

		/*
		 * 하이픈은 있어도 되고 없어도 되게 둡니다. 프론트 입력 폼이 자동으로 하이픈을
		 * 넣는 경우와 넣지 않는 경우가 모두 흔한데, 한쪽만 받으면 그 사실이 문서에
		 * 드러나지 않아 연동 첫날에 400 으로 막힙니다.
		 *
		 * 다만 저장 형태를 정규화하지는 않습니다. 유니크 제약이 문자열 그대로 걸리므로
		 * "01012345678" 과 "010-1234-5678" 은 서로 다른 값으로 취급됩니다. 정규화가
		 * 필요해지면 저장 직전에 하이픈을 제거하는 규칙을 팀에서 정해야 합니다.
		 */
		@Schema(description = "전화번호입니다. 하이픈은 있어도 없어도 됩니다.", example = "010-1234-5678",
				requiredMode = Schema.RequiredMode.REQUIRED)
		@NotBlank(message = "전화번호는 필수입니다")
		@Size(max = 20, message = "전화번호는 20자를 넘을 수 없습니다")
		@Pattern(regexp = "^01[016789]-?[0-9]{3,4}-?[0-9]{4}$", message = "전화번호 형식이 올바르지 않습니다")
		String phone) {
}
