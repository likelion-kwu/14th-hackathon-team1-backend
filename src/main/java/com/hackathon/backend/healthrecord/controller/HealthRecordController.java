package com.hackathon.backend.healthrecord.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
 * 회의록 §7 "첫 번째 통합 목표"가 못박은 엔드포인트 하나만 둡니다. 조회
 * 범위를 오늘(KST)로 고정한 것도 같은 근거입니다. date 파라미터나 회원별
 * 상세 조회(GET /{id}) 는 팀 문서에 없어 추가하지 않습니다.
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
}
