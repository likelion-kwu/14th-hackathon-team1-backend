package com.hackathon.backend.member.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 회원 조회 응답입니다.
 *
 * 엔티티를 그대로 직렬화하지 않습니다. 지연 로딩 연관관계 문제도 있지만, 더 큰
 * 이유는 fcmToken 입니다. 그 값은 해당 기기로 푸시를 보낼 수 있는 자격증명이라
 * 응답에 실으면 안 됩니다. 등록 여부만 boolean 으로 알려줍니다.
 *
 * @param id                 회원 식별자입니다
 * @param nickname           표시 이름입니다
 * @param phone              전화번호입니다
 * @param notifyTime         매일 먼저 연락하는 시각입니다 (KST)
 * @param notifyEnabled      알림을 받을지 여부입니다
 * @param fcmTokenRegistered 푸시 토큰이 등록돼 있는지 여부입니다
 * @param createdAt          가입 시각입니다
 * @param updatedAt          마지막 수정 시각입니다
 */
@Schema(description = "회원 정보입니다.")
public record MemberResponse(

		@Schema(description = "회원 식별자입니다.", example = "1")
		Long id,

		@Schema(description = "표시 이름입니다.", example = "김할머니")
		String nickname,

		@Schema(description = "전화번호입니다.", example = "010-1234-5678")
		String phone,

		@Schema(description = "매일 먼저 연락하는 시각입니다 (KST).", example = "21:00:00")
		LocalTime notifyTime,

		@Schema(description = "알림을 받을지 여부입니다.", example = "true")
		boolean notifyEnabled,

		@Schema(description = "푸시 토큰이 등록돼 있는지 여부입니다. 토큰 값 자체는 응답에 실지 않습니다.", example = "false")
		boolean fcmTokenRegistered,

		@Schema(description = "가입 시각입니다.", example = "2026-08-19T21:00:00")
		LocalDateTime createdAt,

		@Schema(description = "마지막 수정 시각입니다.", example = "2026-08-19T21:00:00")
		LocalDateTime updatedAt) {
}
