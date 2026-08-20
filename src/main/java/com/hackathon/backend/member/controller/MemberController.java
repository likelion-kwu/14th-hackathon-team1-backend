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

	@Operation(summary = "회원 가입")
	@ApiCreated
	@ApiConflict("이미 가입된 전화번호입니다.")
	@PostMapping
	public ResponseEntity<ApiResponse<MemberResponse>> create(@Valid @RequestBody MemberCreateRequest request) {
		MemberResponse response = memberService.create(request);
		return ResponseEntity.created(URI.create("/api/members/" + response.id()))
				.body(ApiResponse.success(response));
	}

	@Operation(summary = "회원 조회")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping("/{memberId}")
	public ApiResponse<MemberResponse> findById(
			@Parameter(required = true) @PathVariable @Positive Long memberId) {
		return ApiResponse.success(memberService.findById(memberId));
	}

	@Operation(summary = "알림 설정 변경")
	@ApiNotFound("해당 회원이 없습니다.")
	@PatchMapping("/{memberId}/notification")
	public ApiResponse<MemberResponse> updateNotificationSetting(
			@Parameter(required = true) @PathVariable @Positive Long memberId,
			@Valid @RequestBody NotificationSettingRequest request) {
		return ApiResponse.success(memberService.updateNotificationSetting(memberId, request));
	}

	@Operation(summary = "FCM 토큰 등록")
	@ApiNotFound("해당 회원이 없습니다.")
	@PutMapping("/{memberId}/fcm-token")
	public ApiResponse<MemberResponse> updateFcmToken(
			@Parameter(required = true) @PathVariable @Positive Long memberId,
			@Valid @RequestBody FcmTokenRequest request) {
		return ApiResponse.success(memberService.updateFcmToken(memberId, request));
	}
}
