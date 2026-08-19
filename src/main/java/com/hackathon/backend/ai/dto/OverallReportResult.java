package com.hackathon.backend.ai.dto;

import java.time.LocalDate;
import java.util.List;

import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;

/** overall-report-v1 모델 출력입니다. */
public record OverallReportResult(String schemaVersion, String summary, Detail detail) {

	public record Detail(
			Period period,
			List<String> highlights,
			List<HealthTrend> healthTrends,
			List<String> recommendations) {
	}

	public record Period(LocalDate from, LocalDate to) {
	}

	public record HealthTrend(HealthType type, Trend trend, String note) {
	}

	public enum Trend {
		IMPROVING, STABLE, WORSENING, UNCLEAR
	}
}
