package com.hackathon.backend.healthrecord.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.hackathon.backend.healthrecord.entity.HealthRecord;

/**
 * 건강 기록 조회 응답입니다.
 *
 * 엔티티를 그대로 직렬화하지 않습니다. member, conversation, analysis 는
 * 지연 로딩 연관관계라 그대로 내보내면 open-in-view 를 끈 상태에서 직렬화
 * 시점에 LazyInitializationException 이 터집니다.
 *
 * 필드는 엔티티를 그대로 미러링합니다. 회의록 §5 가 요구한 "어떤 대화를
 * 근거로 생성됐는지 추적 가능"이라는 요구를 만족하는 최소치가 evidence 와
 * conversationId 이므로, 그 이상(발화 원문 조인 등)은 담지 않습니다.
 *
 * @param id            건강 기록 식별자입니다
 * @param type          기록 유형입니다 (SLEEP, MEAL, EXERCISE, SKIN, MOOD, WATER, OTHER)
 * @param summary       AI 가 추출한 한 줄 요약입니다
 * @param detail        타입별 구조화 정보입니다. JSON 원문을 그대로 내보냅니다
 * @param recordedDate  기록 날짜입니다
 * @param recordedAt    정확한 발생 시각입니다. 없으면 null 입니다
 * @param confidence    AI 추출 신뢰도입니다 (0.0000 ~ 1.0000)
 * @param evidence      추출 근거가 된 대화 원문 스니펫입니다
 * @param status        기록 상태입니다 (EXTRACTED, CONFIRMED, CORRECTED)
 * @param conversationId 근거가 된 대화의 식별자입니다. 대화가 삭제됐거나 없으면 null 입니다
 */
public record HealthRecordResponse(
		Long id,
		HealthRecord.HealthType type,
		String summary,
		@JsonRawValue String detail,
		LocalDate recordedDate,
		LocalDateTime recordedAt,
		BigDecimal confidence,
		String evidence,
		HealthRecord.HealthStatus status,
		Long conversationId) {

	public static HealthRecordResponse from(HealthRecord record) {
		return new HealthRecordResponse(
				record.getId(),
				record.getType(),
				record.getSummary(),
				normalizeDetail(record.getDetail()),
				record.getRecordedDate(),
				record.getRecordedAt(),
				record.getConfidence(),
				record.getEvidence(),
				record.getStatus(),
				record.getConversation() != null ? record.getConversation().getId() : null);
	}

	/**
	 * detail 은 엔티티에 String 으로 저장된 JSON 입니다. @JsonRawValue 없이 그대로
	 * 내보내면 이스케이프되어 프론트가 두 번 파싱해야 합니다. 값이 없거나 공백이면
	 * null 로 정규화합니다. 빈 문자열을 그대로 @JsonRawValue 로 내보내면 깨진 JSON이
	 * 응답에 섞입니다.
	 */
	private static String normalizeDetail(String detail) {
		return (detail == null || detail.isBlank()) ? null : detail;
	}
}
