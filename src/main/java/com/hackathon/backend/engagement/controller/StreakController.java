package com.hackathon.backend.engagement.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.engagement.dto.StreakResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

/**
 * 연속 대화 기록(스트릭) API 명세입니다.
 *
 * ⚠️ 아직 구현이 없는 명세 전용 스텁입니다. 고정 예시를 반환합니다.
 *
 * 경로가 /api/members 아래인데 컨트롤러가 MemberController 와 분리돼 있습니다.
 * 스트릭은 회원 정보가 아니라 참여 유도(engagement) 기능이고 그쪽에 스케줄러와
 * 푸시가 함께 붙을 예정이라 패키지를 나눴습니다. Spring 은 같은 prefix 를 쓰는
 * 컨트롤러가 여럿이어도 하위 경로가 겹치지 않으면 정상 매핑합니다.
 */
@Tag(name = "engagement", description = "연속 대화 기록(스트릭) API 입니다. (구현 전 · 고정 예시 반환)")
@RestController
@RequestMapping("/api/members")
public class StreakController {

	/** 스텁 응답입니다. 구현이 들어오면 지웁니다. */
	private static final StreakResponse SAMPLE = new StreakResponse(
			1L, 3, 12, LocalDate.parse("2026-08-19"), LocalDateTime.parse("2026-08-19T21:10:00"));

	@Operation(summary = "스트릭 조회",
			description = "연속 대화 일수를 조회합니다. 아직 기록이 없는 회원은 404 가 아니라 currentStreak 0 으로 응답합니다. "
					+ "가입 직후 화면이 오류 분기를 타지 않게 하기 위해서입니다.")
	@ApiNotFound("해당 회원이 없습니다.")
	@GetMapping("/{memberId}/streak")
	public ApiResponse<StreakResponse> findByMemberId(
			@Parameter(description = "회원 식별자입니다", required = true)
			@PathVariable @Positive Long memberId) {

		return ApiResponse.success(SAMPLE);
	}
}
