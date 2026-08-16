package com.hackathon.backend.healthrecord.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.hackathon.backend.healthrecord.dto.HealthRecordResponse;
import com.hackathon.backend.healthrecord.entity.HealthRecord;
import com.hackathon.backend.healthrecord.service.HealthRecordService;

/**
 * GET /api/health-records/today 의 웹 계층 계약을 고정합니다.
 *
 * 서비스는 대역으로 두고, 파라미터 검증(400) · 응답 JSON 형태만 확인합니다.
 * 이 API 는 슬롯 개념, 대표 기록 선정, 회원 존재 확인, date 파라미터,
 * GET /{id} 를 전혀 갖지 않으므로 그런 것을 전제한 검증은 하지 않습니다.
 * 서비스가 반환한 목록을 가공 없이 그대로 배열로 내려주는지가 핵심입니다.
 * 특히 detail 이 문자열이 아니라 JSON 객체로 직렬화되는지 확인하는 것이
 * 이 클래스의 핵심입니다 (@JsonRawValue 가 실제로 동작하는지).
 */
@WebMvcTest(HealthRecordController.class)
class HealthRecordControllerTest {

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private HealthRecordService healthRecordService;

	private HealthRecordResponse recordWithDetail(String detailJson) {
		return new HealthRecordResponse(
				1L,
				HealthRecord.HealthType.SLEEP,
				"어젯밤 7시간 수면",
				detailJson,
				LocalDate.of(2026, 8, 16),
				LocalDateTime.of(2026, 8, 16, 7, 0),
				new BigDecimal("0.9000"),
				"어제 새벽 3시에 자서 아침 7시에 일어났어요",
				HealthRecord.HealthStatus.EXTRACTED,
				10L);
	}

	@Test
	@DisplayName("정상 조회 시 200 과 공통 래퍼의 data 배열을 반환합니다")
	void returnsHealthRecordsAsArray() throws Exception {
		given(healthRecordService.findToday(1L)).willReturn(List.of(recordWithDetail(null)));

		mockMvc.perform(get("/api/health-records/today").param("memberId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data[0].id").value(1))
				.andExpect(jsonPath("$.data[0].type").value("SLEEP"))
				.andExpect(jsonPath("$.data[0].summary").value("어젯밤 7시간 수면"))
				.andExpect(jsonPath("$.data[0].conversationId").value(10))
				.andExpect(jsonPath("$.error").doesNotExist());
	}

	@Test
	@DisplayName("기록이 없으면 data 가 빈 배열이고 200 을 반환합니다")
	void returnsEmptyArrayWhenNoRecords() throws Exception {
		given(healthRecordService.findToday(1L)).willReturn(List.of());

		mockMvc.perform(get("/api/health-records/today").param("memberId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").isArray())
				.andExpect(jsonPath("$.data").isEmpty());
	}

	@Test
	@DisplayName("detail 은 문자열이 아니라 JSON 객체로 직렬화됩니다")
	void detailIsSerializedAsRawJson() throws Exception {
		given(healthRecordService.findToday(1L))
				.willReturn(List.of(recordWithDetail("{\"hours\":7.5,\"quality\":\"good\",\"bedtime\":\"03:00\"}")));

		mockMvc.perform(get("/api/health-records/today").param("memberId", "1"))
				.andExpect(status().isOk())
				// 문자열로 이스케이프되어 나가면 이 경로 자체가 존재하지 않습니다.
				.andExpect(jsonPath("$.data[0].detail.bedtime").value("03:00"))
				.andExpect(jsonPath("$.data[0].detail.hours").value(7.5))
				.andExpect(jsonPath("$.data[0].detail.quality").value("good"));
	}

	@Test
	@DisplayName("detail 이 null 이어도 응답이 깨지지 않습니다")
	void doesNotBreakWhenDetailIsNull() throws Exception {
		given(healthRecordService.findToday(1L)).willReturn(List.of(recordWithDetail(null)));

		// doesNotExist() 로 두면 키가 아예 빠진 경우까지 통과합니다. 우리가 지키려는
		// 것은 "키는 있고 값이 null" 이므로 value(nullValue()) 로 단정합니다.
		mockMvc.perform(get("/api/health-records/today").param("memberId", "1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data[0].detail").value(nullValue()));
	}

	@Test
	@DisplayName("memberId 가 없으면 400 을 반환합니다")
	void rejectsMissingMemberId() throws Exception {
		mockMvc.perform(get("/api/health-records/today"))
				.andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("memberId 가 0 이면 400 과 VALIDATION_FAILED 를 반환합니다")
	void rejectsZeroMemberId() throws Exception {
		mockMvc.perform(get("/api/health-records/today").param("memberId", "0"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	@DisplayName("memberId 가 음수이면 400 과 VALIDATION_FAILED 를 반환합니다")
	void rejectsNegativeMemberId() throws Exception {
		mockMvc.perform(get("/api/health-records/today").param("memberId", "-1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}
}
