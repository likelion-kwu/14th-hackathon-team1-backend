package com.hackathon.backend.ai.controller;

import java.time.LocalDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.hackathon.backend.ai.dto.AiAnalysisResponse;
import com.hackathon.backend.ai.entity.AiAnalysis;
import com.hackathon.backend.common.response.ApiNotFound;
import com.hackathon.backend.common.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * AI 분석 작업 상태 API 명세입니다.
 *
 * 주의: 아직 구현이 없는 명세 전용 스텁입니다. 고정 예시를 반환합니다.
 *
 * 프론트에서 이 API 가 필요한 이유는 하나입니다. 대화를 끝낸 직후에는 건강
 * 기록이 아직 만들어지지 않았을 수 있는데, 건강 기록 조회가 빈 배열을 주면
 * "추출 중" 과 "추출했지만 건질 것이 없었음" 을 구분할 수 없습니다.
 */
@Tag(name = "ai", description = "AI 분석 작업 상태 조회 API 입니다. (구현 전 · 고정 예시 반환)")
@RestController
@RequestMapping("/api/ai-analyses")
public class AiAnalysisController {

	/** 스텁 응답입니다. 구현이 들어오면 지웁니다. */
	private static final AiAnalysisResponse SAMPLE = new AiAnalysisResponse(
			1L,
			1L,
			1L,
			AiAnalysis.TaskType.HEALTH_EXTRACTION,
			AiAnalysis.AnalysisStatus.SUCCESS,
			"claude-opus-5",
			"v1",
			LocalDateTime.parse("2026-08-19T21:12:00"),
			LocalDateTime.parse("2026-08-19T21:12:30"));

	@Operation(summary = "분석 작업 상태 조회",
			description = "대화와 작업 종류로 분석 상태를 조회합니다. status 가 SUCCESS 가 되면 결과 조회 API 에 데이터가 있습니다. "
					+ "아직 작업이 만들어지지 않았으면 404 입니다.")
	@ApiNotFound("해당 조건의 분석 작업이 아직 없습니다.")
	@GetMapping
	public ApiResponse<AiAnalysisResponse> findByConversation(
			@Parameter(description = "대화 식별자입니다", required = true)
			@RequestParam @Positive Long conversationId,

			@Parameter(description = "작업 종류입니다", required = true)
			@RequestParam @NotNull AiAnalysis.TaskType taskType) {

		return ApiResponse.success(SAMPLE);
	}
}
