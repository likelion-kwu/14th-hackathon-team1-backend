package com.hackathon.backend.member.controller;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiConflict;
import com.hackathon.backend.common.response.ApiCreated;
import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.member.dto.FcmTokenRequest;
import com.hackathon.backend.member.dto.MemberCreateRequest;
import com.hackathon.backend.member.dto.MemberResponse;
import com.hackathon.backend.member.dto.NotificationSettingRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

/**
 * 회원 API 명세입니다.
 *
 * ⚠️ 아직 구현이 없는 명세 전용 스텁입니다. 고정 예시를 반환하고 DB 를 읽거나
 * 쓰지 않습니다. 프론트가 화면을 먼저 만들 수 있도록 요청·응답 형태만 확정해
 * 둔 것입니다.
 *
 * 구현은 이 패키지 담당자가 넣습니다. 그때 이 클래스에서 바꿀 것은 메서드 본문
 * 뿐이고, 경로·파라미터·응답 타입은 그대로 두어야 합니다. 프론트가 이미 그
 * 형태로 붙여 두었기 때문입니다.
 *
 * 인증은 두지 않습니다. 해커톤 기간에는 로그인을 붙이지 않기로 했으므로
 * memberId 를 경로 변수로 그대로 받습니다.
 */
@Tag(name = "member", description = "회원 가입·조회·알림 설정 API 입니다. (구현 전 · 고정 예시 반환)")
@RestController
@RequestMapping("/api/members")
public class MemberController {

	/** 스텁 응답입니다. 구현이 들어오면 지웁니다. */
	private static final MemberResponse SAMPLE = new MemberResponse(
			1L,
			"김할머니",
			"010-1234-5678",
			LocalTime.of(21, 0),
			true,
			false,
			LocalDateTime.parse("2026-08-19T21:00:00"),
			LocalDateTime.parse("2026-08-19T21:00:00"));

	@Operation(summary = "회원 가입",
			description = "닉네임과 전화번호로 회원을 등록합니다. 알림 시각은 21:00(KST), 알림 사용은 켜짐이 기본값입니다. "
					+ "응답의 id 를 보관했다가 이후 요청의 memberId 로 씁니다.")
	@ApiCreated
	@ApiConflict("이미 가입된 전화번호입니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<MemberResponse>> create(@Valid @RequestBody MemberCreateRequest request) {
		return ResponseEntity.created(URI.create("/api/members/" + SAMPLE.id()))
				.body(ApiResponse.success(SAMPLE));
	}

	@Operation(summary = "회원 조회", description = "회원 정보와 알림 설정을 조회합니다. FCM 토큰 값은 응답에 실리지 않습니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping("/{memberId}")
	public ApiResponse<MemberResponse> findById(
			@Parameter(description = "회원 식별자입니다", required = true)
			@PathVariable @Positive Long memberId) {

		return ApiResponse.success(SAMPLE);
	}

	@Operation(summary = "알림 설정 변경",
			description = "매일 먼저 연락할 시각과 알림 사용 여부를 함께 변경합니다. 부분 수정이 아니라 두 값을 모두 보냅니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@PatchMapping("/{memberId}/notification")
	public ApiResponse<MemberResponse> updateNotificationSetting(
			@Parameter(description = "회원 식별자입니다", required = true)
			@PathVariable @Positive Long memberId,

			@Valid @RequestBody NotificationSettingRequest request) {

		return ApiResponse.success(SAMPLE);
	}

	@Operation(summary = "FCM 토큰 등록",
			description = "기기 푸시 토큰을 등록하거나 교체합니다. 같은 값을 반복해서 보내도 안전하므로 앱을 열 때마다 호출해도 됩니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@PutMapping("/{memberId}/fcm-token")
	public ApiResponse<MemberResponse> updateFcmToken(
			@Parameter(description = "회원 식별자입니다", required = true)
			@PathVariable @Positive Long memberId,

			@Valid @RequestBody FcmTokenRequest request) {

		return ApiResponse.success(SAMPLE);
	}
}
