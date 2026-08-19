package com.hackathon.backend.ai.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import com.hackathon.backend.ai.dto.ConversationSummaryResult;
import com.hackathon.backend.ai.dto.HealthExtractionResult;
import com.hackathon.backend.ai.dto.HealthExtractionResult.ExerciseDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.HealthDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.MealDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.MoodDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.OtherDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.SkinDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.SleepDetail;
import com.hackathon.backend.ai.dto.HealthExtractionResult.WaterDetail;
import com.hackathon.backend.ai.dto.OverallReportResult;
import com.hackathon.backend.ai.dto.OverallReportResult.HealthTrend;
import com.hackathon.backend.ai.dto.OverallReportResult.Period;
import com.hackathon.backend.ai.dto.OverallReportResult.Trend;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * AI 원문 응답을 계약 DTO로 변환하며, 저장 전에 계약 위반을 차단합니다.
 * 원문 보관과 분석 상태 변경은 호출 계층의 책임입니다.
 */
@Service
public class AiResultParser {

	private static final String HEALTH_SCHEMA = "health-extraction-v1";
	private final ObjectMapper objectMapper;

	public AiResultParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public HealthExtractionResult parseHealthExtraction(String rawResponse) {
		JsonNode root = readRoot(rawResponse);
		requireSchema(root, HEALTH_SCHEMA);
		JsonNode records = required(root, "records");
		if (!records.isArray()) {
			throw invalid("records는 배열이어야 합니다.");
		}

		List<HealthExtractionResult.HealthRecordCandidate> candidates = new ArrayList<>();
		for (JsonNode record : records) {
			if (!record.isObject()) {
				throw invalid("records의 각 항목은 객체여야 합니다.");
			}
			HealthType type = enumValue(HealthType.class, requiredText(record, "type"), "type");
			String summary = requiredText(record, "summary");
			if (summary.length() > 500) {
				throw invalid("summary는 500자 이하여야 합니다.");
			}
			String evidence = requiredText(record, "evidence");
			if (evidence.length() > 200) {
				throw invalid("evidence는 200자 이하여야 합니다.");
			}
			BigDecimal confidence = requiredDecimal(record, "confidence");
			if (confidence.compareTo(BigDecimal.ZERO) < 0 || confidence.compareTo(BigDecimal.ONE) > 0) {
				throw invalid("confidence는 0과 1 사이여야 합니다.");
			}
			candidates.add(new HealthExtractionResult.HealthRecordCandidate(
					type,
					summary,
					parseHealthDetail(type, requiredObject(record, "detail")),
					parseDate(requiredText(record, "recordedDate"), "recordedDate"),
					parseNullableDateTime(record, "recordedAt"),
					confidence,
					evidence));
		}
		return new HealthExtractionResult(HEALTH_SCHEMA, List.copyOf(candidates));
	}

	public ConversationSummaryResult parseConversationSummary(TaskType taskType, String rawResponse) {
		String schemaVersion = summarySchema(taskType);
		JsonNode root = readRoot(rawResponse);
		requireSchema(root, schemaVersion);
		String summary = requiredText(root, "summary");
		if (summary.isEmpty()) {
			throw invalid("summary는 빈 문자열일 수 없습니다.");
		}
		return new ConversationSummaryResult(schemaVersion, summary);
	}

	public OverallReportResult parseOverallReport(
			String rawResponse, LocalDate expectedPeriodStart, LocalDate expectedPeriodEnd, Set<HealthType> availableHealthTypes) {
		JsonNode root = readRoot(rawResponse);
		requireSchema(root, "overall-report-v1");
		String summary = requiredText(root, "summary");
		if (summary.isEmpty()) {
			throw invalid("summary는 빈 문자열일 수 없습니다.");
		}

		JsonNode detailNode = requiredObject(root, "detail");
		JsonNode periodNode = requiredObject(detailNode, "period");
		Period period = new Period(
				parseDate(requiredText(periodNode, "from"), "detail.period.from"),
				parseDate(requiredText(periodNode, "to"), "detail.period.to"));
		if (!period.from().equals(expectedPeriodStart) || !period.to().equals(expectedPeriodEnd)) {
			throw invalid("detail.period는 입력 기간과 일치해야 합니다.");
		}

		List<String> highlights = parseTextArray(detailNode, "highlights");
		validateCount(highlights, "highlights", 2, 5);
		List<HealthTrend> healthTrends = parseHealthTrends(requiredArray(detailNode, "healthTrends"), availableHealthTypes);
		List<String> recommendations = parseTextArray(detailNode, "recommendations");
		validateCount(recommendations, "recommendations", 1, 3);
		return new OverallReportResult("overall-report-v1", summary,
				new OverallReportResult.Detail(period, highlights, healthTrends, recommendations));
	}

	private HealthDetail parseHealthDetail(HealthType type, JsonNode detail) {
		return switch (type) {
			case SLEEP -> new SleepDetail(
					requireRange(requiredDecimal(detail, "hours"), "detail.hours", BigDecimal.ZERO, BigDecimal.valueOf(24)),
					enumOrNull(detail, "quality", Set.of("good", "normal", "bad")),
					parseNullableTime(detail, "bedtime"), parseNullableTime(detail, "wakeTime"));
			case MEAL -> new MealDetail(requiredText(detail, "menu"),
					enumOrNull(detail, "timing", Set.of("아침", "점심", "저녁", "간식")),
					enumOrNull(detail, "amount", Set.of("적음", "보통", "많음")));
			case EXERCISE -> new ExerciseDetail(requiredText(detail, "activity"), requiredDecimal(detail, "duration"),
					requireExact(detail, "unit", "min"), enumOrNull(detail, "intensity", Set.of("낮음", "보통", "높음")));
			case SKIN -> new SkinDetail(requiredText(detail, "condition"), nullableText(detail, "area"));
			case MOOD -> new MoodDetail(requiredText(detail, "emotion"), nullableText(detail, "note"));
			case WATER -> new WaterDetail(requiredDecimal(detail, "amount"), enumOrNull(detail, "unit", Set.of("ml", "cup")));
			case OTHER -> new OtherDetail(requiredText(detail, "note"));
		};
	}

	private List<HealthTrend> parseHealthTrends(JsonNode trends, Set<HealthType> availableHealthTypes) {
		if (availableHealthTypes == null) {
			throw invalid("availableHealthTypes는 null일 수 없습니다.");
		}
		List<HealthTrend> result = new ArrayList<>();
		for (JsonNode trend : trends) {
			if (!trend.isObject()) {
				throw invalid("healthTrends의 각 항목은 객체여야 합니다.");
			}
			HealthType type = enumValue(HealthType.class, requiredText(trend, "type"), "healthTrends.type");
			if (!availableHealthTypes.contains(type)) {
				throw invalid("healthTrends에 입력에 없는 type이 있습니다: " + type);
			}
			Trend value = enumValue(Trend.class, requiredText(trend, "trend").toUpperCase(), "healthTrends.trend");
			result.add(new HealthTrend(type, value, requiredText(trend, "note")));
		}
		return List.copyOf(result);
	}

	private String summarySchema(TaskType taskType) {
		return switch (taskType) {
			case DAILY_SUMMARY -> "daily-summary-v1";
			case WEEKLY_SUMMARY -> "weekly-summary-v1";
			case MONTHLY_SUMMARY -> "monthly-summary-v1";
			default -> throw invalid("대화 요약 TaskType만 검증할 수 있습니다: " + taskType);
		};
	}

	private JsonNode readRoot(String rawResponse) {
		if (rawResponse == null || rawResponse.isBlank()) {
			throw invalid("AI 응답이 비어 있습니다.");
		}
		try {
			JsonNode root = objectMapper.readTree(rawResponse);
			if (root == null || !root.isObject()) {
				throw invalid("AI 응답은 JSON 객체 하나여야 합니다.");
			}
			return root;
		} catch (AiResultValidationException e) {
			throw e;
		} catch (Exception e) {
			throw new AiResultValidationException("AI 응답이 유효한 JSON이 아닙니다.", e);
		}
	}

	private void requireSchema(JsonNode root, String expected) {
		if (!expected.equals(requiredText(root, "schemaVersion"))) {
			throw invalid("schemaVersion이 " + expected + "와 일치하지 않습니다.");
		}
	}

	private JsonNode required(JsonNode node, String field) {
		JsonNode value = node.get(field);
		if (value == null) {
			throw invalid(field + " 필드는 필수입니다.");
		}
		return value;
	}

	private JsonNode requiredObject(JsonNode node, String field) {
		JsonNode value = required(node, field);
		if (!value.isObject()) {
			throw invalid(field + "는 객체여야 합니다.");
		}
		return value;
	}

	private JsonNode requiredArray(JsonNode node, String field) {
		JsonNode value = required(node, field);
		if (!value.isArray()) {
			throw invalid(field + "는 배열이어야 합니다.");
		}
		return value;
	}

	private String requiredText(JsonNode node, String field) {
		JsonNode value = required(node, field);
		if (!value.isTextual()) {
			throw invalid(field + "는 문자열이어야 합니다.");
		}
		return value.textValue();
	}

	private String nullableText(JsonNode node, String field) {
		JsonNode value = required(node, field);
		if (value.isNull()) {
			return null;
		}
		if (!value.isTextual()) {
			throw invalid(field + "는 문자열 또는 null이어야 합니다.");
		}
		return value.textValue();
	}

	private BigDecimal requiredDecimal(JsonNode node, String field) {
		JsonNode value = required(node, field);
		if (!value.isNumber()) {
			throw invalid(field + "는 숫자여야 합니다.");
		}
		return value.decimalValue();
	}

	private BigDecimal requireRange(BigDecimal value, String field, BigDecimal min, BigDecimal max) {
		if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
			throw invalid(field + "는 " + min + "과 " + max + " 사이여야 합니다.");
		}
		return value;
	}

	private String enumOrNull(JsonNode node, String field, Set<String> values) {
		String value = nullableText(node, field);
		if (value != null && !values.contains(value)) {
			throw invalid(field + "의 값이 계약에 없습니다: " + value);
		}
		return value;
	}

	private String requireExact(JsonNode node, String field, String expected) {
		String value = requiredText(node, field);
		if (!expected.equals(value)) {
			throw invalid(field + "는 " + expected + "여야 합니다.");
		}
		return value;
	}

	private LocalDate parseDate(String value, String field) {
		try {
			return LocalDate.parse(value);
		} catch (DateTimeParseException e) {
			throw new AiResultValidationException(field + "는 ISO-8601 날짜여야 합니다.", e);
		}
	}

	private LocalDateTime parseNullableDateTime(JsonNode node, String field) {
		String value = nullableText(node, field);
		if (value == null) {
			return null;
		}
		try {
			return LocalDateTime.parse(value);
		} catch (DateTimeParseException e) {
			throw new AiResultValidationException(field + "는 ISO-8601 날짜·시각이어야 합니다.", e);
		}
	}

	private LocalTime parseNullableTime(JsonNode node, String field) {
		String value = nullableText(node, field);
		if (value == null) {
			return null;
		}
		try {
			return LocalTime.parse(value);
		} catch (DateTimeParseException e) {
			throw new AiResultValidationException(field + "는 HH:mm 형식이어야 합니다.", e);
		}
	}

	private List<String> parseTextArray(JsonNode node, String field) {
		List<String> result = new ArrayList<>();
		for (JsonNode value : requiredArray(node, field)) {
			if (!value.isTextual()) {
				throw invalid(field + "의 각 항목은 문자열이어야 합니다.");
			}
			result.add(value.textValue());
		}
		return List.copyOf(result);
	}

	private void validateCount(List<String> values, String field, int min, int max) {
		if (!values.isEmpty() && (values.size() < min || values.size() > max)) {
			throw invalid(field + "는 비어 있거나 " + min + "~" + max + "개여야 합니다.");
		}
	}

	private <T extends Enum<T>> T enumValue(Class<T> type, String value, String field) {
		try {
			return Enum.valueOf(type, value);
		} catch (IllegalArgumentException e) {
			throw new AiResultValidationException(field + "의 enum 값이 계약에 없습니다: " + value, e);
		}
	}

	private AiResultValidationException invalid(String message) {
		return new AiResultValidationException(message);
	}
}
