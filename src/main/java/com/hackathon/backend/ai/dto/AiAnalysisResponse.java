package com.hackathon.backend.ai.dto;

import java.time.LocalDateTime;

import com.hackathon.backend.ai.entity.AiAnalysis;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * AI 분석 작업 상태 응답입니다.
 *
 * rawResponse 와 errorMessage 는 담지 않습니다. 전자는 모델 원문이라 크고
 * 프론트가 쓸 일이 없으며, 후자는 예외 원문이 그대로 들어가는 필드라 스택
 * 트레이스나 프롬프트 일부가 클라이언트로 새어 나갈 수 있습니다. 실패했다는
 * 사실은 status 로 충분히 전달됩니다.
 *
 * @param id             분석 작업 식별자입니다
 * @param memberId       회원 식별자입니다
 * @param conversationId 대상 대화 식별자입니다. 대화와 무관한 작업이면 null 입니다
 * @param taskType       작업 종류입니다
 * @param status         작업 상태입니다
 * @param modelName      사용한 모델 이름입니다. 아직 실행 전이면 null 입니다
 * @param schemaVersion  응답 스키마 버전입니다. 아직 실행 전이면 null 입니다
 * @param createdAt      생성 시각입니다
 * @param updatedAt      마지막 수정 시각입니다
 */
@Schema(description = "AI 분석 작업의 진행 상태입니다.")
public record AiAnalysisResponse(

		@Schema(description = "분석 작업 식별자입니다.", example = "1")
		Long id,

		@Schema(description = "회원 식별자입니다.", example = "1")
		Long memberId,

		@Schema(description = "대상 대화 식별자입니다. 대화와 무관한 작업이면 null 입니다.", example = "1",
				nullable = true)
		Long conversationId,

		@Schema(description = "작업 종류입니다.", example = "HEALTH_EXTRACTION")
		AiAnalysis.TaskType taskType,

		@Schema(description = "작업 상태입니다. SUCCESS 가 되어야 결과 조회 API 에 데이터가 있습니다.",
				example = "SUCCESS")
		AiAnalysis.AnalysisStatus status,

		@Schema(description = "사용한 모델 이름입니다. 아직 실행 전이면 null 입니다.", example = "claude-opus-5",
				nullable = true)
		String modelName,

		@Schema(description = "응답 스키마 버전입니다. 아직 실행 전이면 null 입니다.", example = "v1", nullable = true)
		String schemaVersion,

		@Schema(description = "생성 시각입니다.", example = "2026-08-19T21:12:00")
		LocalDateTime createdAt,

		@Schema(description = "마지막 수정 시각입니다.", example = "2026-08-19T21:12:30")
		LocalDateTime updatedAt) {

	public static AiAnalysisResponse from(AiAnalysis analysis) {
		return new AiAnalysisResponse(
				analysis.getId(),
				analysis.getMember().getId(),
				analysis.getConversation() == null ? null : analysis.getConversation().getId(),
				analysis.getTaskType(),
				analysis.getStatus(),
				analysis.getModelName(),
				analysis.getSchemaVersion(),
				analysis.getCreatedAt(),
				analysis.getUpdatedAt());
	}
}
