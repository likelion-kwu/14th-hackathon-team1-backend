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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

@Tag(name = "engagement", description = "연속 대화 기록 API 입니다.")
@RestController
@RequestMapping("/api/members")
public class StreakController {

	private final StreakService streakService;

	public StreakController(StreakService streakService) {
		this.streakService = streakService;
	}

	@Operation(summary = "스트릭 조회",
			description = "연속 대화 일수를 조회합니다. 가입 직후처럼 스트릭 기록이 없을 때도 404가 아니라 currentStreak와 longestStreak가 0인 응답을 반환합니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping("/{memberId}/streak")
	public ApiResponse<StreakResponse> findByMemberId(@PathVariable @Positive Long memberId) {
		return ApiResponse.success(streakService.findStreak(memberId));
	}
}
