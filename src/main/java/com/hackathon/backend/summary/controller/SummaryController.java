package com.hackathon.backend.summary.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.common.response.ApiResponse;
import com.hackathon.backend.summary.dto.DailySummaryResponse;
import com.hackathon.backend.summary.dto.MonthlySummaryResponse;
import com.hackathon.backend.summary.dto.OverallReportResponse;
import com.hackathon.backend.summary.dto.WeeklySummaryResponse;
import com.hackathon.backend.summary.service.SummaryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

/**
 * 대화 요약·종합 리포트 조회 API 입니다.
 *
 * 전부 조회 전용입니다. 생성/수정/삭제는 없습니다 — 요약과 리포트는 AI 압축
 * 파이프라인이 씁니다.
 *
 * 파라미터 검증은 jakarta.validation 애노테이션만 붙입니다. @Validated 를 따로
 * 붙이지 않아도 Spring 내장 처리가 HandlerMethodValidationException 을 만들고
 * GlobalExceptionHandler 가 400 으로 변환합니다.
 */
@Tag(name = "summary", description = "대화 요약·종합 리포트 조회 API 입니다.")
@RestController
@RequestMapping("/api/summaries")
public class SummaryController {

	private final SummaryService summaryService;

	public SummaryController(SummaryService summaryService) {
		this.summaryService = summaryService;
	}

	@Operation(summary = "일일 대화 요약 조회", description = "date 를 생략하면 오늘(KST) 요약을 조회합니다. 없으면 404 입니다.")
	@GetMapping("/daily")
	public ApiResponse<DailySummaryResponse> findDaily(
			@Parameter(description = "회원 식별자", required = true)
			@RequestParam @Positive Long memberId,

			@Parameter(description = "요약 대상 날짜입니다. 생략하면 오늘(KST)입니다.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

		return ApiResponse.success(summaryService.findDaily(memberId, date));
	}

	@Operation(summary = "주간 대화 요약 조회", description = "periodStart 를 생략하면 이번 주 월요일(KST) 기준으로 조회합니다. 없으면 404 입니다.")
	@GetMapping("/weekly")
	public ApiResponse<WeeklySummaryResponse> findWeekly(
			@Parameter(description = "회원 식별자", required = true)
			@RequestParam @Positive Long memberId,

			@Parameter(description = "기간 시작일입니다. 생략하면 이번 주 월요일(KST)입니다.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {

		return ApiResponse.success(summaryService.findWeekly(memberId, periodStart));
	}

	@Operation(summary = "월간 대화 요약 조회", description = "periodStart 를 생략하면 이번 달 1일(KST) 기준으로 조회합니다. 없으면 404 입니다.")
	@GetMapping("/monthly")
	public ApiResponse<MonthlySummaryResponse> findMonthly(
			@Parameter(description = "회원 식별자", required = true)
			@RequestParam @Positive Long memberId,

			@Parameter(description = "기간 시작일입니다. 생략하면 이번 달 1일(KST)입니다.")
			@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate periodStart) {

		return ApiResponse.success(summaryService.findMonthly(memberId, periodStart));
	}

	@Operation(summary = "종합 리포트 조회", description = "회원당 1건입니다. 없으면 404 입니다.")
	@GetMapping("/overall")
	public ApiResponse<OverallReportResponse> findOverall(
			@Parameter(description = "회원 식별자", required = true)
			@RequestParam @Positive Long memberId) {

		return ApiResponse.success(summaryService.findOverall(memberId));
	}
}
