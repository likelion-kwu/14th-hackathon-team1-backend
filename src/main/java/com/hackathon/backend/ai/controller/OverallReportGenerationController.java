package com.hackathon.backend.ai.controller;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.ai.dto.AiAnalysisResponse;
import com.hackathon.backend.ai.service.AiSummaryGenerationService;
import com.hackathon.backend.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Positive;

/** 종합 리포트는 스케줄러가 아니라 사용자 요청으로 생성하거나 갱신합니다. */
@Tag(name = "ai", description = "AI 생성 작업 API입니다.")
@RestController
@RequestMapping("/api/ai-analyses")
public class OverallReportGenerationController {

	private final AiSummaryGenerationService summaryGenerationService;

	public OverallReportGenerationController(AiSummaryGenerationService summaryGenerationService) {
		this.summaryGenerationService = summaryGenerationService;
	}

	@Operation(summary = "종합 리포트 생성 또는 갱신", description = "월간 요약 또는 건강 기록이 있을 때 즉시 AI 작업을 실행합니다.")
	@PostMapping("/overall-report")
	public ApiResponse<AiAnalysisResponse> generateOverall(
			@Parameter(description = "회원 식별자", required = true)
			@RequestParam @Positive Long memberId) {
		return ApiResponse.success(summaryGenerationService.generateOverall(memberId));
	}
}
