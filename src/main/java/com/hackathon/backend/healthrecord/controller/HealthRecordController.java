package com.hackathon.backend.healthrecord.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.healthrecord.dto.HealthRecordResponse;
import com.hackathon.backend.healthrecord.service.HealthRecordService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

/**
 * 건강 기록 조회 API 입니다.
 *
 * 클래스에 @Validated 를 붙이지 않습니다. Spring MVC 가 6.1 부터 컨트롤러
 * 파라미터의 jakarta.validation 제약을 자체 처리하고
 * (HandlerMethodValidationException), 공통 예외 핸들러가 이미 그 경로를
 * 다루고 있습니다.
 */
@Tag(name = "health-record", description = "건강 기록 조회 API 입니다.")
@RestController
@RequestMapping("/api/health-records")
public class HealthRecordController {

	private final HealthRecordService healthRecordService;

	public HealthRecordController(HealthRecordService healthRecordService) {
		this.healthRecordService = healthRecordService;
	}

	/**
	 * 회원의 오늘(KST) 건강 기록을 있는 그대로 배열로 반환합니다.
	 * 같은 타입이 여러 건이어도 대표 한 건만 고르지 않고 전부 반환합니다.
	 * 기록이 없으면 빈 배열과 200 을 반환합니다.
	 */
	@Operation(summary = "오늘의 건강 기록 조회", description = "회원의 오늘(KST) 건강 기록을 전부 조회합니다. 기록이 없으면 빈 배열을 반환합니다.")
	@GetMapping("/today")
	public ApiResponse<List<HealthRecordResponse>> findToday(
			@Parameter(description = "회원 식별자입니다", required = true)
			@RequestParam @Positive Long memberId) {
		return ApiResponse.success(healthRecordService.findToday(memberId));
	}

	/**
	 * 양쪽 끝을 포함하는 기간으로 조회합니다. 기록이 없으면 빈 배열과 200 을 반환합니다.
	 */
	@Operation(summary = "기간 건강 기록 조회",
			description = "양쪽 끝을 포함하는 기간으로 조회합니다. to 를 생략하면 오늘(KST), from 을 생략하면 to 에서 6일 전입니다. "
					+ "즉 둘 다 생략하면 최근 7일입니다.")
	@GetMapping
	public ApiResponse<List<HealthRecordResponse>> findRange(
			@Parameter(description = "회원 식별자입니다", required = true)
			@RequestParam @Positive Long memberId,

			@Parameter(description = "조회 시작일입니다 (포함). 생략하면 to 에서 6일 전입니다.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,

			@Parameter(description = "조회 종료일입니다 (포함). 생략하면 오늘(KST)입니다.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

		return ApiResponse.success(healthRecordService.findRange(memberId, from, to));
	}

	/**
	 * status 를 CONFIRMED 로 올립니다. 이미 확인한 기록에 다시 와도 성공합니다 —
	 * 확인 버튼이 두 번 눌리는 것은 정상적인 흐름입니다.
	 */
	@Operation(summary = "건강 기록 확인",
			description = "AI 가 추출한 기록을 사용자가 확인 처리합니다. status 가 CONFIRMED 로 바뀝니다. "
					+ "이미 확인한 기록에 다시 보내도 성공합니다.")
	@ApiNotFound("해당 건강 기록이 없습니다.")
	@PatchMapping("/{healthRecordId}/confirm")
	public ApiResponse<HealthRecordResponse> confirm(
			@Parameter(description = "건강 기록 식별자입니다", required = true)
			@PathVariable @Positive Long healthRecordId) {

		return ApiResponse.success(healthRecordService.confirm(healthRecordId));
	}
}
