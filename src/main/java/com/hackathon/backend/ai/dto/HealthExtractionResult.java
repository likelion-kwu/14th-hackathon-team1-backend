package com.hackathon.backend.ai.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;

/** health-extraction-v1 모델 출력입니다. */
public record HealthExtractionResult(String schemaVersion, List<HealthRecordCandidate> records) {

	public record HealthRecordCandidate(
			HealthType type,
			String summary,
			HealthDetail detail,
			LocalDate recordedDate,
			LocalDateTime recordedAt,
			BigDecimal confidence,
			String evidence) {
	}

	public sealed interface HealthDetail permits SleepDetail, MealDetail, ExerciseDetail, SkinDetail, MoodDetail,
			WaterDetail, OtherDetail {
	}

	public record SleepDetail(BigDecimal hours, String quality, LocalTime bedtime, LocalTime wakeTime) implements HealthDetail {
	}

	public record MealDetail(String menu, String timing, String amount) implements HealthDetail {
	}

	public record ExerciseDetail(String activity, BigDecimal duration, String unit, String intensity) implements HealthDetail {
	}

	public record SkinDetail(String condition, String area) implements HealthDetail {
	}

	public record MoodDetail(String emotion, String note) implements HealthDetail {
	}

	public record WaterDetail(BigDecimal amount, String unit) implements HealthDetail {
	}

	public record OtherDetail(String note) implements HealthDetail {
	}
}
