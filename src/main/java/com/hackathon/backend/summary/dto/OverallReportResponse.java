package com.hackathon.backend.summary.dto;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.hackathon.backend.summary.entity.OverallReport;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 종합 리포트 응답입니다.
 *
 * @param memberId            회원 식별자입니다
 * @param summary             사용자에게 보여줄 전체 건강 보고서 본문입니다
 * @param detail              프론트엔드용 구조화 보고서 데이터입니다. JSON 객체로 나갑니다
 * @param monthlySummaryCount 이 리포트를 구성한 월간 요약 건수입니다
 * @param generatedAt         마지막으로 AI 보고서를 생성한 시각입니다. 최초 생성 전이면 null 입니다
 * @param createdAt           생성 시각입니다
 * @param updatedAt           마지막 수정 시각입니다
 */
@Schema(description = "종합 리포트입니다.")
public record OverallReportResponse(
		Long memberId,
		String summary,

		/*
		 * 엔티티에 String 으로 저장된 JSON 을 문자열이 아니라 JSON 객체로 내보냅니다.
		 * HealthRecordResponse.detail 과 같은 이유·같은 방식입니다: JsonNode 로
		 * 파싱해 담지 않는 것은 Boot 4 가 쓰는 Jackson 3 에서 databind 패키지 경로가
		 * 이동했기 때문이고, 애노테이션 패키지는 양쪽이 같아 이 방식이 버전 변화에
		 * 덜 민감합니다.
		 *
		 * 대신 저장된 문자열이 유효한 JSON 이 아니면 응답 전체가 깨집니다.
		 * 값을 쓰는 쪽(AI 파이프라인)이 저장 전에 검증해야 합니다.
		 */
		@JsonRawValue
		@Schema(type = "object", description = "프론트엔드용 구조화 보고서 데이터입니다")
		String detail,

		int monthlySummaryCount,
		LocalDateTime generatedAt,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	public static OverallReportResponse from(OverallReport entity) {
		return new OverallReportResponse(
				// getMember().getId() 가 아니라 getMemberId() 를 씁니다. @MapsId 라 PK 가
				// 곧 memberId 여서 값은 같지만, 굳이 지연 프록시를 경유할 이유가 없습니다.
				entity.getMemberId(),
				entity.getSummary(),
				normalizeJson(entity.getDetail()),
				entity.getMonthlySummaryCount(),
				entity.getGeneratedAt(),
				entity.getCreatedAt(),
				entity.getUpdatedAt());
	}

	/**
	 * 빈 문자열을 null 로 바꿉니다.
	 *
	 * JsonRawValue 는 값을 그대로 응답에 박아 넣으므로, 빈 문자열이 들어오면
	 * "detail": 뒤에 아무것도 없는 깨진 JSON 이 만들어집니다. null 은 정상적으로
	 * null 리터럴로 나갑니다.
	 */
	private static String normalizeJson(String raw) {
		return (raw == null || raw.isBlank()) ? null : raw;
	}
}
