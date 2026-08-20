package com.hackathon.backend.member.controller;

import java.net.URI;

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
import com.hackathon.backend.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;

@Tag(name = "member", description = "회원 가입·조회·알림 설정 API 입니다.")
@RestController
@RequestMapping("/api/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@Operation(summary = "회원 가입",
			description = "닉네임과 전화번호로 회원을 등록합니다. 가입 직후 알림 시각은 21:00(KST), 알림 사용은 true이며, "
					+ "응답의 id를 이후 memberId로 사용합니다.")
	@ApiCreated
	@ApiConflict("이미 가입된 전화번호입니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<MemberResponse>> create(@Valid @RequestBody MemberCreateRequest request) {
		MemberResponse response = memberService.create(request);
		return ResponseEntity.created(URI.create("/api/members/" + response.id()))
				.body(ApiResponse.success(response));
	}

	@Operation(summary = "회원 조회", description = "회원 정보와 알림 설정을 조회합니다. FCM 토큰 값은 응답에 포함하지 않습니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping("/{memberId}")
	public ApiResponse<MemberResponse> findById(
			@Parameter(required = true) @PathVariable @Positive Long memberId) {
		return ApiResponse.success(memberService.findById(memberId));
	}

	@Operation(summary = "알림 설정 변경", description = "알림 시각과 알림 사용 여부를 함께 변경합니다. 부분 수정이 아니므로 두 값을 모두 보내야 합니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@PatchMapping("/{memberId}/notification")
	public ApiResponse<MemberResponse> updateNotificationSetting(
			@Parameter(required = true) @PathVariable @Positive Long memberId,
			@Valid @RequestBody NotificationSettingRequest request) {
		return ApiResponse.success(memberService.updateNotificationSetting(memberId, request));
	}

	@Operation(summary = "FCM 토큰 등록", description = "기기 푸시 토큰을 등록하거나 교체합니다. 같은 값을 반복해서 보내도 안전합니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@PutMapping("/{memberId}/fcm-token")
	public ApiResponse<MemberResponse> updateFcmToken(
			@Parameter(required = true) @PathVariable @Positive Long memberId,
			@Valid @RequestBody FcmTokenRequest request) {
		return ApiResponse.success(memberService.updateFcmToken(memberId, request));
	}
}
