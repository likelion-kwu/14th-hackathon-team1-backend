package com.hackathon.backend.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.hackathon.backend.ai.dto.HealthExtractionResult.SleepDetail;
import com.hackathon.backend.ai.entity.AiAnalysis.TaskType;
import com.hackathon.backend.healthrecord.entity.HealthRecord.HealthType;

import tools.jackson.databind.json.JsonMapper;

class AiResultParserTest {

	private final AiResultParser parser = new AiResultParser(JsonMapper.builder().build());

	@Test
	void parsesHealthExtractionContract() {
		String response = """
				{
				  "schemaVersion": "health-extraction-v1",
				  "records": [{
				    "type": "SLEEP",
				    "summary": "어젯밤 7시간 30분 수면",
				    "detail": {"hours": 7.5, "quality": null, "bedtime": "23:00", "wakeTime": "06:30"},
				    "recordedDate": "2026-08-18",
				    "recordedAt": null,
				    "confidence": 0.92,
				    "evidence": "어제 11시에 자서 아침 6시 반에 일어났어요"
				  }]
				}
				""";

		var result = parser.parseHealthExtraction(response);

		assertThat(result.schemaVersion()).isEqualTo("health-extraction-v1");
		assertThat(result.records()).hasSize(1);
		assertThat(result.records().get(0).detail()).isInstanceOf(SleepDetail.class);
		assertThat(result.records().get(0).recordedDate()).isEqualTo(LocalDate.of(2026, 8, 18));
	}

	@Test
	void rejectsHealthExtractionWithOutOfRangeConfidence() {
		String response = """
				{"schemaVersion":"health-extraction-v1","records":[{
				"type":"WATER","summary":"물 섭취","detail":{"amount":300,"unit":"ml"},
				"recordedDate":"2026-08-19","recordedAt":null,"confidence":1.01,"evidence":"물 마셨어요"}]}
				""";

		assertThatThrownBy(() -> parser.parseHealthExtraction(response))
				.isInstanceOf(AiResultValidationException.class)
				.hasMessageContaining("confidence");
	}

	@Test
	void rejectsHealthExtractionWhenNullableFieldIsOmitted() {
		String response = """
				{"schemaVersion":"health-extraction-v1","records":[{
				"type":"SLEEP","summary":"수면","detail":{"hours":7,"quality":null,"bedtime":null,"wakeTime":null},
				"recordedDate":"2026-08-19","confidence":0.9,"evidence":"잤어요"}]}
				""";

		assertThatThrownBy(() -> parser.parseHealthExtraction(response))
				.isInstanceOf(AiResultValidationException.class)
				.hasMessageContaining("recordedAt");
	}

	@Test
	void rejectsWaterRecordWhenUnitIsNullOrOmitted() {
		String nullUnitResponse = """
				{"schemaVersion":"health-extraction-v1","records":[{
				"type":"WATER","summary":"water intake","detail":{"amount":300,"unit":null},
				"recordedDate":"2026-08-19","recordedAt":null,"confidence":0.9,"evidence":"drank water"}]}
				""";
		String omittedUnitResponse = """
				{"schemaVersion":"health-extraction-v1","records":[{
				"type":"WATER","summary":"water intake","detail":{"amount":300},
				"recordedDate":"2026-08-19","recordedAt":null,"confidence":0.9,"evidence":"drank water"}]}
				""";

		assertThatThrownBy(() -> parser.parseHealthExtraction(nullUnitResponse))
				.isInstanceOf(AiResultValidationException.class)
				.hasMessageContaining("unit");
		assertThatThrownBy(() -> parser.parseHealthExtraction(omittedUnitResponse))
				.isInstanceOf(AiResultValidationException.class)
				.hasMessageContaining("unit");
	}

	@Test
	void parsesConversationSummaryForRequestedTaskTypeOnly() {
		var result = parser.parseConversationSummary(TaskType.DAILY_SUMMARY,
				"{" + "\"schemaVersion\":\"daily-summary-v1\",\"summary\":\"짧은 안부 통화였습니다.\"}");

		assertThat(result.summary()).isEqualTo("짧은 안부 통화였습니다.");
		assertThatThrownBy(() -> parser.parseConversationSummary(TaskType.WEEKLY_SUMMARY,
				"{\"schemaVersion\":\"daily-summary-v1\",\"summary\":\"요약\"}"))
				.isInstanceOf(AiResultValidationException.class)
				.hasMessageContaining("schemaVersion");
	}

	@Test
	void parsesOverallReportWithInputContext() {
		String response = """
				{
				  "schemaVersion":"overall-report-v1",
				  "summary":"전반적으로 규칙적인 수면 패턴을 유지하셨습니다.",
				  "detail":{
				    "period":{"from":"2026-01-01","to":"2026-08-31"},
				    "highlights":["수면 시간이 늘었습니다.","운동 기록이 줄었습니다."],
				    "healthTrends":[{"type":"SLEEP","trend":"improving","note":"수면 시간이 증가했습니다."}],
				    "recommendations":["가벼운 산책을 이어가 보세요."]
				  }
				}
				""";

		var result = parser.parseOverallReport(response, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 8, 31),
				Set.of(HealthType.SLEEP, HealthType.EXERCISE));

		assertThat(result.detail().healthTrends()).singleElement()
				.extracting(trend -> trend.type()).isEqualTo(HealthType.SLEEP);
	}

	@Test
	void rejectsOverallReportTypeNotIncludedInInputHealthStats() {
		String response = """
				{"schemaVersion":"overall-report-v1","summary":"요약",
				"detail":{"period":{"from":"2026-01-01","to":"2026-01-31"},"highlights":[],
				"healthTrends":[{"type":"WATER","trend":"unclear","note":"자료가 적습니다."}],"recommendations":[]}}
				""";

		assertThatThrownBy(() -> parser.parseOverallReport(response, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
				Set.of(HealthType.SLEEP)))
				.isInstanceOf(AiResultValidationException.class)
				.hasMessageContaining("입력에 없는 type");
	}
}
