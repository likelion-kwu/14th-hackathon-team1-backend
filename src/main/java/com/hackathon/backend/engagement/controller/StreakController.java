package com.hackathon.backend.engagement.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.engagement.dto.StreakResponse;
import com.hackathon.backend.engagement.service.StreakService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;

@Tag(name = "engagement", description = "연속 대화 기록(스트릭) API 입니다.")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class StreakController {

	private final StreakService streakService;

	@Operation(summary = "스트릭 조회",
			description = "연속 대화 일수를 조회합니다. 아직 기록이 없는 회원은 404 가 아니라 currentStreak 0 으로 응답합니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping("/{memberId}/streak")
	public ApiResponse<StreakResponse> findByMemberId(
			@Parameter(description = "회원 식별자입니다", required = true)
			@PathVariable @Positive Long memberId) {
		return ApiResponse.success(streakService.findStreak(memberId));
	}
}